package com.project.skillswap.logic.entity.Instructor;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.payouts.*;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Transaction.Transaction;
import com.project.skillswap.logic.entity.Transaction.TransactionRepository;
import com.project.skillswap.logic.entity.Transaction.TransactionType;
import com.project.skillswap.logic.entity.Transaction.PaymentMethod;
import com.project.skillswap.logic.entity.Transaction.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class InstructorPayPalService {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private PayPalHttpClient payPalClient;

    // Tasa de conversión: 1 SkillCoin = 0.90 USD (plataforma se queda con 10% comisión)
    private static final BigDecimal CONVERSION_RATE = new BigDecimal("0.90");
    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("10.00"); // Mínimo 10 SkillCoins

    /**
     * Obtiene o inicializa el cliente PayPal
     */
    private PayPalHttpClient getPayPalClient() {
        if (payPalClient == null) {
            PayPalEnvironment environment;
            if ("sandbox".equalsIgnoreCase(mode)) {
                environment = new PayPalEnvironment.Sandbox(clientId, clientSecret);
            } else {
                environment = new PayPalEnvironment.Live(clientId, clientSecret);
            }
            payPalClient = new PayPalHttpClient(environment);
        }
        return payPalClient;
    }

    /**
     * Vincula y verifica la cuenta PayPal del instructor
     */
    @Transactional
    public void linkPayPalAccount(Long instructorId, String paypalEmail) {

        System.out.println("[PAYPAL_LINK] Vinculando cuenta PayPal para instructor: " + instructorId);

        // Validar instructor
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor no encontrado"));

        // Validar formato de email
        if (paypalEmail == null || !paypalEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new RuntimeException("Email de PayPal inválido");
        }

        // Validar que el email no esté ya en uso por otro instructor
        if (instructorRepository.existsByPaypalAccountAndIdNot(paypalEmail, instructorId)) {
            throw new RuntimeException("Este email de PayPal ya está vinculado a otra cuenta");
        }

        // Guardar cuenta PayPal
        instructor.setPaypalAccount(paypalEmail);
        instructor.setVerifiedAccount(true);
        instructorRepository.save(instructor);

        System.out.println("[PAYPAL_LINK]  Cuenta PayPal vinculada: " + paypalEmail);
    }

    /**
     * Procesa el retiro de SkillCoins a PayPal
     */
    @Transactional
    public Transaction withdrawToPayPal(Long instructorId, BigDecimal skillCoinsAmount) {

        System.out.println("[PAYPAL_WITHDRAWAL] Procesando retiro para instructor: " + instructorId);
        System.out.println("[PAYPAL_WITHDRAWAL] Cantidad: " + skillCoinsAmount + " SkillCoins");

        // 1. Validar instructor
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor no encontrado"));

        Person person = instructor.getPerson();

        // 2. Validar que tenga cuenta PayPal vinculada
        if (instructor.getPaypalAccount() == null || instructor.getPaypalAccount().isEmpty()) {
            throw new RuntimeException("Debes vincular una cuenta PayPal antes de retirar fondos");
        }

        if (instructor.getVerifiedAccount() == null || !instructor.getVerifiedAccount()) {
            throw new RuntimeException("Tu cuenta PayPal no está verificada");
        }

        // 3. Validar cantidad mínima
        if (skillCoinsAmount.compareTo(MIN_WITHDRAWAL) < 0) {
            throw new RuntimeException("El retiro mínimo es de " + MIN_WITHDRAWAL + " SkillCoins");
        }

        // 4. Validar balance suficiente
        if (instructor.getSkillcoinsBalance().compareTo(skillCoinsAmount) < 0) {
            throw new RuntimeException("Balance insuficiente. Tienes " + instructor.getSkillcoinsBalance() +
                    " SkillCoins pero intentas retirar " + skillCoinsAmount);
        }

        // 5. Calcular monto en USD (con comisión del 10%)
        BigDecimal usdAmount = skillCoinsAmount.multiply(CONVERSION_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        System.out.println("[PAYPAL_WITHDRAWAL] Monto a transferir: $" + usdAmount + " USD");
        System.out.println("[PAYPAL_WITHDRAWAL] Cuenta destino: " + instructor.getPaypalAccount());

        // 6. Crear payout en PayPal
        String payoutBatchId;
        try {
            payoutBatchId = createPayPalPayout(instructor.getPaypalAccount(), usdAmount, person.getFullName());
        } catch (Exception e) {
            System.err.println("[PAYPAL_WITHDRAWAL]  Error al crear payout: " + e.getMessage());
            throw new RuntimeException("Error al procesar el retiro. Por favor intenta nuevamente.");
        }

        // 7. Debitar SkillCoins del instructor
        instructor.setSkillcoinsBalance(
                instructor.getSkillcoinsBalance().subtract(skillCoinsAmount)
        );
        instructorRepository.save(instructor);

        System.out.println("[PAYPAL_WITHDRAWAL]  Balance actualizado: " + instructor.getSkillcoinsBalance());

        // 8. Crear transacción de retiro
        Transaction transaction = new Transaction();
        transaction.setPerson(person);
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setSkillcoinsAmount(skillCoinsAmount.negate()); // Negativo porque es débito
        transaction.setUsdAmount(usdAmount);
        transaction.setPaymentMethod(PaymentMethod.PAYPAL);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaypalReference(payoutBatchId);
        transaction.setTransactionDate(new Date());

        Transaction savedTransaction = transactionRepository.save(transaction);

        System.out.println("[PAYPAL_WITHDRAWAL]  Transacción creada: " + savedTransaction.getId());
        System.out.println("[PAYPAL_WITHDRAWAL]  Payout enviado con ID: " + payoutBatchId);

        return savedTransaction;
    }

    /**
     /**
     * Crea un payout en PayPal usando PayPal Payouts API
     */
    private String createPayPalPayout(String recipientEmail, BigDecimal amount, String recipientName) throws Exception {

        System.out.println("[PAYPAL_API] Creando payout para: " + recipientEmail);

        // Crear el item del payout
        PayoutItem payoutItem = new PayoutItem();
        payoutItem.senderItemId("SKILLSWAP-" + System.currentTimeMillis());
        payoutItem.recipientType("EMAIL");
        payoutItem.receiver(recipientEmail);
        payoutItem.amount(new Currency().currency("USD").value(amount.toString()));
        payoutItem.note("SkillSwap - Retiro de ganancias");

        // Crear lista de items
        ArrayList<PayoutItem> items = new ArrayList<>();
        items.add(payoutItem);

        // Crear el request de payout
        CreatePayoutRequest requestBody = new CreatePayoutRequest();
        requestBody.senderBatchHeader(new SenderBatchHeader()
                .senderBatchId("BATCH-" + System.currentTimeMillis())
                .emailSubject("SkillSwap - Pago recibido")
                .emailMessage("Has recibido un pago de SkillSwap por tus sesiones como instructor."));
        requestBody.items(items);

        PayoutsPostRequest request = new PayoutsPostRequest();
        request.requestBody(requestBody);

        try {
            HttpResponse<CreatePayoutResponse> response = getPayPalClient().execute(request);
            CreatePayoutResponse result = response.result();

            System.out.println("[PAYPAL_API]  Payout creado exitosamente");
            System.out.println("[PAYPAL_API] Batch ID: " + result.batchHeader().payoutBatchId());
            System.out.println("[PAYPAL_API] Status: " + result.batchHeader().batchStatus());

            return result.batchHeader().payoutBatchId();

        } catch (Exception e) {
            System.err.println("[PAYPAL_API]  Error: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Error al procesar el payout en PayPal: " + e.getMessage());
        }
    }

    /**
     * Obtiene información de la cuenta PayPal vinculada
     */
    public Map<String, Object> getPayPalInfo(Long instructorId) {

        System.out.println("[PAYPAL_INFO] Obteniendo info para instructor: " + instructorId);

        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor no encontrado"));

        Map<String, Object> info = new HashMap<>();

        // Verificar si tiene cuenta vinculada
        boolean hasAccount = instructor.getPaypalAccount() != null &&
                !instructor.getPaypalAccount().isEmpty();

        info.put("hasLinkedAccount", hasAccount);
        info.put("paypalEmail", instructor.getPaypalAccount());
        info.put("verified", instructor.getVerifiedAccount() != null && instructor.getVerifiedAccount());

        // Balance actual (nunca null)
        BigDecimal currentBalance = instructor.getSkillcoinsBalance() != null ?
                instructor.getSkillcoinsBalance() :
                BigDecimal.ZERO;
        info.put("currentBalance", currentBalance);

        // Configuración del sistema
        info.put("minWithdrawal", MIN_WITHDRAWAL);
        info.put("conversionRate", CONVERSION_RATE);

        // Calcular estimado en USD
        BigDecimal estimatedUsd;
        if (currentBalance.compareTo(BigDecimal.ZERO) > 0) {
            estimatedUsd = currentBalance.multiply(CONVERSION_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            estimatedUsd = BigDecimal.ZERO;
        }
        info.put("estimatedUsd", estimatedUsd);

        System.out.println("[PAYPAL_INFO] Info generada: hasLinkedAccount=" + hasAccount +
                ", balance=" + currentBalance);

        return info;
    }
}