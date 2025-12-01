package com.project.skillswap.logic.entity.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(SessionPaymentService.class);

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
            logger.info(" [PAYMENT] Sesión gratuita, no se requiere pago");
            return;
        }

        BigDecimal cost = session.getSkillcoinsCost();
        BigDecimal learnerBalance = learner.getSkillcoinsBalance();

        logger.info(" [PAYMENT] Procesando pago de sesión premium:");
        logger.info("    Sesión: " + session.getTitle());
        logger.info("    Costo: " + cost + " SkillCoins");
        logger.info("    Learner: " + learner.getPerson().getFullName());
        logger.info("    Balance actual: " + learnerBalance + " SkillCoins");

        // 2. Validar balance suficiente
        if (learnerBalance.compareTo(cost) < 0) {
            BigDecimal deficit = cost.subtract(learnerBalance);
            logger.info(" [PAYMENT] Balance insuficiente");
            logger.info("   Necesitas: " + cost + " SkillCoins");
            logger.info("   Tienes: " + learnerBalance + " SkillCoins");
            logger.info("   Falta: " + deficit + " SkillCoins");

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

        logger.info("    Debitado de learner");
        logger.info("    Nuevo balance: " + newLearnerBalance + " SkillCoins");

        // 4. Acreditar al instructor
        Instructor instructor = session.getInstructor();
        BigDecimal instructorBalance = instructor.getSkillcoinsBalance();
        BigDecimal newInstructorBalance = instructorBalance.add(cost);
        instructor.setSkillcoinsBalance(newInstructorBalance);
        instructorRepository.save(instructor);

        logger.info("    Acreditado a instructor: " + instructor.getPerson().getFullName());
        logger.info("    Nuevo balance instructor: " + newInstructorBalance + " SkillCoins");

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

        logger.info("    Transacciones registradas");
        logger.info("    TX Learner (débito): #" + learnerTransaction.getId());
        logger.info("    TX Instructor (crédito): #" + instructorTransaction.getId());
        logger.info(" [PAYMENT] Pago procesado exitosamente");
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
            logger.info(" [REFUND] Sesión gratuita, no hay reembolso");
            return;
        }

        BigDecimal cost = session.getSkillcoinsCost();

        logger.info(" [REFUND] Procesando reembolso de sesión:");
        logger.info("    Sesión: " + session.getTitle());
        logger.info("    Monto: " + cost + " SkillCoins");
        logger.info("    Learner: " + learner.getPerson().getFullName());

        // 1. Devolver al learner
        BigDecimal learnerBalance = learner.getSkillcoinsBalance();
        BigDecimal newLearnerBalance = learnerBalance.add(cost);
        learner.setSkillcoinsBalance(newLearnerBalance);
        learnerRepository.save(learner);

        logger.info("    Reembolsado a learner");
        logger.info("    Balance: " + learnerBalance + " → " + newLearnerBalance + " SkillCoins");

        // 2. Deducir del instructor
        Instructor instructor = session.getInstructor();
        BigDecimal instructorBalance = instructor.getSkillcoinsBalance();
        BigDecimal newInstructorBalance = instructorBalance.subtract(cost);
        instructor.setSkillcoinsBalance(newInstructorBalance);
        instructorRepository.save(instructor);

        logger.info("    Deducido de instructor: " + instructor.getPerson().getFullName());
        logger.info("    Balance: " + instructorBalance + " → " + newInstructorBalance + " SkillCoins");

        // 3. Crear transacción de reembolso para el learner
        Transaction refundTransaction = new Transaction();
        refundTransaction.setPerson(learner.getPerson());
        refundTransaction.setType(TransactionType.REFUND);
        refundTransaction.setSkillcoinsAmount(cost); // Positivo = crédito
        refundTransaction.setPaymentMethod(PaymentMethod.INTERNAL_CREDIT);
        refundTransaction.setStatus(TransactionStatus.COMPLETED);
        refundTransaction = transactionRepository.save(refundTransaction);

        logger.info("    Transacción de reembolso registrada: #" + refundTransaction.getId());
        logger.info(" [REFUND] Reembolso procesado exitosamente");
    }
}