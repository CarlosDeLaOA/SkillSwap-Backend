package com.project.skillswap.logic.entity.Notification;

import com.project.skillswap.logic.entity.Notification.CredentialAlertDTO;
import com.project.skillswap.logic.entity.Notification.SessionAlertDTO;
import com.project.skillswap.logic.entity.Notification.UserSessionAlertDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Servicio para el envío de correos de alertas
 */
@Service
public class AlertEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public AlertEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía alerta de credenciales cercanas a certificado
     */
    public void sendCredentialAlert(CredentialAlertDTO alert) throws MessagingException {
        String subject = "¡Estás cerca de obtener tu certificado!";
        String htmlContent = buildCredentialAlertTemplate(alert);
        sendHtmlEmail(alert.getLearnerEmail(), subject, htmlContent);
    }

    /**
     * Envía alerta de sesiones próximas
     */
    public void sendSessionAlert(UserSessionAlertDTO userAlert) throws MessagingException {
        String subject = "Tus sesiones programadas para esta semana";
        String htmlContent = buildSessionAlertTemplate(userAlert);
        sendHtmlEmail(userAlert.getEmail(), subject, htmlContent);
    }

    /**
     * Envía un correo en formato HTML
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
     * Construye el template HTML para alerta de credenciales
     */
    private String buildCredentialAlertTemplate(CredentialAlertDTO alert) {
        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Alerta de Certificado</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "                            <p style='color: #ffffff; margin: 10px 0 0 0; font-size: 16px;'>Alerta de Progreso</p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola, " + alert.getLearnerName() + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                ¡Felicitaciones! Estás muy cerca de completar tu certificado en <strong style='color: #aae16b;'>" + alert.getSkillName() + "</strong>." +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 30px; border-radius: 8px; margin: 25px 0; text-align: center;'>" +
                "                                <div style='font-size: 48px; margin-bottom: 20px;'></div>" +
                "                                <h3 style='color: #aae16b; margin: 0 0 15px 0; font-size: 20px;'>Tu Progreso</h3>" +
                "                                <div style='background-color: #141414; padding: 20px; border-radius: 5px; margin: 20px 0;'>" +
                "                                    <p style='color: #ffffff; font-size: 18px; margin: 0;'>Credenciales obtenidas:</p>" +
                "                                    <p style='color: #aae16b; font-size: 36px; font-weight: bold; margin: 10px 0;'>" + alert.getCredentialCount() + " / 10</p>" +
                "                                    <div style='background-color: #39434b; height: 20px; border-radius: 10px; overflow: hidden; margin: 20px 0;'>" +
                "                                        <div style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); width: " + (alert.getCredentialCount() * 10) + "%; height: 100%;'></div>" +
                "                                    </div>" +
                "                                    <p style='color: #b0b0b0; font-size: 14px; margin: 0;'>Te faltan solo <strong style='color: #aae16b;'>" + alert.getRemainingCredentials() + " credenciales</strong> más</p>" +
                "                                </div>" +
                "                            </div>" +
                "                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 20px; margin-bottom: 15px;'>¿Cómo completar tu certificado?</h3>" +
                "                                <ul style='color: #ffffff; font-size: 15px; line-height: 1.8; padding-left: 20px; margin: 15px 0;'>" +
                "                                    <li style='margin-bottom: 10px;'>Asiste a sesiones de <strong style='color: #aae16b;'>" + alert.getSkillName() + "</strong></li>" +
                "                                    <li style='margin-bottom: 10px;'>Completa y aprueba los quizzes al final de cada sesión</li>" +
                "                                </ul>" +
                "                            </div>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + frontendUrl + "/sessions?skill=" + alert.getSkillId() + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Sesiones Disponibles</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0 0 0; text-align: center;'>" +
                "                                ¡Sigue adelante, estás muy cerca de tu objetivo!" +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
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
     * Construye el template HTML para alerta de sesiones
     */
    private String buildSessionAlertTemplate(UserSessionAlertDTO userAlert) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd 'de' MMMM 'a las' HH:mm", new Locale("es", "ES"));

        StringBuilder instructorSessionsHtml = new StringBuilder();
        StringBuilder learnerSessionsHtml = new StringBuilder();

        // Construir HTML para sesiones como instructor
        if (!userAlert.getInstructorSessions().isEmpty()) {
            instructorSessionsHtml.append("<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 20px 0;'>");
            instructorSessionsHtml.append("<h3 style='color: #aae16b; margin-top: 0; font-size: 20px; margin-bottom: 20px;'>👨‍🏫 Tus Sesiones como Instructor (" + userAlert.getInstructorSessions().size() + ")</h3>");

            for (SessionAlertDTO session : userAlert.getInstructorSessions()) {
                instructorSessionsHtml.append("<div style='background-color: #141414; padding: 20px; border-radius: 8px; margin-bottom: 15px; border-left: 4px solid #aae16b;'>");
                instructorSessionsHtml.append("<h4 style='color: #ffffff; margin: 0 0 10px 0; font-size: 18px;'>" + session.getSessionTitle() + "</h4>");
                instructorSessionsHtml.append("<p style='color: #b0b0b0; margin: 5px 0; font-size: 14px;'><strong style='color: #aae16b;'>Skill:</strong> " + session.getSkillName() + "</p>");
                instructorSessionsHtml.append("<p style='color: #b0b0b0; margin: 5px 0; font-size: 14px;'><strong style='color: #aae16b;'>📅 Fecha:</strong> " + dateFormat.format(session.getScheduledDatetime()) + "</p>");
                instructorSessionsHtml.append("<p style='color: #b0b0b0; margin: 5px 0; font-size: 14px;'><strong style='color: #aae16b;'>⏱️ Duración:</strong> " + session.getDurationMinutes() + " minutos</p>");
                if (session.getVideoCallLink() != null && !session.getVideoCallLink().isEmpty()) {
                    instructorSessionsHtml.append("<p style='margin: 15px 0 0 0;'><a href='" + session.getVideoCallLink() + "' style='display: inline-block; background-color: #504ab7; color: #ffffff; text-decoration: none; padding: 10px 20px; border-radius: 5px; font-size: 14px;'>🎥 Unirse a la Videollamada</a></p>");
                }
                instructorSessionsHtml.append("</div>");
            }
            instructorSessionsHtml.append("</div>");
        }

        // Construir HTML para sesiones como learner
        if (!userAlert.getLearnerSessions().isEmpty()) {
            learnerSessionsHtml.append("<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 20px 0;'>");
            learnerSessionsHtml.append("<h3 style='color: #aae16b; margin-top: 0; font-size: 20px; margin-bottom: 20px;'>📚 Sesiones en las que Participas (" + userAlert.getLearnerSessions().size() + ")</h3>");

            for (SessionAlertDTO session : userAlert.getLearnerSessions()) {
                learnerSessionsHtml.append("<div style='background-color: #141414; padding: 20px; border-radius: 8px; margin-bottom: 15px; border-left: 4px solid #504ab7;'>");
                learnerSessionsHtml.append("<h4 style='color: #ffffff; margin: 0 0 10px 0; font-size: 18px;'>" + session.getSessionTitle() + "</h4>");
                learnerSessionsHtml.append("<p style='color: #b0b0b0; margin: 5px 0; font-size: 14px;'><strong style='color: #aae16b;'>Skill:</strong> " + session.getSkillName() + "</p>");
                learnerSessionsHtml.append("<p style='color: #b0b0b0; margin: 5px 0; font-size: 14px;'><strong style='color: #aae16b;'>👨‍🏫 Instructor:</strong> " + session.getInstructorName() + "</p>");
                learnerSessionsHtml.append("<p style='color: #b0b0b0; margin: 5px 0; font-size: 14px;'><strong style='color: #aae16b;'>📅 Fecha:</strong> " + dateFormat.format(session.getScheduledDatetime()) + "</p>");
                learnerSessionsHtml.append("<p style='color: #b0b0b0; margin: 5px 0; font-size: 14px;'><strong style='color: #aae16b;'>⏱️ Duración:</strong> " + session.getDurationMinutes() + " minutos</p>");
                if (session.getVideoCallLink() != null && !session.getVideoCallLink().isEmpty()) {
                    learnerSessionsHtml.append("<p style='margin: 15px 0 0 0;'><a href='" + session.getVideoCallLink() + "' style='display: inline-block; background-color: #504ab7; color: #ffffff; text-decoration: none; padding: 10px 20px; border-radius: 5px; font-size: 14px;'>🎥 Unirse a la Videollamada</a></p>");
                }
                learnerSessionsHtml.append("</div>");
            }
            learnerSessionsHtml.append("</div>");
        }

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Sesiones Programadas</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "                            <p style='color: #ffffff; margin: 10px 0 0 0; font-size: 16px;'>Sesiones Programadas</p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola, " + userAlert.getFullName() + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Tienes <strong style='color: #aae16b;'>" + userAlert.getTotalSessions() + " sesión(es)</strong> programadas para esta semana en <strong>SkillSwap</strong>." +
                "                            </p>" +
                instructorSessionsHtml +
                learnerSessionsHtml +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + frontendUrl + "/my-sessions' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Todas Mis Sesiones</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0 0 0; text-align: center;'>" +
                "                                ¡No olvides prepararte para tus sesiones!" +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
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
}