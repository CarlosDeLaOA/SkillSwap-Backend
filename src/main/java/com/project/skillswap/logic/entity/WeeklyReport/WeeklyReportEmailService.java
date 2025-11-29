package com.project.skillswap.logic.entity.WeeklyReport;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio para el envío de correos de reportes semanales.
 */
@Service
public class WeeklyReportEmailService {

    //#region Dependencies
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public WeeklyReportEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    //#endregion

    //#region Public Methods
    /**
     * Envía el reporte semanal a un aprendiz.
     *
     * @param toEmail correo del destinatario
     * @param fullName nombre completo del destinatario
     * @param data datos del reporte
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendLearnerWeeklyReport(String toEmail, String fullName, LearnerReportData data) throws MessagingException {
        String subject = "Tu Resumen Semanal en SkillSwap";
        String htmlContent = buildLearnerReportTemplate(fullName, data);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Envía el reporte semanal a un instructor.
     *
     * @param toEmail correo del destinatario
     * @param fullName nombre completo del destinatario
     * @param data datos del reporte
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendInstructorWeeklyReport(String toEmail, String fullName, InstructorReportData data) throws MessagingException {
        String subject = "Tu Resumen Semanal en SkillSwap";
        String htmlContent = buildInstructorReportTemplate(fullName, data);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }
    //#endregion

    //#region Private Methods
    /**
     * Envía un correo en formato HTML.
     *
     * @param to correo del destinatario
     * @param subject asunto del correo
     * @param htmlContent contenido HTML del correo
     * @throws MessagingException si hay un error al enviar el correo
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
     * Construye el template HTML para el reporte de aprendiz.
     *
     * @param fullName nombre del usuario
     * @param data datos del reporte
     * @return contenido HTML del correo
     */
    private String buildLearnerReportTemplate(String fullName, LearnerReportData data) {
        StringBuilder recommendationsHtml = new StringBuilder();
        if (data.getRecommendedSessions() != null && !data.getRecommendedSessions().isEmpty()) {
            for (String session : data.getRecommendedSessions()) {
                recommendationsHtml.append("<li style='margin-bottom: 8px;'>").append(session).append("</li>");
            }
        } else {
            recommendationsHtml.append("<li style='margin-bottom: 8px;'>No hay recomendaciones disponibles esta semana</li>");
        }

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Resumen Semanal</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "                            <p style='color: #ffffff; margin: 10px 0 0 0; font-size: 16px;'>Tu Resumen Semanal</p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola, " + fullName + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Aquí está tu resumen de actividad de esta semana en <strong style='color: #aae16b;'>SkillSwap</strong>." +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 20px; margin-bottom: 20px;'> Tu Actividad</h3>" +
                "                                <table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom: 15px;'>" +
                "                                    <tr>" +
                "                                        <td style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #ffffff; font-size: 15px;'> Credenciales obtenidas:</span>" +
                "                                        </td>" +
                "                                        <td align='right' style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #aae16b; font-size: 18px; font-weight: bold;'>" + data.getCredentialsObtained() + "</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #ffffff; font-size: 15px;'> Sesiones asistidas:</span>" +
                "                                        </td>" +
                "                                        <td align='right' style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #aae16b; font-size: 18px; font-weight: bold;'>" + data.getSessionsAttended() + "</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='padding: 12px 0;'>" +
                "                                            <span style='color: #ffffff; font-size: 15px;'> Skillcoins invertidos:</span>" +
                "                                        </td>" +
                "                                        <td align='right' style='padding: 12px 0;'>" +
                "                                            <span style='color: #aae16b; font-size: 18px; font-weight: bold;'>" + data.getSkillcoinsInvested() + "</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +
                "                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 20px; margin-bottom: 15px;'> Recomendaciones para la Próxima Semana</h3>" +
                "                                <p style='color: #b0b0b0; font-size: 14px; margin-bottom: 15px;'>Basado en tu idioma preferido y tus habilidades seleccionadas:</p>" +
                "                                <ul style='color: #ffffff; font-size: 15px; line-height: 1.8; padding-left: 20px; margin: 0;'>" +
                recommendationsHtml +
                "                                </ul>" +
                "                            </div>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + frontendUrl + "/dashboard' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver mi Dashboard</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0 0 0; text-align: center;'>" +
                "                                ¡Sigue aprendiendo y creciendo con SkillSwap!" +
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
     * Construye el template HTML para el reporte de instructor.
     *
     * @param fullName nombre del usuario
     * @param data datos del reporte
     * @return contenido HTML del correo
     */
    private String buildInstructorReportTemplate(String fullName, InstructorReportData data) {
        String ratingStars = generateStarRating(data.getAverageRating());

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Resumen Semanal</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "                            <p style='color: #ffffff; margin: 10px 0 0 0; font-size: 16px;'>Tu Resumen Semanal de Instructor</p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola, " + fullName + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Aquí está tu resumen de enseñanza de esta semana en <strong style='color: #aae16b;'>SkillSwap</strong>." +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 20px; margin-bottom: 20px;'> Tu Desempeño</h3>" +
                "                                <table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom: 15px;'>" +
                "                                    <tr>" +
                "                                        <td style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #ffffff; font-size: 15px;'> Sesiones impartidas:</span>" +
                "                                        </td>" +
                "                                        <td align='right' style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #aae16b; font-size: 18px; font-weight: bold;'>" + data.getSessionsTaught() + "</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #ffffff; font-size: 15px;'>️ Horas de enseñanza:</span>" +
                "                                        </td>" +
                "                                        <td align='right' style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #aae16b; font-size: 18px; font-weight: bold;'>" + data.getTotalHoursTaught() + "h</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #ffffff; font-size: 15px;'> Skillcoins ganados:</span>" +
                "                                        </td>" +
                "                                        <td align='right' style='padding: 12px 0; border-bottom: 1px solid #504ab7;'>" +
                "                                            <span style='color: #aae16b; font-size: 18px; font-weight: bold;'>" + data.getSkillcoinsEarned() + "</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='padding: 12px 0;'>" +
                "                                            <span style='color: #ffffff; font-size: 15px;'> Reseñas recibidas:</span>" +
                "                                        </td>" +
                "                                        <td align='right' style='padding: 12px 0;'>" +
                "                                            <span style='color: #aae16b; font-size: 18px; font-weight: bold;'>" + data.getTotalReviews() + "</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +
                "                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; text-align: center;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 20px; margin-bottom: 15px;'> Calificación Promedio de la Semana</h3>" +
                "                                <div style='font-size: 32px; margin: 15px 0;'>" + ratingStars + "</div>" +
                "                                <p style='color: #ffffff; font-size: 24px; font-weight: bold; margin: 10px 0;'>" + String.format("%.1f", data.getAverageRating()) + " / 5.0</p>" +
                "                                <p style='color: #b0b0b0; font-size: 14px; margin: 10px 0;'>Basado en " + data.getTotalReviews() + " reseña(s)</p>" +
                "                            </div>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + frontendUrl + "/instructor/dashboard' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver mi Dashboard</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0 0 0; text-align: center;'>" +
                "                                ¡Gracias por compartir tu conocimiento en SkillSwap!" +
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
     * Genera representación visual de estrellas basado en calificación.
     *
     * @param rating calificación numérica
     * @return string con estrellas
     */
    private String generateStarRating(double rating) {
        int fullStars = (int) rating;
        boolean hasHalfStar = (rating - fullStars) >= 0.5;
        StringBuilder stars = new StringBuilder();

        for (int i = 0; i < fullStars; i++) {
            stars.append("⭐");
        }
        if (hasHalfStar && fullStars < 5) {
            stars.append("⭐");
        }
        for (int i = fullStars + (hasHalfStar ? 1 : 0); i < 5; i++) {
            stars.append("☆");
        }

        return stars.toString();
    }
    //#endregion

    //#region Inner Classes
    /**
     * Clase que encapsula los datos del reporte de un aprendiz.
     */
    public static class LearnerReportData {
        private final int credentialsObtained;
        private final int sessionsAttended;
        private final int skillcoinsInvested;
        private final List<String> recommendedSessions;

        public LearnerReportData(int credentialsObtained, int sessionsAttended, int skillcoinsInvested, List<String> recommendedSessions) {
            this.credentialsObtained = credentialsObtained;
            this.sessionsAttended = sessionsAttended;
            this.skillcoinsInvested = skillcoinsInvested;
            this.recommendedSessions = recommendedSessions;
        }

        public int getCredentialsObtained() {
            return credentialsObtained;
        }

        public int getSessionsAttended() {
            return sessionsAttended;
        }

        public int getSkillcoinsInvested() {
            return skillcoinsInvested;
        }

        public List<String> getRecommendedSessions() {
            return recommendedSessions;
        }
    }

    /**
     * Clase que encapsula los datos del reporte de un instructor.
     */
    public static class InstructorReportData {
        private final int sessionsTaught;
        private final int totalHoursTaught;
        private final int skillcoinsEarned;
        private final double averageRating;
        private final int totalReviews;

        public InstructorReportData(int sessionsTaught, int totalHoursTaught, int skillcoinsEarned, double averageRating, int totalReviews) {
            this.sessionsTaught = sessionsTaught;
            this.totalHoursTaught = totalHoursTaught;
            this.skillcoinsEarned = skillcoinsEarned;
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
        }

        public int getSessionsTaught() {
            return sessionsTaught;
        }

        public int getTotalHoursTaught() {
            return totalHoursTaught;
        }

        public int getSkillcoinsEarned() {
            return skillcoinsEarned;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public int getTotalReviews() {
            return totalReviews;
        }
    }
    //#endregion
}