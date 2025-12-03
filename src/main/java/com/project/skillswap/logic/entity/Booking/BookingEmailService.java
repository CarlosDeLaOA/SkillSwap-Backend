package com.project.skillswap.logic.entity.Booking;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Service
public class BookingEmailService {
    private static final Logger logger = LoggerFactory.getLogger(BookingEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public BookingEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendBookingConfirmationEmail(
            Long bookingId,
            String learnerFullName,
            String learnerEmail,
            String sessionTitle,
            String sessionDescription,
            String instructorFullName,
            String skillName,
            String categoryName,
            Date scheduledDatetime,
            Integer durationMinutes,
            String videoCallLink,
            String mySessionsLink
    ) throws MessagingException {

        logger.info("[EMAIL] Preparando email de confirmación para: " + learnerEmail);

        String subject = "Confirmación de registro - " + sessionTitle;

        String formattedDate = formatDate(scheduledDatetime);
        String formattedTime = formatTime(scheduledDatetime);
        String duration = durationMinutes + " minutos";

        LearningSession mockSession = createMockSessionForCalendarLink(
                sessionTitle,
                sessionDescription,
                scheduledDatetime,
                durationMinutes,
                videoCallLink
        );
        String googleCalendarLink = buildGoogleCalendarLink(mockSession);

        String htmlContent = buildBookingConfirmationTemplate(
                learnerFullName,
                sessionTitle,
                sessionDescription,
                instructorFullName,
                skillName,
                categoryName,
                formattedDate,
                formattedTime,
                duration,
                videoCallLink,
                mySessionsLink,
                googleCalendarLink
        );

        sendHtmlEmail(learnerEmail, subject, htmlContent);

        logger.info("[EMAIL] Email de confirmación enviado exitosamente a: " + learnerEmail);
    }

    public void sendBookingCancellationEmail(
            String learnerFullName,
            String learnerEmail,
            String sessionTitle,
            Date scheduledDatetime
    ) throws MessagingException {

        logger.info("[EMAIL] Preparando email de cancelación para: " + learnerEmail);

        String subject = "Cancelación de registro - " + sessionTitle;
        String formattedDate = formatDate(scheduledDatetime);
        String sessionsLink = frontendUrl + "/app/sessions";

        String htmlContent = buildBookingCancellationTemplate(
                learnerFullName,
                sessionTitle,
                formattedDate,
                sessionsLink
        );

        sendHtmlEmail(learnerEmail, subject, htmlContent);

        logger.info("[EMAIL] Email de cancelación enviado exitosamente a: " + learnerEmail);
    }

    public void sendGroupBookingConfirmationEmailFromData(Map<String, Object> data) throws MessagingException {

        String personEmail = (String) data.get("personEmail");
        logger.info("[EMAIL] Preparando email grupal para: " + personEmail);

        String subject = "Confirmación de registro grupal - " + data.get("sessionTitle");
        String htmlContent = buildGroupBookingConfirmationTemplateFromData(data);

        sendHtmlEmail(personEmail, subject, htmlContent);

        logger.info("[EMAIL] Email grupal enviado exitosamente a: " + personEmail);
    }

    public void sendWaitlistConfirmationEmail(
            String learnerFullName,
            String learnerEmail,
            String sessionTitle,
            String sessionDescription,
            String instructorFullName,
            String skillName,
            String categoryName,
            Date scheduledDatetime,
            Integer durationMinutes
    ) throws MessagingException {

        logger.info("[EMAIL] Preparando email de lista de espera para: " + learnerEmail);

        String subject = "Unido a lista de espera - " + sessionTitle;

        String formattedDate = formatDate(scheduledDatetime);
        String formattedTime = formatTime(scheduledDatetime);
        String duration = durationMinutes + " minutos";
        String mySessionsLink = frontendUrl + "/app/my-sessions";

        LearningSession mockSession = createMockSessionForCalendarLink(
                sessionTitle,
                sessionDescription,
                scheduledDatetime,
                durationMinutes,
                null
        );
        String googleCalendarLink = buildGoogleCalendarLink(mockSession);

        String htmlContent = buildWaitlistConfirmationTemplate(
                learnerFullName,
                sessionTitle,
                sessionDescription,
                instructorFullName,
                skillName,
                categoryName,
                formattedDate,
                formattedTime,
                duration,
                mySessionsLink,
                googleCalendarLink
        );

        sendHtmlEmail(learnerEmail, subject, htmlContent);

        logger.info("[EMAIL] Email de lista de espera enviado exitosamente a: " + learnerEmail);
    }

    public void sendSpotAvailableEmail(
            String learnerFullName,
            String learnerEmail,
            String sessionTitle,
            Date scheduledDatetime,
            Long sessionId
    ) throws MessagingException {

        logger.info("[EMAIL] Preparando email de cupo disponible para: " + learnerEmail);

        String subject = "¡Cupo disponible! - " + sessionTitle;

        String formattedDate = formatDate(scheduledDatetime);
        String formattedTime = formatTime(scheduledDatetime);
        String sessionLink = frontendUrl + "/app/sessions/" + sessionId;

        LearningSession mockSession = createMockSessionForCalendarLink(
                sessionTitle,
                "",
                scheduledDatetime,
                60,
                null
        );
        String googleCalendarLink = buildGoogleCalendarLink(mockSession);

        String htmlContent = buildSpotAvailableTemplate(
                learnerFullName,
                sessionTitle,
                formattedDate,
                formattedTime,
                sessionLink,
                googleCalendarLink
        );

        sendHtmlEmail(learnerEmail, subject, htmlContent);

        logger.info("[EMAIL] Email de cupo disponible enviado exitosamente a: " + learnerEmail);
    }

    public void sendWaitlistExitConfirmationEmail(
            String learnerFullName,
            String learnerEmail,
            String sessionTitle,
            Date scheduledDatetime
    ) throws MessagingException {

        logger.info("[EMAIL] Preparando email de salida de lista de espera para: " + learnerEmail);

        String subject = "Has salido de la lista de espera - " + sessionTitle;

        String formattedDate = formatDate(scheduledDatetime);
        String formattedTime = formatTime(scheduledDatetime);
        String sessionsLink = frontendUrl + "/app/sessions";

        String htmlContent = buildWaitlistExitConfirmationTemplate(
                learnerFullName,
                sessionTitle,
                formattedDate,
                formattedTime,
                sessionsLink
        );

        sendHtmlEmail(learnerEmail, subject, htmlContent);

        logger.info("[EMAIL] Email de salida de lista de espera enviado exitosamente a: " + learnerEmail);
    }

    public void sendInstructorNotificationEmail(
            String instructorFullName,
            String instructorEmail,
            String sessionTitle,
            Date scheduledDatetime,
            String learnerName,
            boolean isGroup,
            int spotsFreed,
            int maxCapacity,
            int currentBookings
    ) throws MessagingException {

        logger.info("[EMAIL] Preparando email de notificación para instructor: " + instructorEmail);

        String subject = "Cancelación de registro - " + sessionTitle;

        String formattedDate = formatDate(scheduledDatetime);
        String formattedTime = formatTime(scheduledDatetime);
        int availableSpots = maxCapacity - currentBookings;

        LearningSession mockSession = createMockSessionForCalendarLink(
                sessionTitle,
                "",
                scheduledDatetime,
                60,
                null
        );
        String googleCalendarLink = buildGoogleCalendarLink(mockSession);

        String htmlContent = buildInstructorNotificationTemplate(
                instructorFullName,
                sessionTitle,
                formattedDate,
                formattedTime,
                learnerName,
                isGroup,
                spotsFreed,
                availableSpots,
                maxCapacity,
                googleCalendarLink
        );

        sendHtmlEmail(instructorEmail, subject, htmlContent);

        logger.info("[EMAIL] Email de notificación enviado exitosamente a instructor: " + instructorEmail);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        logger.info("[EMAIL] Enviando email a: " + to);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
        logger.info("[EMAIL] Email enviado exitosamente");
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        return sdf.format(date);
    }

    private String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", new Locale("es", "ES"));
        return sdf.format(date);
    }

    private String buildGoogleCalendarLink(LearningSession session) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            String startTime = isoFormat.format(session.getScheduledDatetime());
            long endTimeMillis = session.getScheduledDatetime().getTime() + (session.getDurationMinutes() * 60 * 1000);
            Date endDate = new Date(endTimeMillis);
            String endTime = isoFormat.format(endDate);

            String title = "SkillSwap: " + session.getTitle();
            String description = session.getDescription() != null ? session.getDescription() : "";
            String location = session.getVideoCallLink() != null ? session.getVideoCallLink() : "Online";

            String calendarUrl = "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                    "&text=" + encodeURL(title) +
                    "&dates=" + startTime + "/" + endTime +
                    "&details=" + encodeURL(description) +
                    "&location=" + encodeURL(location) +
                    "&sf=true&output=xml";

            return calendarUrl;
        } catch (Exception e) {
            logger.error("Error generando enlace de Google Calendar: " + e.getMessage());
            return "https://calendar.google.com";
        }
    }

    private String encodeURL(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(str, "UTF-8")
                    .replace("+", "%20")
                    .replace("%21", "!")
                    .replace("%27", "'")
                    .replace("%28", "(")
                    .replace("%29", ")")
                    .replace("%7E", "~");
        } catch (Exception e) {
            return str.replaceAll(" ", "%20")
                    .replaceAll("&", "%26")
                    .replaceAll("'", "%27")
                    .replaceAll("\"", "%22")
                    .replaceAll("\n", "%0A");
        }
    }

    private LearningSession createMockSessionForCalendarLink(String title, String description, Date date, int duration, String videoCallLink) {
        LearningSession mockSession = new LearningSession();
        mockSession.setTitle(title);
        mockSession.setDescription(description);
        mockSession.setScheduledDatetime(date);
        mockSession.setDurationMinutes(duration);
        mockSession.setVideoCallLink(videoCallLink);
        return mockSession;
    }

    private String buildBookingConfirmationTemplate(
            String learnerFullName,
            String sessionTitle,
            String sessionDescription,
            String instructorFullName,
            String skillName,
            String categoryName,
            String formattedDate,
            String formattedTime,
            String duration,
            String videoCallLink,
            String mySessionsLink,
            String googleCalendarLink
    ) {
        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Confirmación de Registro</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>Registro Confirmado</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Hola <strong style='color: #aae16b;'>" + learnerFullName + "</strong>," +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Te has registrado exitosamente en la siguiente sesión de aprendizaje:" +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>" + sessionTitle + "</h3>" +
                "                                <p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'>" + sessionDescription + "</p>" +
                "                                <hr style='border: none; border-top: 1px solid #504ab7; margin: 20px 0;'>" +
                "                                <table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px;'>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; width: 40%;'><strong>Fecha:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + formattedDate + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Hora:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + formattedTime + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Duración:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + duration + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Instructor:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + instructorFullName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Habilidad:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + skillName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Categoría:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + categoryName + "</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #504ab7;'>" +
                "                                <h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'>Enlace de acceso a la sesión:</h4>" +
                "                                <p style='font-size: 14px; word-break: break-all; color: #aae16b; background-color: #141414; padding: 15px; border-radius: 5px; margin: 10px 0;'>" +
                "                                    <a href='" + videoCallLink + "' style='color: #aae16b; text-decoration: none;'>" + videoCallLink + "</a>" +
                "                                </p>" +
                "                                <p style='font-size: 12px; color: #b0b0b0; margin: 5px 0 0 0;'>" +
                "                                    Guarda este enlace en un lugar seguro.  Lo necesitarás para acceder a la sesión." +
                "                                </p>" +
                "                            </div>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + videoCallLink + "' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%, #8ec756 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4); margin: 0 5px;'>Ir a la Videollamada</a>" +
                "                                        <a href='" + googleCalendarLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #6b63d8 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(80, 74, 183, 0.4); margin: 0 5px;'>Agregar a Google Calendar</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #504ab7;'>" +
                "                                <h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'>Recordatorios importantes:</h4>" +
                "                                <ul style='color: #ffffff; font-size: 14px; line-height: 1.8; padding-left: 20px; margin: 10px 0;'>" +
                "                                    <li>Asegúrate de tener una conexión a internet estable</li>" +
                "                                    <li>Prueba tu cámara y micrófono antes de la sesión</li>" +
                "                                    <li>Llega 5 minutos antes para prepararte</li>" +
                "                                    <li>Guarda el enlace de acceso en un lugar seguro</li>" +
                "                                </ul>" +
                "                            </div>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Si necesitas cancelar tu registro, puedes hacerlo desde tu <a href='" + mySessionsLink + "' style='color: #aae16b; text-decoration: none;'>panel de sesiones</a>." +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>" +
                "                                © 2025 SkillSwap.  Todos los derechos reservados." +
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

    private String buildBookingCancellationTemplate(
            String learnerFullName,
            String sessionTitle,
            String formattedDate,
            String sessionsLink
    ) {
        return "<! DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Cancelación de Registro</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #ff6b6b; margin-top: 0; font-size: 24px;'>Registro Cancelado</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Hola <strong style='color: #aae16b;'>" + learnerFullName + "</strong>," +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Tu registro para la siguiente sesión ha sido cancelado:" +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #ff6b6b;'>" +
                "                                <h3 style='color: #ffffff; margin-top: 0; font-size: 18px;'>" + sessionTitle + "</h3>" +
                "                                <p style='font-size: 14px; color: #b0b0b0; margin: 5px 0;'>Fecha: " + formattedDate + "</p>" +
                "                            </div>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Puedes explorar otras sesiones disponibles que podrían interesarte." +
                "                            </p>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + sessionsLink + "' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%, #8ec756 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4);'>Explorar Sesiones</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
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

    private String buildGroupBookingConfirmationTemplateFromData(Map<String, Object> data) {
        String sessionTitle = (String) data.get("sessionTitle");
        String sessionDescription = (String) data.get("sessionDescription");
        String instructorName = (String) data.get("instructorName");
        String skillName = (String) data.get("skillName");
        String categoryName = (String) data.get("categoryName");
        String communityName = (String) data.get("communityName");
        String personFullName = (String) data.get("personFullName");
        String formattedDate = formatDate((Date) data.get("sessionDate"));
        String formattedTime = formatTime((Date) data.get("sessionDate"));
        String duration = data.get("sessionDuration") + " minutos";
        String accessLink = (String) data.get("accessLink");
        String videoCallLink = (String) data.get("videoCallLink");
        LearningSession mockSession = createMockSessionForCalendarLink(sessionTitle, sessionDescription, (Date) data.get("sessionDate"), ((Number) data.get("sessionDuration")).intValue(), videoCallLink);
        String googleCalendarLink = buildGoogleCalendarLink(mockSession);

        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Confirmación de Registro Grupal</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'><table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'><tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "<tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'><h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1></td></tr>" +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'><h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>Registro Grupal Confirmado</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Hola <strong style='color: #aae16b;'>" + personFullName + "</strong>,</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Tu comunidad <strong style='color: #aae16b;'>" + communityName + "</strong> se ha registrado exitosamente en la siguiente sesión de aprendizaje:</p>" +
                "<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'><h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>" + sessionTitle + "</h3>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'>" + sessionDescription + "</p><hr style='border: none; border-top: 1px solid #504ab7; margin: 20px 0;'>" +
                "<table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px;'><tr><td style='color: #aae16b; width: 40%;'><strong>Fecha:</strong></td><td style='color: #ffffff;'>" + formattedDate + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Hora:</strong></td><td style='color: #ffffff;'>" + formattedTime + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Duración:</strong></td><td style='color: #ffffff;'>" + duration + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Instructor:</strong></td><td style='color: #ffffff;'>" + instructorName + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Habilidad:</strong></td><td style='color: #ffffff;'>" + skillName + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Categoría:</strong></td><td style='color: #ffffff;'>" + categoryName + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Comunidad:</strong></td><td style='color: #ffffff;'>" + communityName + "</td></tr></table></div>" +
                "<div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #504ab7;'>" +
                "<h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'>Enlace de acceso a la sesión:</h4>" +
                "<p style='font-size: 14px; word-break: break-all; color: #aae16b; background-color: #141414; padding: 15px; border-radius: 5px; margin: 10px 0;'><a href='" + accessLink + "' style='color: #aae16b; text-decoration: none;'>" + accessLink + "</a></p>" +
                "<p style='font-size: 12px; color: #b0b0b0; margin: 5px 0 0 0;'>Este es tu enlace personal de acceso.Cada miembro de tu comunidad recibirá su propio enlace.</p></div>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'><tr><td align='center'>" +
                "<a href='" + videoCallLink + "' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%, #8ec756 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4); margin: 0 5px;'>Ir a la Videollamada</a>" +
                "<a href='" + googleCalendarLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #6b63d8 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(80, 74, 183, 0.4); margin: 0 5px;'>Agregar a Google Calendar</a>" +
                "</td></tr></table><p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>Asegúrate de coordinar con los demás miembros de tu comunidad para asistir juntos.</p></td></tr>" +
                "<tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'><p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap.Todos los derechos reservados.</p></td></tr></table></td></tr></table></body></html>";
    }

    private String buildWaitlistConfirmationTemplate(
            String learnerFullName,
            String sessionTitle,
            String sessionDescription,
            String instructorFullName,
            String skillName,
            String categoryName,
            String formattedDate,
            String formattedTime,
            String duration,
            String mySessionsLink,
            String googleCalendarLink
    ) {
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Lista de Espera</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'><table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'><tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "<tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'><h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1></td></tr>" +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'><h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>Unido a Lista de Espera</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Hola <strong style='color: #aae16b;'>" + learnerFullName + "</strong>,</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Te has unido exitosamente a la lista de espera de la siguiente sesión:</p>" +
                "<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'><h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>" + sessionTitle + "</h3>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'>" + sessionDescription + "</p><hr style='border: none; border-top: 1px solid #504ab7; margin: 20px 0;'>" +
                "<table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px;'><tr><td style='color: #aae16b; width: 40%;'><strong>Fecha:</strong></td><td style='color: #ffffff;'>" + formattedDate + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Hora:</strong></td><td style='color: #ffffff;'>" + formattedTime + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Duración:</strong></td><td style='color: #ffffff;'>" + duration + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Instructor:</strong></td><td style='color: #ffffff;'>" + instructorFullName + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Habilidad:</strong></td><td style='color: #ffffff;'>" + skillName + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Categoría:</strong></td><td style='color: #ffffff;'>" + categoryName + "</td></tr></table></div>" +
                "<div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #504ab7;'><h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'>¿Qué sigue?</h4>" +
                "<p style='color: #ffffff; font-size: 14px; line-height: 1.8; margin: 10px 0;'>Te notificaremos por email cuando se libere un cupo en esta sesión.   Serás el primero en la lista de espera.</p>" +
                "<p style='color: #ffffff; font-size: 14px; line-height: 1.8; margin: 10px 0;'>Recibirás un enlace de acceso cuando se confirme tu registro.</p></div>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'><tr><td align='center'>" +
                "<a href='" + mySessionsLink + "' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%, #8ec756 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4); margin: 0 5px;'>Ver Mis Sesiones</a>" +
                "<a href='" + googleCalendarLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #6b63d8 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(80, 74, 183, 0.4); margin: 0 5px;'>Agregar a Google Calendar</a>" +
                "</td></tr></table></td></tr><tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'><p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap.Todos los derechos reservados.</p></td></tr></table></td></tr></table></body></html>";
    }

    private String buildSpotAvailableTemplate(
            String learnerFullName,
            String sessionTitle,
            String formattedDate,
            String formattedTime,
            String sessionLink,
            String googleCalendarLink
    ) {
        return "<! DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Cupo Disponible</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'><table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'><tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "<tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'><h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1></td></tr>" +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'><h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Cupo Disponible!  </h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Hola <strong style='color: #aae16b;'>" + learnerFullName + "</strong>,</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>¡Buenas noticias! Se ha liberado un cupo en la sesión en la que estabas en lista de espera:</p>" +
                "<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'><h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>" + sessionTitle + "</h3>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'>Fecha: " + formattedDate + " - " + formattedTime + "</p></div>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Haz clic en el botón de abajo para confirmar tu registro antes de que el cupo se asigne a otro usuario.</p>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'><tr><td align='center'>" +
                "<a href='" + sessionLink + "' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%, #8ec756 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4); margin: 0 5px;'>Confirmar Registro</a>" +
                "<a href='" + googleCalendarLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #6b63d8 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(80, 74, 183, 0.4); margin: 0 5px;'>Agregar a Google Calendar</a>" +
                "</td></tr></table></td></tr><tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'><p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap.Todos los derechos reservados.</p></td></tr></table></td></tr></table></body></html>";
    }

    private String buildWaitlistExitConfirmationTemplate(
            String learnerFullName,
            String sessionTitle,
            String formattedDate,
            String formattedTime,
            String sessionsLink
    ) {
        return "<! DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Salida de Lista de Espera</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'><table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'><tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "<tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'><h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1></td></tr>" +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'><h2 style='color: #ffa500; margin-top: 0; font-size: 24px;'>Has salido de la lista de espera</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Hola <strong style='color: #aae16b;'>" + learnerFullName + "</strong>,</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Has salido exitosamente de la lista de espera de la siguiente sesión:</p>" +
                "<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #ffa500;'><h3 style='color: #ffffff; margin-top: 0; font-size: 18px;'>" + sessionTitle + "</h3>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 5px 0;'>Fecha: " + formattedDate + " - " + formattedTime + "</p></div>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Ya no recibirás notificaciones sobre esta sesión.  Puedes explorar otras sesiones disponibles que podrían interesarte.</p>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'><tr><td align='center'>" +
                "<a href='" + sessionsLink + "' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%, #8ec756 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4);'>Explorar Sesiones</a>" +
                "</td></tr></table></td></tr><tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'><p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap. Todos los derechos reservados.</p></td></tr></table></td></tr></table></body></html>";
    }

    private String buildInstructorNotificationTemplate(
            String instructorFullName,
            String sessionTitle,
            String formattedDate,
            String formattedTime,
            String learnerName,
            boolean isGroup,
            int spotsFreed,
            int availableSpots,
            int maxCapacity,
            String googleCalendarLink
    ) {
        String cancellationType = isGroup ? "grupal" : "individual";
        String spotsText = spotsFreed == 1 ?  "1 cupo ha" : spotsFreed + " cupos han";

        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Notificación de Cancelación</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'><table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'><tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "<tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'><h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1></td></tr>" +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'><h2 style='color: #ffa500; margin-top: 0; font-size: 24px;'>Cancelación de Registro</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Hola <strong style='color: #aae16b;'>" + instructorFullName + "</strong>,</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>Te informamos que <strong style='color: #aae16b;'>" + learnerName + "</strong> ha cancelado su registro " + cancellationType + " en tu sesión:</p>" +
                "<div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #ffa500;'><h3 style='color: #aae16b; margin-top: 0; font-size: 18px;'>" + sessionTitle + "</h3>" +
                "<table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px; margin-top: 15px;'><tr><td style='color: #aae16b; width: 40%;'><strong>Fecha:</strong></td><td style='color: #ffffff;'>" + formattedDate + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong>Hora:</strong></td><td style='color: #ffffff;'>" + formattedTime + "</td></tr></table></div>" +
                "<div style='background-color: #2a2a2a; padding: 20px; border-radius: 8px; margin: 25px 0;'><h4 style='color: #aae16b; margin: 0 0 15px 0; font-size: 16px;'>Estado de la sesión:</h4>" +
                "<table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px;'><tr><td style='color: #b0b0b0;'><strong>Cupos liberados:</strong></td><td style='color: #ffffff; text-align: right;'>" + spotsFreed + "</td></tr>" +
                "<tr><td style='color: #b0b0b0;'><strong>Cupos disponibles:</strong></td><td style='color: #aae16b; text-align: right; font-weight: bold;'>" + availableSpots + "/" + maxCapacity + "</td></tr></table></div>" +
                "<p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0;'>" + spotsText + " sido liberado(s) y ahora está(n) disponible(s) para nuevos registros.</p>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'><tr><td align='center'>" +
                "<a href='" + frontendUrl + "/app/my-sessions' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%, #8ec756 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4); margin: 0 5px;'>Ver Mis Sesiones</a>" +
                "<a href='" + googleCalendarLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #6b63d8 100%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(80, 74, 183, 0.4); margin: 0 5px;'>Agregar a Google Calendar</a>" +
                "</td></tr></table></td></tr><tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'><p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap.Todos los derechos reservados.</p></td></tr></table></td></tr></table></body></html>";
    }
}