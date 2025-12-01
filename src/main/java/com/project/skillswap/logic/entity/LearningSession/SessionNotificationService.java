
package com.project.skillswap.logic.entity.LearningSession;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Booking.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Servicio para el envío de notificaciones por correo electrónico
 * relacionadas con sesiones de aprendizaje
 */
@Service
public class SessionNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SessionNotificationService.class);

    //#region Dependencies
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public SessionNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    //#endregion

    //#region Public Methods
    /**
     * Envía notificaciones de cancelación a todos los participantes de una sesión
     *
     * @param session Sesión cancelada
     * @param participantEmails Lista de emails de los participantes
     * @return Número de emails enviados exitosamente
     */
    public int sendCancellationNotifications(LearningSession session, List<String> participantEmails) {
        int emailsSent = 0;

        for (String email : participantEmails) {
            try {
                sendCancellationEmail(email, session);
                emailsSent++;
                logger.info(" [EMAIL] Cancellation notification sent to: " + email);
            } catch (MessagingException e) {
                logger.info(" [ERROR] Failed to send cancellation email to: " + email);
                logger.info(" Error: " + e.getMessage());
            }
        }

        return emailsSent;
    }

    /**
     * Envía un correo de notificación de cancelación a un participante
     *
     * @param toEmail Email del participante
     * @param session Sesión cancelada
     * @throws MessagingException Si hay un error al enviar el correo
     */
    private void sendCancellationEmail(String toEmail, LearningSession session) throws MessagingException {
        String subject = "Sesión Cancelada - " + session.getTitle();
        String htmlContent = buildCancellationEmailTemplate(session);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }
    //#endregion

    //#region Private Methods
    /**
     * Envía un correo en formato HTML
     *
     * @param to Correo del destinatario
     * @param subject Asunto del correo
     * @param htmlContent Contenido HTML del correo
     * @throws MessagingException Si hay un error al enviar el correo
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Construye el template HTML para el correo de cancelación
     *
     * @param session Sesión cancelada
     * @return Contenido HTML del correo
     */
    private String buildCancellationEmailTemplate(LearningSession session) {
        String formattedDate = formatDate(session.getScheduledDatetime());
        String instructorName = session.getInstructor().getPerson().getFullName();
        String cancellationReason = session.getCancellationReason() != null && !session.getCancellationReason().isEmpty()
                ? session.getCancellationReason()
                : "No se proporcionó una razón específica.";

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Sesión Cancelada</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <!-- Header -->" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <!-- Body -->" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #ff6b6b; margin-top: 0; font-size: 24px;'>Sesión Cancelada</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Lamentamos informarte que la siguiente sesión ha sido cancelada:" +
                "                            </p>" +
                "                            <!-- Session Info Card -->" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; border-left: 4px solid #ff6b6b; margin: 25px 0;'>" +
                "                                <h3 style='color: #aae16b; margin: 0 0 15px 0; font-size: 20px;'>" + session.getTitle() + "</h3>" +
                "                                <table style='width: 100%; color: #ffffff; font-size: 14px;'>" +
                "                                    <tr>" +
                "                                        <td style='padding: 8px 0; color: #b0b0b0;'>Fecha programada:</td>" +
                "                                        <td style='padding: 8px 0; text-align: right; font-weight: bold;'>" + formattedDate + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='padding: 8px 0; color: #b0b0b0;'>Duración:</td>" +
                "                                        <td style='padding: 8px 0; text-align: right; font-weight: bold;'>" + session.getDurationMinutes() + " minutos</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='padding: 8px 0; color: #b0b0b0;'>Instructor:</td>" +
                "                                        <td style='padding: 8px 0; text-align: right; font-weight: bold;'>" + instructorName + "</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +
                "                            <!-- Cancellation Reason -->" +
                "                            <div style='background-color: #2a2a2a; padding: 20px; border-radius: 8px; margin: 25px 0;'>" +
                "                                <h4 style='color: #aae16b; margin: 0 0 10px 0; font-size: 16px;'>Razón de la cancelación:</h4>" +
                "                                <p style='color: #ffffff; margin: 0; font-size: 14px; line-height: 1.6;'>" + cancellationReason + "</p>" +
                "                            </div>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 25px 0;'>" +
                "                                Te invitamos a explorar otras sesiones disponibles que puedan ser de tu interés." +
                "                            </p>" +
                "                            <!-- CTA Button -->" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + frontendUrl + "/sessions' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Explorar Sesiones</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Si tienes alguna pregunta, no dudes en contactarnos." +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <!-- Footer -->" +
                "                    <tr>" +
                "                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>" +
                "                                © 2025 SkillSwap. Todos los derechos reservados." +
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

    /**
     * Formatea una fecha a string legible
     *
     * @param date Fecha a formatear
     * @return String formateado
     */
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    //#endregion
}