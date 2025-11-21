package com.project.skillswap.logic.entity.LearningSession;

import com.project.skillswap.logic.entity.Notification.Notification;
import com.project.skillswap.logic.entity.Notification.NotificationRepository;
import com.project.skillswap.logic.entity.Notification.NotificationType;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Servicio para el envío de correos electrónicos relacionados con sesiones de aprendizaje.
 * COMBINA: SessionEmailService + SessionEmailServiceReminder
 *
 * Características:
 * ✅ Google Calendar Link
 * ✅ Botón Editar Sesión
 * ✅ Video Call Link
 * ✅ Notificaciones en BD
 * ✅ Detalles completos de la sesión
 * ✅ Consejos para el instructor
 */
@Service
public class SessionEmailService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Value("${mail.from:${spring.mail.username}}")
    private String from;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Autowired
    public SessionEmailService(
            JavaMailSender mailSender,
            NotificationRepository notificationRepository) {
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Envía un correo de confirmación de creación de sesión al instructor.
     * INCLUYE: Google Calendar, Video Call Link, Botón Editar, Notificaciones
     */
    public boolean sendSessionCreationEmail(LearningSession session, Person instructor) {
        try {
            System.out.println(" [SessionEmailService] 📧 Iniciando envío para: " + instructor.getEmail());

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setFrom(from);
            helper.setTo(instructor.getEmail());
            helper.setSubject(buildSubject(session));

            String htmlContent = buildSessionCreationTemplate(session, instructor);
            helper.setText(htmlContent, true);

            mailSender.send(msg);

            registerSuccessfulNotification(instructor, session, "SESSION_CREATED");
            System.out.println(" [SessionEmailService] ✅ Email de confirmación enviado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println(" [SessionEmailService] ❌ Error enviando email: " + e.getMessage());
            e.printStackTrace();
            registerFailedNotification(instructor, "SESSION_CREATED", "Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Construye el asunto del email
     */
    private String buildSubject(LearningSession session) {
        return "✅ Tu sesión '" + session.getTitle() + "' ha sido publicada exitosamente";
    }

    /**
     * Construye el template HTML para el correo de creación de sesión.
     * COMBINA lo mejor de ambas versiones
     */
    private String buildSessionCreationTemplate(LearningSession session, Person instructor) {
        String formattedDate = formatDate(session.getScheduledDatetime());
        String formattedTime = formatTime(session.getScheduledDatetime());
        String sessionLink = frontendUrl + "/app/sessions/" + session.getId();
        String editLink = frontendUrl + "/app/sessions/" + session.getId() + "/edit";
        String googleCalendarLink = buildGoogleCalendarLink(session);
        String categoryName = session.getSkill().getKnowledgeArea() != null ?
                session.getSkill().getKnowledgeArea().getName() : "N/A";

        return """
<!DOCTYPE html>
<html lang='es'>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>Sesión Publicada</title>
</head>
<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>
    <table width='100%%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>
        <tr>
            <td align='center'>
                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>
                    
                    <!-- Header con Gradiente -->\
                    <tr>
                        <td style='background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); padding: 40px 20px; text-align: center;'>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>✅ SkillSwap</h1>
                            <p style='color: rgba(255, 255, 255, 0.8); margin: 8px 0 0 0; font-size: 14px;'>Sesión Publicada Exitosamente</p>
                        </td>
                    </tr>
                    
                    <!-- Contenido Principal -->
                    <tr>
                        <td style='padding: 40px 30px; color: #ffffff;'>
                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola %s!</h2>
                            
                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>
                                Tu sesión <strong style='color: #aae16b;'>"%s"</strong> ha sido <strong style='color: #aae16b;'>publicada exitosamente</strong> y está disponible para que los estudiantes se inscriban.
                            </p>

                            <!-- Detalles de la Sesión -->
                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>
                                <h3 style='color: #aae16b; margin-top: 0; margin-bottom: 15px; font-size: 20px;'>%s</h3>
                                <p style='font-size: 13px; color: #b0b0b0; margin: 0 0 15px 0;'>%s</p>
                                
                                <hr style='border: none; border-top: 1px solid #504ab7; margin: 15px 0;'>
                                
                                <table width='100%%' cellpadding='8' cellspacing='0' style='font-size: 14px;'>
                                    <tr>
                                        <td style='color: #aae16b; width: 40%%;'><strong>📅 Fecha:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>⏰ Hora:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>⏱️ Duración:</strong></td>
                                        <td style='color: #ffffff;'>%d minutos</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>🎯 Habilidad:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>📂 Categoría:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>🌐 Idioma:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>👥 Capacidad:</strong></td>
                                        <td style='color: #ffffff;'>%d participantes</td>
                                    </tr>
                                </table>
                            </div>

                            <!-- Info de Inscripciones -->
                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #504ab7;'>
                                <h4 style='color: #aae16b; margin-top: 0; margin-bottom: 10px; font-size: 16px;'>📊 Inscripciones</h4>
                                <p style='margin: 0; font-size: 14px; color: #ffffff;'>
                                    Los estudiantes ya pueden ver tu sesión y registrarse. Recibirás notificaciones de nuevas inscripciones.
                                </p>
                            </div>

                            <!-- Google Calendar Integration -->
                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #4285F4;'>
                                <h4 style='color: #4285F4; margin-top: 0; margin-bottom: 10px; font-size: 16px;'>📅 Agregar a Google Calendar</h4>
                                <p style='margin: 0 0 15px 0; font-size: 14px; color: #ffffff;'>
                                    Haz clic en el botón para agregar esta sesión automáticamente a tu calendario de Google.
                                </p>
                                <a href='%s' style='display: inline-block; background-color: #4285F4; color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 5px; font-size: 14px; font-weight: bold;'>Agregar a Google Calendar</a>
                            </div>

                            <!-- Consejos para la sesión -->
                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #aae16b;'>
                                <h4 style='color: #aae16b; margin-top: 0; margin-bottom: 10px; font-size: 16px;'>✅ Consejos para tu sesión:</h4>
                                <ul style='color: #ffffff; font-size: 13px; line-height: 1.8; padding-left: 20px; margin: 0;'>
                                    <li>Prepara tu material con anticipación</li>
                                    <li>Verifica tu conexión y equipo antes de comenzar</li>
                                    <li>Revisa la lista de inscritos en tu panel</li>
                                    <li>Recuerda llegar 5 minutos antes</li>
                                </ul>
                            </div>

                            <!-- Botones de Acción -->
                            <table width='100%%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>
                                <tr>
                                    <td align='center'>
                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold; margin-right: 10px;'>Ver Sesión</a>
                                        <a href='%s' style='display: inline-block; background-color: #39434b; color: #aae16b; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold; border: 2px solid #aae16b; margin-right: 10px;'>Editar Sesión</a>
                                        <a href='%s' style='display: inline-block; background-color: #4CAF50; color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Unirse a Videollamada</a>
                                    </td>
                                </tr>
                            </table>

                            <p style='font-size: 13px; color: #b0b0b0; margin-top: 30px; line-height: 1.6;'>
                                Si tienes preguntas, no dudes en contactar con nuestro equipo de soporte.
                            </p>
                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>
                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>
                                © 2025 SkillSwap. Todos los derechos reservados.
                            </p>
                            <p style='margin: 10px 0 0 0; font-size: 11px; color: #888888;'>
                                Este es un correo automático, por favor no responder.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
""".formatted(
                instructor.getFullName(),
                session.getTitle(),
                session.getTitle(),
                session.getDescription(),
                formattedDate,
                formattedTime,
                session.getDurationMinutes(),
                session.getSkill().getName(),
                categoryName,
                getLanguageName(session.getLanguage()),
                session.getMaxCapacity(),
                googleCalendarLink,
                sessionLink,
                editLink,
                session.getVideoCallLink() != null ? session.getVideoCallLink() : frontendUrl + "/app/sessions/" + session.getId()
        );
    }

    /**
     * Construye el link de Google Calendar
     */
    private String buildGoogleCalendarLink(LearningSession session) {
        try {
            String title = java.net.URLEncoder.encode(session.getTitle(), "UTF-8");
            String description = java.net.URLEncoder.encode(
                    "Habilidad: " + session.getSkill().getName() + "\n" +
                            "Duración: " + session.getDurationMinutes() + " minutos\n" +
                            "Descripción: " + session.getDescription(),
                    "UTF-8"
            );
            String location = java.net.URLEncoder.encode("SkillSwap - Online", "UTF-8");

            String startDateTime = formatDateForGoogle(session.getScheduledDatetime());
            String endDateTime = formatEndDateForGoogle(session.getScheduledDatetime(), session.getDurationMinutes());

            return "https://www.google.com/calendar/render?action=TEMPLATE" +
                    "&text=" + title +
                    "&dates=" + startDateTime + "/" + endDateTime +
                    "&details=" + description +
                    "&location=" + location +
                    "&trp=true";
        } catch (Exception e) {
            System.err.println("[SessionEmailService] ❌ Error construyendo Google Calendar link: " + e.getMessage());
            return "#";
        }
    }

    /**
     * Formatea fecha para Google Calendar (yyyyMMddTHHmmssZ)
     */
    private String formatDateForGoogle(java.util.Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /**
     * Formatea fecha final para Google Calendar (agregando duración)
     */
    private String formatEndDateForGoogle(java.util.Date date, Integer durationMinutes) {
        long endTime = date.getTime() + (durationMinutes * 60 * 1000);
        java.util.Date endDate = new java.util.Date(endTime);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(endDate);
    }

    /**
     * Registra notificación exitosa en BD
     */
    private void registerSuccessfulNotification(Person person, LearningSession session, String eventType) {
        try {
            Notification notification = new Notification();
            notification.setPerson(person);
            notification.setType(NotificationType.EMAIL);
            notification.setTitle("✅ " + eventType + " - " + session.getTitle());
            notification.setMessage("Email enviado exitosamente para la sesión #" + session.getId());
            notification.setRead(false);
            notificationRepository.save(notification);
            System.out.println(" [SessionEmailService] 📢 Notificación registrada en BD");
        } catch (Exception e) {
            System.err.println(" [SessionEmailService] Error registrando notificación: " + e.getMessage());
        }
    }

    /**
     * Registra notificación fallida en BD
     */
    private void registerFailedNotification(Person person, String eventType, String reason) {
        try {
            Notification notification = new Notification();
            notification.setPerson(person);
            notification.setType(NotificationType.EMAIL);
            notification.setTitle("❌ Email no enviado - " + eventType);
            notification.setMessage("Razón: " + reason);
            notification.setRead(false);
            notificationRepository.save(notification);
            System.out.println(" [SessionEmailService] 📢 Notificación de error registrada en BD");
        } catch (Exception e) {
            System.err.println(" [SessionEmailService] Error registrando notificación fallida: " + e.getMessage());
        }
    }

    /**
     * Formatea solo la fecha (EEEE, dd 'de' MMMM 'de' yyyy)
     */
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        return sdf.format(date);
    }

    /**
     * Formatea solo la hora (HH:mm)
     */
    private String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", new Locale("es", "ES"));
        return sdf.format(date);
    }

    /**
     * Convierte código de idioma a nombre legible
     */
    private String getLanguageName(String code) {
        return switch (code) {
            case "es" -> "Español";
            case "en" -> "English";
            case "pt" -> "Português";
            case "fr" -> "Français";
            case "de" -> "Deutsch";
            default -> code;
        };
    }
}