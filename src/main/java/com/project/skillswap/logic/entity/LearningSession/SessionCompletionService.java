package com.project.skillswap.logic.entity.LearningSession;

import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import com.project.skillswap.logic.entity.Quiz.QuizEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;

/**
 * Servicio para procesar acciones post-sesión
 * Se ejecuta después de que una sesión finaliza y se procesa su transcripción
 *
 * FUNCIONALIDADES:
 * 1. Marca automáticamente todos los bookings como asistidos
 * 2. FUERZA el guardado en BD con flush
 * 3. Envía invitaciones de quiz a los learners
 */
@Service
public class SessionCompletionService {

    //#region Properties
    private static final Logger logger = LoggerFactory.getLogger(SessionCompletionService.class);

    @Autowired
    private LearningSessionRepository learningSessionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private QuizEmailService quizEmailService;

    @Autowired
    private EntityManager entityManager;
    //#endregion

    //#region Constructor
    public SessionCompletionService() {
        // Constructor vacío para Spring
    }
    //#endregion

    //#region Public Methods
    /**
     * Procesa las acciones que deben ejecutarse después de completar una sesión
     * Esto incluye:
     * - Marcar bookings como asistidos
     * - Enviar invitaciones al quiz
     *
     * @param sessionId ID de la sesión que finalizó
     */
    @Transactional
    public void processSessionCompletion(Long sessionId) {
        logger.info("╔════════════════════════════════════════════════════════════╗");
        logger.info("║  PROCESANDO FINALIZACIÓN DE SESIÓN (QUIZ INVITATIONS)    ║");
        logger.info("╠════════════════════════════════════════════════════════════╣");
        logger.info("║  Session ID: {:<45} ║", sessionId);
        logger.info("╚════════════════════════════════════════════════════════════╝");

        try {
            // Paso 1: Buscar sesión
            logger.info("1️⃣  Buscando sesión en la base de datos...");
            LearningSession session = learningSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada: " + sessionId));

            logger.info("    Sesión encontrada:");
            logger.info("     - ID: {}", session.getId());
            logger.info("     - Título: {}", session.getTitle());
            logger.info("     - Skill: {}", session.getSkill() != null ? session.getSkill().getName() : "N/A");
            logger.info("     - Instructor: {}", session.getInstructor() != null ?
                    session.getInstructor().getPerson().getFullName() : "N/A");

            // Paso 2: Validar transcripción
            logger.info("");
            logger.info("2️⃣  Validando transcripción...");
            validateSessionHasTranscription(session);
            logger.info("    Transcripción encontrada: {} caracteres",
                    session.getFullText() != null ? session.getFullText().length() : 0);

            // Paso 3: Marcar bookings como asistidos AUTOMÁTICAMENTE
            logger.info("");
            logger.info("3️⃣  Procesando bookings...");
            int markedAsAttended = markAllBookingsAsAttendedWithFlush(sessionId);

            logger.info("    Bookings marcados como asistidos: {}", markedAsAttended);

            if (markedAsAttended == 0) {
                logger.warn("   ️  No hay bookings para esta sesión");
                logger.info("   → Saltando envío de invitaciones de quiz");
                logger.info("");
                logger.info("╔════════════════════════════════════════════════════════════╗");
                logger.info("║  PROCESAMIENTO COMPLETADO (Sin bookings)                  ║");
                logger.info("╚════════════════════════════════════════════════════════════╝");
                return;
            }

            // Paso 4: Recargar sesión para asegurar que tiene los bookings actualizados
            logger.info("");
            logger.info("4️⃣  Recargando sesión con bookings actualizados...");
            entityManager.clear(); // Limpiar caché
            session = learningSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada: " + sessionId));
            logger.info("    Sesión recargada");

            // Paso 5: Enviar invitaciones
            logger.info("");
            logger.info("5️⃣  Enviando invitaciones de quiz a {} learners...", markedAsAttended);
            boolean success = sendQuizInvitations(session);

            logger.info("");
            if (success) {
                logger.info("╔════════════════════════════════════════════════════════════╗");
                logger.info("║   PROCESAMIENTO COMPLETADO EXITOSAMENTE                 ║");
                logger.info("╚════════════════════════════════════════════════════════════╝");
            } else {
                logger.warn("╔════════════════════════════════════════════════════════════╗");
                logger.warn("║  ️  PROCESAMIENTO COMPLETADO CON ERRORES                 ║");
                logger.warn("╚════════════════════════════════════════════════════════════╝");
            }

        } catch (IllegalArgumentException e) {
            logger.error("╔════════════════════════════════════════════════════════════╗");
            logger.error("║   ERROR: SESIÓN NO ENCONTRADA                            ║");
            logger.error("╠════════════════════════════════════════════════════════════╣");
            logger.error("║  Session ID: {:<45} ║", sessionId);
            logger.error("║  Mensaje: {:<48} ║", e.getMessage());
            logger.error("╚════════════════════════════════════════════════════════════╝");

        } catch (IllegalStateException e) {
            logger.error("╔════════════════════════════════════════════════════════════╗");
            logger.error("║   ERROR: VALIDACIÓN FALLIDA                              ║");
            logger.error("╠════════════════════════════════════════════════════════════╣");
            logger.error("║  Session ID: {:<45} ║", sessionId);
            logger.error("║  Mensaje: {:<48} ║", e.getMessage());
            logger.error("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            logger.error("╔════════════════════════════════════════════════════════════╗");
            logger.error("║   ERROR CRÍTICO EN PROCESAMIENTO                         ║");
            logger.error("╠════════════════════════════════════════════════════════════╣");
            logger.error("║  Session ID: {:<45} ║", sessionId);
            logger.error("║  Error: {:<48} ║", e.getMessage());
            logger.error("╚════════════════════════════════════════════════════════════╝");
            logger.error("Stack trace:", e);
        }
    }

    /**
     * Envía invitaciones de quiz solo a los learners (SkillSeekers) que asistieron
     *
     * @param sessionId ID de la sesión
     */
    @Transactional
    public void sendQuizInvitationsForSession(Long sessionId) {
        logger.info("════════════════════════════════════════════════════════════");
        logger.info(" ENVIANDO INVITACIONES MANUALMENTE (ENDPOINT REST)");
        logger.info(" Session ID: {}", sessionId);
        logger.info("════════════════════════════════════════════════════════════");

        try {
            logger.info("→ Buscando sesión...");
            LearningSession session = learningSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada: " + sessionId));

            logger.info("→ Sesión encontrada: {}", session.getTitle());

            // Marcar bookings como asistidos
            logger.info("→ Marcando bookings como asistidos...");
            int marked = markAllBookingsAsAttendedWithFlush(sessionId);
            logger.info("→ Bookings marcados: {}", marked);

            // Recargar sesión
            logger.info("→ Recargando sesión...");
            entityManager.clear();
            session = learningSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada: " + sessionId));

            logger.info("→ Enviando invitaciones...");
            boolean success = sendQuizInvitations(session);

            if (success) {
                logger.info("════════════════════════════════════════════════════════════");
                logger.info("  INVITACIONES ENVIADAS EXITOSAMENTE");
                logger.info("════════════════════════════════════════════════════════════");
            } else {
                logger.warn("════════════════════════════════════════════════════════════");
                logger.warn(" ️  NO SE ENVIARON INVITACIONES");
                logger.warn("════════════════════════════════════════════════════════════");
            }

        } catch (Exception e) {
            logger.error("════════════════════════════════════════════════════════════");
            logger.error("  ERROR AL ENVIAR INVITACIONES");
            logger.error(" Error: {}", e.getMessage(), e);
            logger.error("════════════════════════════════════════════════════════════");
            throw e;
        }
    }
    //#endregion

    //#region Private Methods
    /**
     * Valida que la sesión tenga transcripción
     *
     * @param session la sesión a validar
     * @throws IllegalStateException si no tiene transcripción
     */
    private void validateSessionHasTranscription(LearningSession session) {
        if (session.getFullText() == null || session.getFullText().trim().isEmpty()) {
            logger.error("    La sesión {} no tiene transcripción generada", session.getId());
            throw new IllegalStateException("La sesión no tiene transcripción generada");
        }
    }

    /**
     * Marca TODOS los bookings de una sesión como asistidos automáticamente
     * y FUERZA el guardado en BD con flush
     *
     * LÓGICA: Si el usuario completó la sesión (hay transcripción),
     * asumimos que todos los que tenían booking asistieron.
     *
     * @param sessionId ID de la sesión
     * @return cantidad de bookings marcados
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected int markAllBookingsAsAttendedWithFlush(Long sessionId) {
        logger.debug("   → Obteniendo bookings de la sesión directamente desde BD...");

        // Obtener bookings directamente desde el repositorio
        List<Booking> bookings = bookingRepository.findByLearningSessionId(sessionId);

        if (bookings == null || bookings.isEmpty()) {
            logger.warn("   → No hay bookings para esta sesión");
            return 0;
        }

        logger.debug("   → Total de bookings: {}", bookings.size());

        int markedCount = 0;

        for (Booking booking : bookings) {
            if (booking.getLearner() == null || booking.getLearner().getPerson() == null) {
                logger.debug("   → Booking {} sin learner válido, saltando", booking.getId());
                continue;
            }

            String learnerEmail = booking.getLearner().getPerson().getEmail();
            Long learnerId = booking.getLearner().getId();

            if (Boolean.TRUE.equals(booking.getAttended())) {
                logger.debug("   → Booking {} ya marcado como asistido: {} (Learner ID: {})",
                        booking.getId(), learnerEmail, learnerId);
            } else {
                logger.debug("   → Marcando booking {} como asistido: {} (Learner ID: {})",
                        booking.getId(), learnerEmail, learnerId);

                // Marcar como asistido
                booking.setAttended(true);

                // Guardar en BD
                bookingRepository.save(booking);

                // FORZAR el flush a la base de datos
                entityManager.flush();
                logger.info("Transaction rollback-only? {}",
                        TransactionAspectSupport.currentTransactionStatus().isRollbackOnly());

                logger.info("    Booking {} marcado como asistido: {} (Learner ID: {})",
                        booking.getId(), learnerEmail, learnerId);

                // Verificar que se guardó
                Booking verificacion = bookingRepository.findById(booking.getId()).orElse(null);
                if (verificacion != null && Boolean.TRUE.equals(verificacion.getAttended())) {
                    logger.debug("   →  VERIFICADO en BD: Booking {} está attended=true", booking.getId());
                } else {
                    logger.error("   →  ERROR: Booking {} NO se guardó correctamente en BD!", booking.getId());
                }
            }

            markedCount++;
        }

        logger.info("   → Total de learners para invitar: {}", markedCount);

        // Flush final para asegurar
        entityManager.flush();
        logger.debug("   → Flush final ejecutado");

        return markedCount;
    }

    /**
     * Envía las invitaciones de quiz a los participantes
     *
     * @param session la sesión
     * @return true si se enviaron emails exitosamente
     */
    private boolean sendQuizInvitations(LearningSession session) {
        try {
            logger.info("   → Llamando a QuizEmailService...");
            boolean result = quizEmailService.sendQuizInvitations(session);

            if (result) {
                logger.info("    QuizEmailService completado exitosamente");
            } else {
                logger.warn("   ️  QuizEmailService no envió ningún email");
            }

            return result;

        } catch (Exception e) {
            logger.error("    Error al llamar QuizEmailService: {}", e.getMessage(), e);
            return false;
        }
    }
    //#endregion
}