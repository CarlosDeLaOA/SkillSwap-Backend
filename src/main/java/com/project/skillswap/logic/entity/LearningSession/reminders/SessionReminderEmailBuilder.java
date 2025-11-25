package com.project.skillswap.logic.entity.LearningSession.reminders;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Constructor de emails de recordatorio para sesiones.
 * Genera HTML para emails de recordatorio 24 horas antes e incluye
 * un botón "Añadir a Google Calendar" apuntando a la UI de Google Calendar.
 */
@Component
public class SessionReminderEmailBuilder {

    /**
     * Construye el asunto del email de recordatorio
     *
     * @param session Sesión
     * @return Asunto del email
     */
    public String buildSubject(LearningSession session) {
        return "Recordatorio: Tu sesión '" + session.getTitle() + "' comienza mañana";
    }

    /**
     * Construye el contenido HTML del email de recordatorio
     *
     * @param session Sesión
     * @return HTML formateado
     */
    public String buildReminderEmail(LearningSession session) {
        String formattedDate = formatDateTime(session.getScheduledDatetime());
        String instructorName = session.getInstructor().getPerson().getFullName();
        String skillName = session.getSkill() != null ? session.getSkill().getName() : "";
        String sessionLink = "https://app.skillswap.com/sessions/" + session.getId();

        // Enlace "Añadir a Google Calendar"
        String googleCalendarUrl = buildGoogleCalendarUrl(
                session.getTitle(),
                session.getDescription(),
                session.getScheduledDatetime(),
                calculateEndTime(session.getScheduledDatetime(), session.getDurationMinutes()),
                session.getVideoCallLink() != null ? session.getVideoCallLink() : sessionLink
        );

        return """
<!DOCTYPE html>
<html lang='es'>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>Recordatorio de Sesión</title>
</head>
<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>
    <table width='100%%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>
        <tr>
            <td align='center'>
                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>
                    <tr>
                        <td style='background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); padding: 40px 20px; text-align: center;'>
                            <h1 style='color: #ffffff; margin: 0; font-size: 24px; font-weight: bold;'>SkillSwap</h1>
                            <p style='color: rgba(255,255,255,0.8); margin: 8px 0 0 0; font-size: 13px;'>Recordatorio de Sesión</p>
                        </td>
                    </tr>
                    <tr>
                        <td style='padding: 30px 30px; color: #ffffff;'>
                            <h2 style='color: #aae16b; margin-top: 0; font-size: 20px;'>¡Hola %s!</h2>
                            <p style='font-size: 14px; line-height: 1.6; color: #ffffff; margin: 12px 0;'>Este es un recordatorio de que tu sesión <strong style='color: #aae16b;'>comienza mañana</strong>.</p>
                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #aae16b;'>
                                <h3 style='color: #aae16b; margin-top: 0; margin-bottom: 10px; font-size: 16px;'>%s</h3>
                                <table width='100%%' cellpadding='6' cellspacing='0' style='font-size: 13px;'>
                                    <tr>
                                        <td style='color: #aae16b; width: 35%%; vertical-align: top;'><strong>Fecha y Hora:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b; vertical-align: top;'><strong>Duración:</strong></td>
                                        <td style='color: #ffffff;'>%d minutos</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b; vertical-align: top;'><strong>Habilidad:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b; vertical-align: top;'><strong>Capacidad:</strong></td>
                                        <td style='color: #ffffff;'>%d participantes</td>
                                    </tr>
                                </table>
                            </div>
                            <div style='margin: 18px 0;'>
                                <table width='100%%' cellpadding='0' cellspacing='0'>
                                    <tr>
                                        <td align='center' style='padding: 0;'>
                                            <table cellpadding='0' cellspacing='8' style='display: inline-block;'>
                                                <tr>
                                                    <td align='center'>
                                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 5px; font-size: 13px; font-weight: bold; white-space: nowrap;' target='_blank' rel='noopener noreferrer'>Añadir a Google Calendar</a>
                                                    </td>
                                                    <td align='center'>
                                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 5px; font-size: 13px; font-weight: bold; white-space: nowrap;' target='_blank' rel='noopener noreferrer'>Ver Sesión</a>
                                                    </td>
                                                    <td align='center'>
                                                        <a href='%s' style='display: inline-block; background-color: #39434b; color: #aae16b; text-decoration: none; padding: 12px 24px; border-radius: 5px; font-size: 13px; font-weight: bold; border: 2px solid #aae16b; white-space: nowrap;' target='_blank' rel='noopener noreferrer'>Unirse a Videollamada</a>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                            <p style='font-size: 12px; color: #b0b0b0; margin-top: 12px;'>Si tienes preguntas o necesitas ayuda, contacta con soporte.</p>
                        </td>
                    </tr>
                    <tr>
                        <td style='background-color: #39434b; padding: 16px 20px; text-align: center;'>
                            <p style='margin: 0; font-size: 11px; color: #b0b0b0;'>© 2025 SkillSwap. Todos los derechos reservados.</p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
""".formatted(
                instructorName,
                session.getTitle(),
                formattedDate,
                session.getDurationMinutes(),
                skillName,
                session.getMaxCapacity(),
                googleCalendarUrl,
                sessionLink,
                session.getVideoCallLink() == null ? "" : session.getVideoCallLink()
        );
    }

    private Date calculateEndTime(Date start, Integer durationMinutes) {
        if (start == null || durationMinutes == null) return null;
        return new Date(start.getTime() + (durationMinutes * 60L * 1000L));
    }

    private String buildGoogleCalendarUrl(String title, String description, Date start, Date end, String location) {
        try {
            String startUtc = formatDateForGoogle(start);
            String endUtc = formatDateForGoogle(end);

            StringBuilder url = new StringBuilder("https://www.google.com/calendar/render?action=TEMPLATE");
            if (title != null && !title.isEmpty()) url.append("&text=").append(URLEncoder.encode(title, StandardCharsets.UTF_8.toString()));
            if (description != null && !description.isEmpty()) url.append("&details=").append(URLEncoder.encode(description, StandardCharsets.UTF_8.toString()));
            if (startUtc != null && endUtc != null) url.append("&dates=").append(URLEncoder.encode(startUtc + "/" + endUtc, StandardCharsets.UTF_8.toString()));
            if (location != null && !location.isEmpty()) url.append("&location=").append(URLEncoder.encode(location, StandardCharsets.UTF_8.toString()));
            url.append("&trp=true");
            return url.toString();
        } catch (Exception e) {
            return "https://www.google.com/calendar";
        }
    }

    private String formatDateForGoogle(Date date) {
        if (date == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private String formatDateTime(Date dateTime) {
        if (dateTime == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy 'a las' HH:mm", new Locale("es", "ES"));
        return sdf.format(dateTime);
    }
}