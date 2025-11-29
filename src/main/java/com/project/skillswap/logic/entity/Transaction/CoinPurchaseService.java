package com.project.skillswap.logic.entity.Transaction;

import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para procesar compras de SkillCoins mediante integración con PayPal.
 * Maneja el flujo completo de compra incluyendo procesamiento de pagos, registro de transacciones,
 * actualización de balances y envío de comprobantes por email.
 *
 * @author Equipo de Desarrollo SkillSwap
 * @version 1.0
 */
@Service
public class CoinPurchaseService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private LearnerRepository learnerRepository;

    @Autowired
    private PayPalService payPalService;

    @Autowired
    private TransactionEmailService emailService;

    private static final BigDecimal MAX_COINS_PER_DAY = new BigDecimal("500");

    /**
     * Procesa una transacción de compra de SkillCoins.
     * Valida el usuario, verifica límites diarios, procesa el pago de PayPal,
     * actualiza el balance del learner y envía email con comprobante PDF adjunto.
     *
     * @param personId ID de la persona que realiza la compra
     * @param packageType paquete de monedas que se está comprando
     * @param paypalOrderId ID de orden de PayPal después de que el usuario aprueba el pago
     * @return la transacción completada con estado COMPLETED o FAILED
     * @throws IllegalArgumentException si no se encuentra el usuario o se excede el límite diario
     * @throws IllegalStateException si el usuario no es un learner
     */
    @Transactional
    public Transaction purchaseCoins(Long personId, CoinPackageType packageType, String paypalOrderId) {

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new IllegalStateException("Only learners can purchase SkillCoins");
        }

        BigDecimal coinsToday = transactionRepository.sumCoinsPurchasedToday(personId);
        BigDecimal newTotal = coinsToday.add(packageType.getCoins());

        if (newTotal.compareTo(MAX_COINS_PER_DAY) > 0) {
            throw new IllegalArgumentException(
                    String.format("Daily purchase limit exceeded. You can only purchase %s more coins today.",
                            MAX_COINS_PER_DAY.subtract(coinsToday))
            );
        }

        Optional<Transaction> existingTransaction = transactionRepository.findByPaypalReference(paypalOrderId);
        if (existingTransaction.isPresent()) {
            return existingTransaction.get();
        }

        boolean paymentSuccessful = payPalService.executePayment(paypalOrderId, packageType.getPriceUsd());

        if (!paymentSuccessful) {
            Transaction failedTransaction = createTransaction(
                    person, packageType, paypalOrderId, TransactionStatus.FAILED
            );
            failedTransaction = transactionRepository.save(failedTransaction);
            sendFailureEmailAsync(person, packageType);
            return failedTransaction;
        }

        Transaction transaction = createTransaction(
                person, packageType, paypalOrderId, TransactionStatus.COMPLETED
        );
        transaction = transactionRepository.save(transaction);

        BigDecimal newBalance = learner.getSkillcoinsBalance().add(packageType.getCoins());
        learner.setSkillcoinsBalance(newBalance);
        learnerRepository.save(learner);

        sendPurchaseEmailAsync(person, transaction, packageType, newBalance);

        return transaction;
    }

    /**
     * Crea una orden de PayPal para compra de monedas.
     * Esto se llama antes del pago para iniciar el flujo de PayPal.
     *
     * @param packageType tipo de paquete a comprar
     * @return ID de orden de PayPal para ser usado por el cliente
     * @throws RuntimeException si falla la creación de la orden
     */
    public String createPayPalOrder(CoinPackageType packageType) {
        try {
            return payPalService.createOrder(packageType.getPriceUsd(), packageType.name());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PayPal order: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el balance actual de SkillCoins de un learner.
     *
     * @param personId ID de la persona
     * @return balance actual de SkillCoins
     * @throws IllegalArgumentException si no se encuentra el usuario
     * @throws IllegalStateException si el usuario no es un learner
     */
    public BigDecimal getBalance(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new IllegalStateException("User is not a learner");
        }

        return learner.getSkillcoinsBalance();
    }

    /**
     * Obtiene el historial de compras de un usuario.
     *
     * @param personId ID de la persona
     * @return lista de transacciones de compra ordenadas por fecha
     */
    public List<Transaction> getUserPurchases(Long personId) {
        return transactionRepository.findPurchasesByPersonId(personId);
    }

    /**
     * Obtiene todos los paquetes de monedas disponibles.
     *
     * @return arreglo de valores del enum CoinPackageType disponibles
     */
    public CoinPackageType[] getAvailablePackages() {
        return CoinPackageType.values();
    }

    /**
     * Crea una entidad de transacción con los parámetros especificados.
     *
     * @param person persona que realiza la compra
     * @param packageType paquete que se está comprando
     * @param paypalReference ID de referencia de la orden de PayPal
     * @param status estado de la transacción (COMPLETED o FAILED)
     * @return entidad Transaction creada pero aún no persistida
     */
    private Transaction createTransaction(
            Person person,
            CoinPackageType packageType,
            String paypalReference,
            TransactionStatus status) {

        Transaction transaction = new Transaction();
        transaction.setPerson(person);
        transaction.setType(TransactionType.PURCHASE);
        transaction.setSkillcoinsAmount(packageType.getCoins());
        transaction.setUsdAmount(packageType.getPriceUsd());
        transaction.setPaymentMethod(PaymentMethod.PAYPAL);
        transaction.setStatus(status);
        transaction.setPaypalReference(paypalReference);

        return transaction;
    }

    /**
     * Envía email de confirmación de compra con PDF adjunto de forma asíncrona.
     * Se ejecuta en un hilo separado para evitar bloquear la transacción
     * y para prevenir que fallos en el envío de emails causen rollback de la transacción.
     *
     * @param person persona que realizó la compra
     * @param transaction transacción completada
     * @param packageType tipo de paquete comprado
     * @param newBalance balance actualizado de SkillCoins
     */
    private void sendPurchaseEmailAsync(Person person, Transaction transaction, CoinPackageType packageType, BigDecimal newBalance) {
        new Thread(() -> {
            try {
                Thread.sleep(500);
                emailService.sendPurchaseConfirmation(
                        person.getEmail(),
                        person.getFullName(),
                        transaction,
                        packageType,
                        packageType.getCoins(),
                        packageType.getPriceUsd(),
                        newBalance,
                        transaction.getPaypalReference()
                );
            } catch (Exception e) {
                // El fallo del email se registra pero no afecta la transacción
            }
        }).start();
    }

    /**
     * Envía email de notificación de fallo de compra de forma asíncrona.
     * Notifica al usuario que su intento de pago falló.
     *
     * @param person persona que intentó realizar la compra
     * @param packageType tipo de paquete que se intentó comprar
     */
    private void sendFailureEmailAsync(Person person, CoinPackageType packageType) {
        new Thread(() -> {
            try {
                Thread.sleep(500);
                emailService.sendPurchaseFailedNotification(
                        person.getEmail(),
                        person.getFullName(),
                        packageType.name(),
                        "Payment processing failed. Please verify your PayPal account and try again."
                );
            } catch (Exception e) {
                // El fallo del email se registra pero no afecta la transacción
            }
        }).start();
    }
}