package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@Service
public class BookingEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${mail.from:${spring.mail.username}}")
    private String fromEmail;

    public BookingEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ==================== PUBLIC METHODS ====================

    public void sendBookingConfirmationEmail(Booking booking, Person person) throws MessagingException {
        LearningSession session = booking.getLearningSession();
        String subject = " Registro Confirmado - " + session.getTitle();
        String htmlContent = buildBookingConfirmationTemplate(booking, person);
        sendHtmlEmail(person.getEmail(), subject, htmlContent);
    }

    public void sendBookingCancellationEmail(Booking booking, Person person) throws MessagingException {
        String subject = " Registro Cancelado - " + booking.getLearningSession().getTitle();
        String htmlContent = buildBookingCancellationTemplate(booking, person);
        sendHtmlEmail(person.getEmail(), subject, htmlContent);
    }

    public void sendGroupBookingConfirmationEmail(Booking booking, Person person, String communityName) throws MessagingException {
        String subject = " Registro Grupal Confirmado - " + booking.getLearningSession().getTitle();
        String htmlContent = buildGroupBookingConfirmationTemplate(booking, person, communityName);
        sendHtmlEmail(person.getEmail(), subject, htmlContent);
    }

    public void sendGroupBookingConfirmationEmailFromData(Map<String, Object> data) throws MessagingException {
        String subject = " Registro Grupal Confirmado - " + data.get("sessionTitle");
        String htmlContent = buildGroupBookingConfirmationTemplateFromData(data);
        sendHtmlEmail((String) data.get("personEmail"), subject, htmlContent);
    }

    public void sendWaitlistConfirmationEmail(Booking booking, Person person) throws MessagingException {
        String subject = " Lista de Espera - " + booking.getLearningSession().getTitle();
        String htmlContent = buildWaitlistConfirmationTemplate(booking, person);
        sendHtmlEmail(person.getEmail(), subject, htmlContent);
    }

    public void sendSpotAvailableEmail(Booking booking, Person person) throws MessagingException {
        String subject = " ¡Cupo Disponible! - " + booking.getLearningSession().getTitle();
        String htmlContent = buildSpotAvailableTemplate(booking, person);
        sendHtmlEmail(person.getEmail(), subject, htmlContent);
    }

    public void sendWaitlistExitConfirmationEmail(Booking booking, Person person) throws MessagingException {
        String subject = "Has salido de la lista de espera - " + booking.getLearningSession().getTitle();
        String htmlContent = buildWaitlistExitConfirmationTemplate(booking, person);
        sendHtmlEmail(person.getEmail(), subject, htmlContent);
    }

    public void sendInstructorNotificationEmail(LearningSession session, Person instructorPerson,
                                                String learnerName, boolean isGroup, int spotsFreed) throws MessagingException {
        String subject = " Cancelación de registro - " + session.getTitle();
        String htmlContent = buildInstructorNotificationTemplate(session, instructorPerson, learnerName, isGroup, spotsFreed);
        sendHtmlEmail(instructorPerson.getEmail(), subject, htmlContent);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
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
            String title = URLEncoder.encode(session.getTitle(), StandardCharsets.UTF_8.toString());
            String description = URLEncoder.encode(
                    "Sesión de SkillSwap: " + (session.getDescription() != null ? session.getDescription() : ""),
                    StandardCharsets.UTF_8.toString()
            );
            String location = URLEncoder.encode(
                    session.getVideoCallLink() != null ? session.getVideoCallLink() : "Online",
                    StandardCharsets.UTF_8.toString()
            );

            Date startDate = session.getScheduledDatetime();
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.MINUTE, session.getDurationMinutes());
            Date endDate = cal.getTime();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
            String start = sdf.format(startDate);
            String end = sdf.format(endDate);

            return String.format(
                    "https://calendar.google.com/calendar/render?action=TEMPLATE&text=%s&dates=%s/%s&details=%s&location=%s",
                    title, start, end, description, location
            );
        } catch (UnsupportedEncodingException e) {
            return "https://calendar.google.com";
        }
    }

    private String buildGoogleCalendarLinkFromData(Map<String, Object> data) {
        try {
            String title = URLEncoder.encode((String) data.get("sessionTitle"), StandardCharsets.UTF_8.toString());
            String description = URLEncoder.encode(
                    "Sesión de SkillSwap: " + (data.get("sessionDescription") != null ? (String) data.get("sessionDescription") : ""),
                    StandardCharsets.UTF_8.toString()
            );
            String location = URLEncoder.encode(
                    data.get("videoCallLink") != null ? (String) data.get("videoCallLink") : "Online",
                    StandardCharsets.UTF_8.toString()
            );

            Date startDate = (Date) data.get("sessionDate");
            Integer durationMinutes = (Integer) data.get("sessionDuration");

            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.MINUTE, durationMinutes);
            Date endDate = cal.getTime();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
            String start = sdf.format(startDate);
            String end = sdf.format(endDate);

            return String.format(
                    "https://calendar.google.com/calendar/render?action=TEMPLATE&text=%s&dates=%s/%s&details=%s&location=%s",
                    title, start, end, description, location
            );
        } catch (UnsupportedEncodingException e) {
            return "https://calendar.google.com";
        }
    }

    // ==================== PAYMENT SECTIONS ====================

    private String buildPaymentSection(BigDecimal skillcoinsCost, Person person) {
        BigDecimal newBalance = BigDecimal.ZERO;
        if (person.getLearner() != null && person.getLearner().getSkillcoinsBalance() != null) {
            newBalance = person.getLearner().getSkillcoinsBalance();
        }

        return "<div style='background-color: #2a2a2a; padding: 20px; border-radius: 8px; margin: 25px 0; border: 2px solid #FFD700;'>" +
                "<h4 style='color: #FFD700; margin-top: 0; font-size: 16px;'> Detalles del Pago</h4>" +
                "<table width='100%' cellpadding='8' cellspacing='0' style='font-size: 14px;'>" +
                "<tr><td style='color: #b0b0b0;'>Tipo de sesión:</td><td style='color: #FFD700; text-align: right; font-weight: bold;'>Premium</td></tr>" +
                "<tr><td style='color: #b0b0b0;'>Costo:</td><td style='color: #ffffff; text-align: right; font-weight: bold;'>" + skillcoinsCost.intValue() + " SkillCoins</td></tr>" +
                "<tr style='border-top: 1px solid #444;'><td style='color: #b0b0b0; padding-top: 10px;'>Tu nuevo balance:</td><td style='color: #aae16b; text-align: right; font-weight: bold; padding-top: 10px;'>" + newBalance.intValue() + " SkillCoins</td></tr>" +
                "</table>" +
                "<p style='font-size: 12px; color: #888; margin: 15px 0 0 0; text-align: center;'> Pago procesado exitosamente</p>" +
                "</div>";
    }

    private String buildPaymentSectionFromData(BigDecimal skillcoinsCost, BigDecimal newBalance) {
        int balanceValue = newBalance != null ? newBalance.intValue() : 0;

        return "<div style='background-color: #2a2a2a; padding: 20px; border-radius: 8px; margin: 25px 0; border: 2px solid #FFD700;'>" +
                "<h4 style='color: #FFD700; margin-top: 0; font-size: 16px;'> Detalles del Pago</h4>" +
                "<table width='100%' cellpadding='8' cellspacing='0' style='font-size: 14px;'>" +
                "<tr><td style='color: #b0b0b0;'>Tipo de sesión:</td><td style='color: #FFD700; text-align: right; font-weight: bold;'>Premium</td></tr>" +
                "<tr><td style='color: #b0b0b0;'>Costo:</td><td style='color: #ffffff; text-align: right; font-weight: bold;'>" + skillcoinsCost.intValue() + " SkillCoins</td></tr>" +
                "<tr style='border-top: 1px solid #444;'><td style='color: #b0b0b0; padding-top: 10px;'>Tu nuevo balance:</td><td style='color: #aae16b; text-align: right; font-weight: bold; padding-top: 10px;'>" + balanceValue + " SkillCoins</td></tr>" +
                "</table>" +
                "<p style='font-size: 12px; color: #888; margin: 15px 0 0 0; text-align: center;'> Pago procesado exitosamente</p>" +
                "</div>";
    }

    private String buildRefundSection(BigDecimal skillcoinsCost, Person person) {
        BigDecimal newBalance = BigDecimal.ZERO;
        if (person.getLearner() != null && person.getLearner().getSkillcoinsBalance() != null) {
            newBalance = person.getLearner().getSkillcoinsBalance();
        }

        return "<div style='background-color: #2a2a2a; padding: 20px; border-radius: 8px; margin: 25px 0; border: 2px solid #aae16b;'>" +
                "<h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'> Reembolso Procesado</h4>" +
                "<table width='100%' cellpadding='8' cellspacing='0' style='font-size: 14px;'>" +
                "<tr><td style='color: #b0b0b0;'>SkillCoins reembolsados:</td><td style='color: #aae16b; text-align: right; font-weight: bold;'>+" + skillcoinsCost.intValue() + " SkillCoins</td></tr>" +
                "<tr style='border-top: 1px solid #444;'><td style='color: #b0b0b0; padding-top: 10px;'>Tu nuevo balance:</td><td style='color: #aae16b; text-align: right; font-weight: bold; padding-top: 10px;'>" + newBalance.intValue() + " SkillCoins</td></tr>" +
                "</table>" +
                "<p style='font-size: 12px; color: #888; margin: 15px 0 0 0; text-align: center;'> Los SkillCoins han sido devueltos a tu cuenta</p>" +
                "</div>";
    }

    // ==================== EMAIL HEADER/FOOTER ====================

    private String getEmailHeader() {
        return "<!  DOCTYPE html>" +
                "<html lang='es'>" +
                "<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "<tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "<h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "</td></tr>";
    }

    private String getEmailFooter() {
        return "<tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "<p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap. Todos los derechos reservados.</p>" +
                "</td></tr></table></td></tr></table></body></html>";
    }

    // ==================== BOOKING CONFIRMATION ====================

    private String buildBookingConfirmationTemplate(Booking booking, Person person) {
        LearningSession session = booking.getLearningSession();

        String sessionTitle = session.getTitle();
        String sessionDescription = session.getDescription();
        String instructorName = session.getInstructor().getPerson().getFullName();
        String skillName = session.getSkill().getName();
        String categoryName = session.getSkill().getKnowledgeArea().getName();
        String formattedDate = formatDate(session.getScheduledDatetime());
        String formattedTime = formatTime(session.getScheduledDatetime());
        String duration = session.getDurationMinutes() + " minutos";

        String videoCallLink = session.getVideoCallLink();
        String mySessionsLink = frontendUrl + "/app/my-sessions";
        String googleCalendarLink = buildGoogleCalendarLink(session);

        boolean isPremium = Boolean.TRUE.equals(session.getIsPremium());
        BigDecimal skillcoinsCost = session.getSkillcoinsCost();
        String sessionTypeLabel = isPremium ? "Premium" : "Gratuita";
        String sessionTypeColor = isPremium ? "#FFD700" : "#aae16b";

        String paymentSection = "";
        if (isPremium && skillcoinsCost != null && skillcoinsCost.compareTo(BigDecimal.ZERO) > 0) {
            paymentSection = buildPaymentSection(skillcoinsCost, person);
        }

        return getEmailHeader() +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "<h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'> Registro Confirmado</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Hola <strong style='color: #aae16b;'>" + person.getFullName() + "</strong>," +
                "</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Te has registrado exitosamente en la siguiente sesión de aprendizaje:" +
                "</p>" +

                "<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>" +
                "<h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>" + sessionTitle + "</h3>" +
                "<span style='display: inline-block; background-color: " + sessionTypeColor + "; color: #141414; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 15px;'>" + sessionTypeLabel + "</span>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'>" + sessionDescription + "</p>" +
                "<hr style='border: none; border-top: 1px solid #504ab7; margin: 20px 0;'>" +
                "<table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px;'>" +
                "<tr><td style='color: #aae16b; width: 40%;'><strong> Fecha:</strong></td><td style='color: #ffffff;'>" + formattedDate + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Hora:</strong></td><td style='color: #ffffff;'>" + formattedTime + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Duración:</strong></td><td style='color: #ffffff;'>" + duration + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Instructor:</strong></td><td style='color: #ffffff;'>" + instructorName + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Habilidad:</strong></td><td style='color: #ffffff;'>" + skillName + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Categoría:</strong></td><td style='color: #ffffff;'>" + categoryName + "</td></tr>" +
                "</table></div>" +

                paymentSection +

                "<div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #504ab7;'>" +
                "<h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'> Enlace de acceso:</h4>" +
                "<p style='font-size: 14px; word-break: break-all; color: #aae16b; background-color: #141414; padding: 15px; border-radius: 5px; margin: 10px 0; text-align: center;'>" +
                "<a href='" + videoCallLink + "' style='color: #aae16b; text-decoration: none;'>" + videoCallLink + "</a></p></div>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "<tr>" +
                "<td align='center'>" +
                "<table cellpadding='0' cellspacing='0'>" +
                "<tr>" +
                "<td style='padding-right: 10px;'>" +
                "<a href='" + videoCallLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 30px; border-radius: 5px; font-size: 14px; font-weight: bold; border: 2px solid #aae16b; min-width: 180px; text-align: center;'>Unirse a Videollamada</a>" +
                "</td>" +
                "<td style='padding-left: 10px;'>" +
                "<a href='" + googleCalendarLink + "' style='display: inline-block; background-color: #39434b; color: #aae16b; text-decoration: none; padding: 15px 30px; border-radius: 5px; font-size: 14px; font-weight: bold; border: 2px solid #aae16b; min-width: 180px; text-align: center;'> Agregar a Calendar</a>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +

                "<div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #504ab7;'>" +
                "<h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'> Recordatorios:</h4>" +
                "<ul style='color: #ffffff; font-size: 14px; line-height: 1.8; padding-left: 20px; margin: 10px 0;'>" +
                "<li>Asegúrate de tener una conexión estable</li>" +
                "<li>Prueba tu cámara y micrófono antes</li>" +
                "<li>Llega 5 minutos antes</li></ul></div>" +

                "<p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "Si necesitas cancelar, hazlo desde tu <a href='" + mySessionsLink + "' style='color: #aae16b;'>panel de sesiones</a>.</p>" +
                "</td></tr>" + getEmailFooter();
    }

    // ==================== BOOKING CANCELLATION ====================

    private String buildBookingCancellationTemplate(Booking booking, Person person) {
        LearningSession session = booking.getLearningSession();
        String sessionTitle = session.getTitle();
        String formattedDate = formatDate(session.getScheduledDatetime());
        String formattedTime = formatTime(session.getScheduledDatetime());
        String sessionsLink = frontendUrl + "/app/sessions";

        boolean isPremium = Boolean.TRUE.equals(session.getIsPremium());
        BigDecimal skillcoinsCost = session.getSkillcoinsCost();

        String refundSection = "";
        if (isPremium && skillcoinsCost != null && skillcoinsCost.compareTo(BigDecimal.ZERO) > 0) {
            refundSection = buildRefundSection(skillcoinsCost, person);
        }

        return getEmailHeader() +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "<h2 style='color: #ff6b6b; margin-top: 0; font-size: 24px;'> Registro Cancelado</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Hola <strong style='color: #aae16b;'>" + person.getFullName() + "</strong>," +
                "</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Tu registro para la siguiente sesión ha sido cancelado:" +
                "</p>" +

                "<div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #ff6b6b;'>" +
                "<h3 style='color: #ffffff; margin-top: 0; font-size: 18px;'>" + sessionTitle + "</h3>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 5px 0;'> " + formattedDate + " - " + formattedTime + "</p>" +
                (isPremium ? "<span style='display: inline-block; background-color: #FFD700; color: #141414; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-top: 10px;'>Premium</span>" : "") +
                "</div>" +

                refundSection +

                "<div style='background-color: #2a2a2a; padding: 15px; border-radius: 8px; margin: 20px 0; border: 1px solid #ff6b6b;'>" +
                "<p style='color: #ff6b6b; margin: 0; font-size: 14px;'>" +
                "<strong> Recordatorio:</strong> Si agregaste esta sesión a tu Google Calendar, recuerda eliminarla manualmente." +
                "</p></div>" +

                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Puedes explorar otras sesiones disponibles.</p>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "<tr><td align='center'>" +
                "<a href='" + sessionsLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold; border: 2px solid #aae16b;'>Explorar Sesiones</a>" +
                "</td></tr></table>" +
                "</td></tr>" + getEmailFooter();
    }

    // ==================== GROUP BOOKING ====================

    private String buildGroupBookingConfirmationTemplate(Booking booking, Person person, String communityName) {
        LearningSession session = booking.getLearningSession();
        String googleCalendarLink = buildGoogleCalendarLink(session);

        String videoCallLink = session.getVideoCallLink();

        boolean isPremium = Boolean.TRUE.equals(session.getIsPremium());
        BigDecimal skillcoinsCost = session.getSkillcoinsCost();
        String sessionTypeLabel = isPremium ? "Premium" : "Gratuita";
        String sessionTypeColor = isPremium ? "#FFD700" : "#aae16b";

        String paymentSection = "";
        if (isPremium && skillcoinsCost != null && skillcoinsCost.compareTo(BigDecimal.ZERO) > 0) {
            paymentSection = buildPaymentSection(skillcoinsCost, person);
        }

        return getEmailHeader() +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "<h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'> Registro Grupal Confirmado</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Hola <strong style='color: #aae16b;'>" + person.getFullName() + "</strong>," +
                "</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Tu comunidad <strong style='color: #aae16b;'>" + communityName + "</strong> se ha registrado exitosamente:" +
                "</p>" +

                "<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>" +
                "<h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>" + session.getTitle() + "</h3>" +
                "<span style='display: inline-block; background-color: " + sessionTypeColor + "; color: #141414; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 15px;'>" + sessionTypeLabel + "</span>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'>" + session.getDescription() + "</p>" +
                "<hr style='border: none; border-top: 1px solid #504ab7; margin: 20px 0;'>" +
                "<table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px;'>" +
                "<tr><td style='color: #aae16b; width: 40%;'><strong> Fecha:</strong></td><td style='color: #ffffff;'>" + formatDate(session.getScheduledDatetime()) + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Hora:</strong></td><td style='color: #ffffff;'>" + formatTime(session.getScheduledDatetime()) + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Duración:</strong></td><td style='color: #ffffff;'>" + session.getDurationMinutes() + " minutos</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Instructor:</strong></td><td style='color: #ffffff;'>" + session.getInstructor().getPerson().getFullName() + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Comunidad:</strong></td><td style='color: #ffffff;'>" + communityName + "</td></tr>" +
                "</table></div>" +

                paymentSection +

                "<div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #504ab7;'>" +
                "<h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'> Tu enlace de acceso:</h4>" +
                "<p style='font-size: 14px; word-break: break-all; color: #aae16b; background-color: #141414; padding: 15px; border-radius: 5px; margin: 10px 0; text-align: center;'>" +
                "<a href='" + videoCallLink + "' style='color: #aae16b; text-decoration: none;'>" + videoCallLink + "</a></p></div>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "<tr>" +
                "<td align='center'>" +
                "<table cellpadding='0' cellspacing='0'>" +
                "<tr>" +
                "<td style='padding-right: 10px;'>" +
                "<a href='" + videoCallLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 30px; border-radius: 5px; font-size: 14px; font-weight: bold; border: 2px solid #aae16b; min-width: 180px; text-align: center;'> Unirse a Videollamada</a>" +
                "</td>" +
                "<td style='padding-left: 10px;'>" +
                "<a href='" + googleCalendarLink + "' style='display: inline-block; background-color: #39434b; color: #aae16b; text-decoration: none; padding: 15px 30px; border-radius: 5px; font-size: 14px; font-weight: bold; border: 2px solid #aae16b; min-width: 180px; text-align: center;'> Agregar a Calendar</a>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +

                "<p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>Coordina con tu comunidad para asistir juntos.</p>" +
                "</td></tr>" + getEmailFooter();
    }

    private String buildGroupBookingConfirmationTemplateFromData(Map<String, Object> data) {
        String googleCalendarLink = buildGoogleCalendarLinkFromData(data);
        Boolean isPremium = (Boolean) data.getOrDefault("isPremium", false);
        BigDecimal skillcoinsCost = (BigDecimal) data.get("skillcoinsCost");
        BigDecimal newBalance = (BigDecimal) data.get("newBalance");
        String sessionTypeLabel = Boolean.TRUE.equals(isPremium) ? "Premium" : "Gratuita";
        String sessionTypeColor = Boolean.TRUE.equals(isPremium) ? "#FFD700" : "#aae16b";

        String videoCallLink = (String) data.get("videoCallLink");

        String paymentSection = "";
        if (Boolean.TRUE.equals(isPremium) && skillcoinsCost != null && skillcoinsCost.compareTo(BigDecimal.ZERO) > 0) {
            paymentSection = buildPaymentSectionFromData(skillcoinsCost, newBalance);
        }

        return getEmailHeader() +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "<h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'> Registro Grupal Confirmado</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Hola <strong style='color: #aae16b;'>" + data.get("personFullName") + "</strong>," +
                "</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Tu comunidad <strong style='color: #aae16b;'>" + data.get("communityName") + "</strong> se ha registrado exitosamente:" +
                "</p>" +

                "<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>" +
                "<h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>" + data.get("sessionTitle") + "</h3>" +
                "<span style='display: inline-block; background-color: " + sessionTypeColor + "; color: #141414; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 15px;'>" + sessionTypeLabel + "</span>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'>" + data.get("sessionDescription") + "</p>" +
                "<hr style='border: none; border-top: 1px solid #504ab7; margin: 20px 0;'>" +
                "<table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px;'>" +
                "<tr><td style='color: #aae16b; width: 40%;'><strong> Fecha:</strong></td><td style='color: #ffffff;'>" + formatDate((Date) data.get("sessionDate")) + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Hora:</strong></td><td style='color: #ffffff;'>" + formatTime((Date) data.get("sessionDate")) + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Instructor:</strong></td><td style='color: #ffffff;'>" + data.get("instructorName") + "</td></tr>" +
                "<tr><td style='color: #aae16b;'><strong> Comunidad:</strong></td><td style='color: #ffffff;'>" + data.get("communityName") + "</td></tr>" +
                "</table></div>" +

                paymentSection +

                "<div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #504ab7;'>" +
                "<h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'> Tu enlace de acceso:</h4>" +
                "<p style='font-size: 14px; word-break: break-all; color: #aae16b; background-color: #141414; padding: 15px; border-radius: 5px; margin: 10px 0; text-align: center;'>" +
                "<a href='" + videoCallLink + "' style='color: #aae16b; text-decoration: none;'>" + videoCallLink + "</a></p></div>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "<tr>" +
                "<td align='center'>" +
                "<table cellpadding='0' cellspacing='0'>" +
                "<tr>" +
                "<td style='padding-right: 10px;'>" +
                "<a href='" + videoCallLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 30px; border-radius: 5px; font-size: 14px; font-weight: bold; border: 2px solid #aae16b; min-width: 180px; text-align: center;'> Unirse a Videollamada</a>" +
                "</td>" +
                "<td style='padding-left: 10px;'>" +
                "<a href='" + googleCalendarLink + "' style='display: inline-block; background-color: #39434b; color: #aae16b; text-decoration: none; padding: 15px 30px; border-radius: 5px; font-size: 14px; font-weight: bold; border: 2px solid #aae16b; min-width: 180px; text-align: center;'> Agregar a Calendar</a>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +

                "<p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>Coordina con tu comunidad para asistir juntos.</p>" +
                "</td></tr>" + getEmailFooter();
    }

    // ==================== WAITLIST ====================

    private String buildWaitlistConfirmationTemplate(Booking booking, Person person) {
        LearningSession session = booking.getLearningSession();
        boolean isPremium = Boolean.TRUE.equals(session.getIsPremium());
        BigDecimal skillcoinsCost = session.getSkillcoinsCost();
        String sessionTypeLabel = isPremium ?  "Premium" : "Gratuita";
        String sessionTypeColor = isPremium ? "#FFD700" : "#aae16b";

        String premiumNote = "";
        if (isPremium && skillcoinsCost != null && skillcoinsCost.compareTo(BigDecimal.ZERO) > 0) {
            premiumNote = "<div style='background-color: #2a2a2a; padding: 15px; border-radius: 8px; margin: 20px 0; border: 1px solid #FFD700;'>" +
                    "<p style='color: #FFD700; margin: 0; font-size: 14px;'><strong> Nota:</strong> Se te cobrarán <strong>" + skillcoinsCost.intValue() + " SkillCoins</strong> cuando se confirme tu registro.</p></div>";
        }

        return getEmailHeader() +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "<h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>⏳ Unido a Lista de Espera</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Hola <strong style='color: #aae16b;'>" + person.getFullName() + "</strong>," +
                "</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Te has unido a la lista de espera de:" +
                "</p>" +

                "<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>" +
                "<h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>" + session.getTitle() + "</h3>" +
                "<span style='display: inline-block; background-color: " + sessionTypeColor + "; color: #141414; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 15px;'>" + sessionTypeLabel + "</span>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'> " + formatDate(session.getScheduledDatetime()) + " - " + formatTime(session.getScheduledDatetime()) + "</p>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 5px 0;'> " + session.getInstructor().getPerson().getFullName() + "</p>" +
                "</div>" +

                premiumNote +

                "<div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #504ab7;'>" +
                "<h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'>¿Qué sigue? </h4>" +
                "<p style='color: #ffffff; font-size: 14px; line-height: 1.8; margin: 10px 0;'>" +
                "Te notificaremos por email cuando se libere un cupo. Recibirás tu enlace de acceso cuando se confirme tu registro.</p></div>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "<tr><td align='center'>" +
                "<a href='" + frontendUrl + "/app/my-sessions' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold; border: 2px solid #aae16b;'>Ver Mis Sesiones</a>" +
                "</td></tr></table>" +
                "</td></tr>" + getEmailFooter();
    }

    private String buildSpotAvailableTemplate(Booking booking, Person person) {
        LearningSession session = booking.getLearningSession();
        String googleCalendarLink = buildGoogleCalendarLink(session);

        String videoCallLink = session.getVideoCallLink();

        return getEmailHeader() +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "<h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'> ¡Cupo Disponible!  </h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Hola <strong style='color: #aae16b;'>" + person.getFullName() + "</strong>," +
                "</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "¡Buenas noticias! Se ha liberado un cupo y tu registro ha sido confirmado:" +
                "</p>" +

                "<div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>" +
                "<h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>" + session.getTitle() + "</h3>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'> " + formatDate(session.getScheduledDatetime()) + " - " + formatTime(session.getScheduledDatetime()) + "</p>" +
                "</div>" +

                "<div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #504ab7;'>" +
                "<h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'> Tu enlace de acceso:</h4>" +
                "<p style='font-size: 14px; word-break: break-all; color: #aae16b; background-color: #141414; padding: 15px; border-radius: 5px; margin: 10px 0; text-align: center;'>" +
                "<a href='" + videoCallLink + "' style='color: #aae16b; text-decoration: none;'>" + videoCallLink + "</a></p></div>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "<tr>" +
                "<td align='center'>" +
                "<table cellpadding='0' cellspacing='0'>" +
                "<tr>" +
                "<td style='padding-right: 10px;'>" +
                "<a href='" + videoCallLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 30px; border-radius: 5px; font-size: 14px; font-weight: bold; border: 2px solid #aae16b; min-width: 180px; text-align: center;'> Unirse a Videollamada</a>" +
                "</td>" +
                "<td style='padding-left: 10px;'>" +
                "<a href='" + googleCalendarLink + "' style='display: inline-block; background-color: #39434b; color: #aae16b; text-decoration: none; padding: 15px 30px; border-radius: 5px; font-size: 14px; font-weight: bold; border: 2px solid #aae16b; min-width: 180px; text-align: center;'> Agregar a Calendar</a>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td></tr>" + getEmailFooter();
    }

    private String buildWaitlistExitConfirmationTemplate(Booking booking, Person person) {
        LearningSession session = booking.getLearningSession();

        return getEmailHeader() +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "<h2 style='color: #ffa500; margin-top: 0; font-size: 24px;'>Has salido de la lista de espera</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Hola <strong style='color: #aae16b;'>" + person.getFullName() + "</strong>," +
                "</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Has salido de la lista de espera de:" +
                "</p>" +

                "<div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #ffa500;'>" +
                "<h3 style='color: #ffffff; margin-top: 0; font-size: 18px;'>" + session.getTitle() + "</h3>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 5px 0;'> " + formatDate(session.getScheduledDatetime()) + "</p>" +
                "</div>" +

                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Puedes explorar otras sesiones disponibles.</p>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "<tr><td align='center'>" +
                "<a href='" + frontendUrl + "/app/sessions' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold; border: 2px solid #aae16b;'>Explorar Sesiones</a>" +
                "</td></tr></table>" +
                "</td></tr>" + getEmailFooter();
    }

    // ==================== INSTRUCTOR NOTIFICATION ====================

    private String buildInstructorNotificationTemplate(LearningSession session, Person instructorPerson,
                                                       String learnerName, boolean isGroup, int spotsFreed) {
        long confirmedBookings = session.getCurrentBookings() - spotsFreed;
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;
        String cancellationType = isGroup ? "grupal" : "individual";

        return getEmailHeader() +
                "<tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "<h2 style='color: #ffa500; margin-top: 0; font-size: 24px;'> Cancelación de Registro</h2>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "Hola <strong style='color: #aae16b;'>" + instructorPerson.getFullName() + "</strong>," +
                "</p>" +
                "<p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "<strong style='color: #aae16b;'>" + learnerName + "</strong> ha cancelado su registro " + cancellationType + " en:" +
                "</p>" +

                "<div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #ffa500;'>" +
                "<h3 style='color: #aae16b; margin-top: 0; font-size: 18px;'>" + session.getTitle() + "</h3>" +
                "<p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'> " + formatDate(session.getScheduledDatetime()) + " - " + formatTime(session.getScheduledDatetime()) + "</p>" +
                "</div>" +

                "<div style='background-color: #2a2a2a; padding: 20px; border-radius: 8px; margin: 25px 0;'>" +
                "<h4 style='color: #aae16b; margin: 0 0 15px 0; font-size: 16px;'>Estado de la sesión:</h4>" +
                "<table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px;'>" +
                "<tr><td style='color: #b0b0b0;'>Cupos liberados:</td><td style='color: #ffffff; text-align: right;'>" + spotsFreed + "</td></tr>" +
                "<tr><td style='color: #b0b0b0;'>Cupos disponibles:</td><td style='color: #aae16b; text-align: right; font-weight: bold;'>" + availableSpots + "/" + session.getMaxCapacity() + "</td></tr>" +
                "</table></div>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "<tr><td align='center'>" +
                "<a href='" + frontendUrl + "/app/instructor/sessions' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold; border: 2px solid #aae16b;'>Ver Mis Sesiones</a>" +
                "</td></tr></table>" +
                "</td></tr>" + getEmailFooter();
    }
}