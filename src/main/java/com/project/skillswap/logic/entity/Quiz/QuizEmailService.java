package com.project.skillswap.logic.entity.Quiz;

import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Notification.Notification;
import com.project.skillswap.logic.entity.Notification.NotificationRepository;
import com.project.skillswap.logic.entity.Notification.NotificationType;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Servicio para enviar invitaciones por email a realizar quizzes
 * Usa la misma lógica que SessionSummaryEmailService
 */
@Service
public class QuizEmailService {

    //#region Properties
    private static final Logger logger = LoggerFactory.getLogger(QuizEmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@skillswap.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.name:SkillSwap}")
    private String appName;

    @Autowired
    private NotificationRepository notificationRepository;
    //#endregion

    //#region Public Methods
    /**
     * Envía invitaciones por email a todos los learners que asistieron a una sesión
     * para que realicen el cuestionario de evaluación
     *
     * @param session la sesión que finalizó
     * @return true si al menos un email fue enviado correctamente
     */
    public boolean sendQuizInvitations(LearningSession session) {
        logger.info("========================================");
        logger.info(" ENVIANDO INVITACIONES DE QUIZ");
        logger.info("   Sesión ID: {}", session.getId());
        logger.info("   Título: {}", session.getTitle());
        logger.info("========================================");

        try {
            List<Person> attendedLearners = getAttendedLearners(session);

            logger.info("Total de learners que asistieron: {}", attendedLearners.size());

            if (attendedLearners.isEmpty()) {
                logger.warn("⚠️ No hay participantes que hayan asistido a la sesión {}", session.getId());
                logger.info("========================================");
                return false;
            }

            int successfulEmails = 0;
            int failedEmails = 0;

            for (Person learner : attendedLearners) {
                logger.debug("Procesando learner: {} ({})", learner.getFullName(), learner.getEmail());

                try {
                    if (sendQuizInvitationEmail(learner, session)) {
                        successfulEmails++;
                        logger.info("✓ Email enviado exitosamente a: {}", learner.getEmail());

                        // Crear notificación
                        try {
                            createNotification(learner, session);
                            logger.debug("  → Notificación creada");
                        } catch (Exception e) {
                            logger.error("  → Error al crear notificación: {}", e.getMessage());
                        }
                    } else {
                        failedEmails++;
                        logger.error("✗ Error al enviar email a: {}", learner.getEmail());
                    }
                } catch (Exception e) {
                    failedEmails++;
                    logger.error("✗ Excepción al procesar learner {}: {}", learner.getEmail(), e.getMessage(), e);
                }
            }

            logger.info("========================================");
            logger.info(" RESUMEN DE INVITACIONES");
            logger.info("   Total participantes: {}", attendedLearners.size());
            logger.info("   Emails enviados: {}", successfulEmails);
            logger.info("   Emails fallidos: {}", failedEmails);
            logger.info("   Resultado: {}", successfulEmails > 0 ? "ÉXITO ✓" : "FALLÓ ✗");
            logger.info("========================================");

            return successfulEmails > 0;

        } catch (Exception e) {
            logger.error("========================================");
            logger.error(" ❌ ERROR CRÍTICO AL ENVIAR INVITACIONES");
            logger.error("   Sesión ID: {}", session.getId());
            logger.error("   Error: {}", e.getMessage(), e);
            logger.error("========================================");
            return false;
        }
    }
    //#endregion

    //#region Private Methods - Participant Collection
    /**
     * Obtiene todos los learners que asistieron a la sesión
     *
     * @param session la sesión
     * @return lista de personas que asistieron
     */
    private List<Person> getAttendedLearners(LearningSession session) {
        logger.debug("Obteniendo learners que asistieron...");

        List<Person> attendedLearners = new ArrayList<>();

        if (session.getBookings() == null) {
            logger.warn("La sesión {} no tiene bookings", session.getId());
            return attendedLearners;
        }

        logger.debug("Total de bookings: {}", session.getBookings().size());

        for (Booking booking : session.getBookings()) {
            logger.debug("  - Booking ID {}: attended={}, learner={}",
                    booking.getId(),
                    booking.getAttended(),
                    booking.getLearner() != null ? booking.getLearner().getId() : "null");

            if (Boolean.TRUE.equals(booking.getAttended()) &&
                    booking.getLearner() != null &&
                    booking.getLearner().getPerson() != null) {

                Person person = booking.getLearner().getPerson();
                attendedLearners.add(person);
                logger.debug("    → Añadido: {} ({})", person.getFullName(), person.getEmail());
            }
        }

        logger.debug("Total de learners que asistieron: {}", attendedLearners.size());
        return attendedLearners;
    }
    //#endregion

    //#region Private Methods - Email Sending
    /**
     * Envía un correo individual a un learner con la invitación al quiz
     *
     * @param learner la persona destinataria
     * @param session la sesión correspondiente
     * @return true si se envió exitosamente
     */
    private boolean sendQuizInvitationEmail(Person learner, LearningSession session) {
        logger.debug("Preparando email para: {}", learner.getEmail());

        try {
            String quizUrl = String.format("%s/app/quiz?sessionId=%d", frontendUrl, session.getId());

            logger.debug("  - URL del quiz: {}", quizUrl);
            logger.debug("  - From: {}", fromEmail);
            logger.debug("  - To: {}", learner.getEmail());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(learner.getEmail());
            helper.setSubject("📝 Completa tu evaluación - " + session.getTitle());

            String htmlContent = buildQuizInvitationEmailTemplate(learner, session, quizUrl);
            helper.setText(htmlContent, true);

            logger.debug("  - Enviando mensaje...");
            mailSender.send(message);
            logger.debug("  - Mensaje enviado exitosamente");

            return true;

        } catch (Exception e) {
            logger.error("  - Error al enviar email: {}", e.getMessage(), e);
            return false;
        }
    }
    //#endregion

    //#region Private Methods - Email Template
    /**
     * Construye el template HTML para el correo de invitación al quiz
     *
     * @param learner la persona destinataria
     * @param session la sesión
     * @param quizUrl URL del quiz
     * @return contenido HTML del correo
     */
    private String buildQuizInvitationEmailTemplate(Person learner, LearningSession session, String quizUrl) {
        logger.debug("  - Construyendo template HTML");

        String instructorName = session.getInstructor() != null &&
                session.getInstructor().getPerson() != null
                ? session.getInstructor().getPerson().getFullName()
                : "N/A";

        String skillName = session.getSkill() != null
                ? session.getSkill().getName()
                : "N/A";

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Evaluación Disponible</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>" + appName + "</h1>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>📝 ¡Completa tu evaluación!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Hola <strong style='color: #aae16b;'>" + learner.getFullName() + "</strong>," +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Gracias por participar en nuestra sesión de aprendizaje. Ahora es momento de demostrar lo que has aprendido." +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0;'>" +
                "                                <p style='margin: 5px 0; color: #ffffff;'><strong style='color: #aae16b;'>Sesión completada:</strong> " + session.getTitle() + "</p>" +
                "                                <p style='margin: 5px 0; color: #ffffff;'><strong style='color: #aae16b;'>Habilidad:</strong> " + skillName + "</p>" +
                "                                <p style='margin: 5px 0; color: #ffffff;'><strong style='color: #aae16b;'>Instructor:</strong> " + instructorName + "</p>" +
                "                            </div>" +
                "                            <h3 style='color: #aae16b; margin-top: 30px; font-size: 18px;'>¿Qué debes hacer?</h3>" +
                "                            <ul style='color: #ffffff; font-size: 14px; line-height: 1.8; padding-left: 20px;'>" +
                "                                <li>Responder <strong>10 preguntas</strong> sobre el contenido de la sesión</li>" +
                "                                <li>Necesitas al menos <strong style='color: #aae16b;'>70%</strong> para aprobar</li>" +
                "                                <li>Tienes <strong style='color: #aae16b;'>2 intentos</strong> disponibles</li>" +
                "                                <li>Puedes <strong>guardar tu progreso</strong> y continuar después</li>" +
                "                            </ul>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + quizUrl + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>🎯 Iniciar Evaluación</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <div style='background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 5px; padding: 15px; margin: 20px 0;'>" +
                "                                <p style='margin: 0; font-size: 14px; color: #856404;'>" +
                "                                    <strong>💡 Tip:</strong> Si apruebas, recibirás una <strong>credencial</strong>. ¡Acumula 10 credenciales de la misma habilidad y obtendrás un <strong>certificado oficial</strong>!" +
                "                                </p>" +
                "                            </div>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0;'>" +
                "                                Si no puedes hacer clic en el botón, copia y pega el siguiente enlace en tu navegador:" +
                "                            </p>" +
                "                            <p style='font-size: 12px; word-break: break-all; color: #504ab7; background-color: #39434b; padding: 10px; border-radius: 5px;'>" +
                "                                " + quizUrl +
                "                            </p>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Si tienes alguna duda, no dudes en contactarnos." +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>" +
                "                                © 2025 " + appName + ". Todos los derechos reservados." +
                "                            </p>" +
                "                            <p style='margin: 5px 0 0 0; font-size: 11px; color: #888;'>" +
                "                                Este es un correo automático, por favor no respondas." +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";
    }
    //#endregion

    //#region Private Methods - Notification
    /**
     * Crea una notificación en el sistema
     *
     * @param learner la persona
     * @param session la sesión
     */
    private void createNotification(Person learner, LearningSession session) {
        try {
            Notification notification = new Notification();
            notification.setPerson(learner);
            notification.setType(NotificationType.SESSION);
            notification.setTitle("Evaluación disponible: " + session.getTitle());
            notification.setMessage(String.format(
                    "Completa la evaluación de la sesión '%s'. Necesitas 70%% para aprobar. Tienes 2 intentos.",
                    session.getTitle()
            ));
            notification.setRead(false);
            notification.setSendDate(new Date());

            notificationRepository.save(notification);

        } catch (Exception e) {
            logger.error("Error al crear notificación para {}: {}", learner.getEmail(), e.getMessage());
        }
    }
    //#endregion
}