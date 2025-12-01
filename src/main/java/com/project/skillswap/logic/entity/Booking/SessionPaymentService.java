package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Transaction.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Servicio para procesar pagos de sesiones premium con SkillCoins.
 * Maneja débitos de learners y créditos a instructors.
 */

@Service
public class SessionPaymentService {

    @Autowired
    private LearnerRepository learnerRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Procesa el pago de una sesión premium.
     * - Valida que sea sesión premium
     * - Verifica balance suficiente
     * - Debita SkillCoins del learner
     * - Acredita SkillCoins al instructor
     * - Crea registros de transacción
     *
     * @param learner Learner que paga por la sesión
     * @param session Sesión premium a la que se registra
     * @throws IllegalStateException Si el learner no tiene suficiente balance
     */
    @Transactional
    public void processSessionPayment(Learner learner, LearningSession session) {

        // 1. Validar que la sesión sea premium
        if (!session.getIsPremium() || session.getSkillcoinsCost() == null ||
                session.getSkillcoinsCost().compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println(" [PAYMENT] Sesión gratuita, no se requiere pago");
            return;
        }

        BigDecimal cost = session.getSkillcoinsCost();
        BigDecimal learnerBalance = learner.getSkillcoinsBalance();

        System.out.println(" [PAYMENT] Procesando pago de sesión premium:");
        System.out.println("    Sesión: " + session.getTitle());
        System.out.println("    Costo: " + cost + " SkillCoins");
        System.out.println("    Learner: " + learner.getPerson().getFullName());
        System.out.println("    Balance actual: " + learnerBalance + " SkillCoins");

        // 2. Validar balance suficiente
        if (learnerBalance.compareTo(cost) < 0) {
            BigDecimal deficit = cost.subtract(learnerBalance);
            System.err.println(" [PAYMENT] Balance insuficiente");
            System.err.println("   Necesitas: " + cost + " SkillCoins");
            System.err.println("   Tienes: " + learnerBalance + " SkillCoins");
            System.err.println("   Falta: " + deficit + " SkillCoins");

            throw new IllegalStateException(
                    String.format("Balance insuficiente. Necesitas %s SkillCoins pero solo tienes %s. " +
                                    "Por favor compra %s SkillCoins más.",
                            cost, learnerBalance, deficit)
            );
        }

        // 3. Debitar del learner
        BigDecimal newLearnerBalance = learnerBalance.subtract(cost);
        learner.setSkillcoinsBalance(newLearnerBalance);
        learnerRepository.save(learner);

        System.out.println("    Debitado de learner");
        System.out.println("    Nuevo balance: " + newLearnerBalance + " SkillCoins");

        // 4. Acreditar al instructor
        Instructor instructor = session.getInstructor();
        BigDecimal instructorBalance = instructor.getSkillcoinsBalance();
        BigDecimal newInstructorBalance = instructorBalance.add(cost);
        instructor.setSkillcoinsBalance(newInstructorBalance);
        instructorRepository.save(instructor);

        System.out.println("    Acreditado a instructor: " + instructor.getPerson().getFullName());
        System.out.println("    Nuevo balance instructor: " + newInstructorBalance + " SkillCoins");

        // 5. Crear transacción de pago del learner (débito)
        Transaction learnerTransaction = new Transaction();
        learnerTransaction.setPerson(learner.getPerson());
        learnerTransaction.setType(TransactionType.SESSION_PAYMENT);
        learnerTransaction.setSkillcoinsAmount(cost.negate()); // Negativo = débito
        learnerTransaction.setPaymentMethod(PaymentMethod.INTERNAL_CREDIT);
        learnerTransaction.setStatus(TransactionStatus.COMPLETED);
        learnerTransaction = transactionRepository.save(learnerTransaction);

        // 6. Crear transacción de cobro del instructor (crédito)
        Transaction instructorTransaction = new Transaction();
        instructorTransaction.setPerson(instructor.getPerson());
        instructorTransaction.setType(TransactionType.COLLECTION);
        instructorTransaction.setSkillcoinsAmount(cost); // Positivo = crédito
        instructorTransaction.setPaymentMethod(PaymentMethod.INTERNAL_CREDIT);
        instructorTransaction.setStatus(TransactionStatus.COMPLETED);
        instructorTransaction = transactionRepository.save(instructorTransaction);

        System.out.println("    Transacciones registradas");
        System.out.println("    TX Learner (débito): #" + learnerTransaction.getId());
        System.out.println("    TX Instructor (crédito): #" + instructorTransaction.getId());
        System.out.println(" [PAYMENT] Pago procesado exitosamente");
    }

    /**
     * Reembolsa el pago de una sesión cancelada.
     * - Devuelve SkillCoins al learner
     * - Deduce SkillCoins del instructor
     * - Crea transacción de reembolso
     *
     * @param learner Learner a reembolsar
     * @param session Sesión cancelada
     */
    @Transactional
    public void refundSessionPayment(Learner learner, LearningSession session) {

        if (!session.getIsPremium() || session.getSkillcoinsCost() == null ||
                session.getSkillcoinsCost().compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println(" [REFUND] Sesión gratuita, no hay reembolso");
            return;
        }

        BigDecimal cost = session.getSkillcoinsCost();

        System.out.println(" [REFUND] Procesando reembolso de sesión:");
        System.out.println("    Sesión: " + session.getTitle());
        System.out.println("    Monto: " + cost + " SkillCoins");
        System.out.println("    Learner: " + learner.getPerson().getFullName());

        // 1. Devolver al learner
        BigDecimal learnerBalance = learner.getSkillcoinsBalance();
        BigDecimal newLearnerBalance = learnerBalance.add(cost);
        learner.setSkillcoinsBalance(newLearnerBalance);
        learnerRepository.save(learner);

        System.out.println("    Reembolsado a learner");
        System.out.println("    Balance: " + learnerBalance + " → " + newLearnerBalance + " SkillCoins");

        // 2. Deducir del instructor
        Instructor instructor = session.getInstructor();
        BigDecimal instructorBalance = instructor.getSkillcoinsBalance();
        BigDecimal newInstructorBalance = instructorBalance.subtract(cost);
        instructor.setSkillcoinsBalance(newInstructorBalance);
        instructorRepository.save(instructor);

        System.out.println("    Deducido de instructor: " + instructor.getPerson().getFullName());
        System.out.println("    Balance: " + instructorBalance + " → " + newInstructorBalance + " SkillCoins");

        // 3. Crear transacción de reembolso para el learner
        Transaction refundTransaction = new Transaction();
        refundTransaction.setPerson(learner.getPerson());
        refundTransaction.setType(TransactionType.REFUND);
        refundTransaction.setSkillcoinsAmount(cost); // Positivo = crédito
        refundTransaction.setPaymentMethod(PaymentMethod.INTERNAL_CREDIT);
        refundTransaction.setStatus(TransactionStatus.COMPLETED);
        refundTransaction = transactionRepository.save(refundTransaction);

        System.out.println("    Transacción de reembolso registrada: #" + refundTransaction.getId());
        System.out.println(" [REFUND] Reembolso procesado exitosamente");
    }
}