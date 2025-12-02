package com.project.skillswap.logic.entity.LearningSession;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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

@Service
public class SessionEmailService {
    private static final Logger logger = LoggerFactory.getLogger(SessionEmailService.class);

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

    public boolean sendSessionCreationEmail(LearningSession session, Person instructor) {
        try {
            logger.info(" [SessionEmailService] Iniciando envío para: " + instructor.getEmail());

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setFrom(from);
            helper.setTo(instructor.getEmail());
            helper.setSubject("Sesión Publicada - " + session.getTitle());

            String htmlContent = buildSessionCreationTemplate(session, instructor);
            helper.setText(htmlContent, true);

            mailSender.send(msg);

            registerSuccessfulNotification(instructor, session, "SESSION_CREATED");
            logger.info(" [SessionEmailService] Email enviado exitosamente");
            return true;

        } catch (Exception e) {
            logger.info(" [SessionEmailService] Error: " + e.getMessage());
            e.printStackTrace();
            registerFailedNotification(instructor, "SESSION_CREATED", "Error: " + e.getMessage());
            return false;
        }
    }

    public boolean sendTranscriptionReadyEmail(LearningSession session, Person instructor) {
        try {
            logger.info("========================================");
            logger.info(" ENVIANDO EMAIL DE TRANSCRIPCIÓN");
            logger.info("   Sesión: " + session.getTitle());
            logger.info("   Instructor: " + instructor.getEmail());
            logger.info("========================================");

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                logger.info(" No hay texto de transcripción para enviar");
                return false;
            }

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setFrom(from);
            helper.setTo(instructor.getEmail());
            helper.setSubject("Transcripción Disponible - " + session.getTitle());

            String htmlContent = buildTranscriptionReadyTemplate(session, instructor);
            helper.setText(htmlContent, true);

            mailSender.send(msg);

            registerSuccessfulNotification(instructor, session, "TRANSCRIPTION_READY");

            logger.info("========================================");
            logger.info(" EMAIL DE TRANSCRIPCIÓN ENVIADO");
            logger.info("========================================");

            return true;

        } catch (Exception e) {
            logger.info("========================================");
            logger.info(" ERROR ENVIANDO EMAIL DE TRANSCRIPCIÓN");
            logger.info("   Error: " + e.getMessage());
            logger.info("========================================");
            e.printStackTrace();
            registerFailedNotification(instructor, "TRANSCRIPTION_READY", "Error: " + e.getMessage());
            return false;
        }
    }

    private String buildSessionCreationTemplate(LearningSession session, Person instructor) {
        String formattedDate = formatDate(session.getScheduledDatetime());
        String formattedTime = formatTime(session.getScheduledDatetime());
        String sessionLink = frontendUrl + "/app/sessions/" + session.getId();
        String googleCalendarLink = buildGoogleCalendarLink(session);

        return """
<! DOCTYPE html>
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
                    <tr>
                        <td style='background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); padding: 40px 20px; text-align: center;'>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>
                        </td>
                    </tr>
                    <tr>
                        <td style='padding: 40px 30px; color: #ffffff;'>
                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>Sesión Publicada Exitosamente</h2>
                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>
                                Hola <strong style='color: #aae16b;'>%s</strong>,
                            </p>
                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>
                                Tu sesión ha sido publicada correctamente y ya está disponible para que los estudiantes se inscriban.
                            </p>
                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>
                                <h3 style='color: #aae16b; margin-top: 0; font-size: 20px;'>%s</h3>
                                <p style='font-size: 14px; color: #b0b0b0; margin: 10px 0;'>%s</p>
                                <hr style='border: none; border-top: 1px solid #504ab7; margin: 20px 0;'>
                                <table width='100%%' cellpadding='5' cellspacing='0' style='font-size: 14px;'>
                                    <tr>
                                        <td style='color: #aae16b; width: 40%%;'><strong>ID de Sesión:</strong></td>
                                        <td style='color: #ffffff;'>#%d</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>Fecha:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>Hora:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>Duración:</strong></td>
                                        <td style='color: #ffffff;'>%d minutos</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>Habilidad:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>Categoría:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>Idioma:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong>Capacidad:</strong></td>
                                        <td style='color: #ffffff;'>%d participantes</td>
                                    </tr>
                                </table>
                            </div>
                            <table width='100%%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>
                                <tr>
                                    <td align='center'>
                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%%, #8ec756 100%%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4); margin: 0 5px;'>Ver Sesión</a>
                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%%, #6b63d8 100%%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(80, 74, 183, 0.4); margin: 0 5px;'>Agregar a Google Calendar</a>
                                    </td>
                                </tr>
                            </table>
                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #504ab7;'>
                                <h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'>Consejos para tu sesión:</h4>
                                <ul style='color: #ffffff; font-size: 14px; line-height: 1.8; padding-left: 20px; margin: 10px 0;'>
                                    <li>Prepara tu material con anticipación</li>
                                    <li>Verifica tu conexión y equipo antes de comenzar</li>
                                    <li>Revisa la lista de inscritos en tu panel</li>
                                    <li>Recuerda llegar 5 minutos antes</li>
                                </ul>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>
                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>
                                © 2025 SkillSwap. Todos los derechos reservados.
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
                session.getDescription(),
                session.getId(),
                formattedDate,
                formattedTime,
                session.getDurationMinutes(),
                session.getSkill().getName(),
                session.getSkill().getKnowledgeArea().getName(),
                getLanguageName(session.getLanguage()),
                session.getMaxCapacity(),
                sessionLink,
                googleCalendarLink
        );
    }
    private String buildTranscriptionReadyTemplate(LearningSession session, Person instructor) {
        String downloadTxtLink = "http://localhost:8080/videocall/transcription/" + session.getId() + "/download-txt";
        String downloadPdfLink = "http://localhost:8080/videocall/transcription/" + session.getId() + "/download-pdf";

        int wordCount = session.getFullText() != null ? session.getFullText().split("\\s+").length : 0;
        int charCount = session.getFullText() != null ? session.getFullText().length() : 0;
        int durationMinutes = session.getDurationSeconds() != null ? session.getDurationSeconds() / 60 : 0;

        String preview = "";
        if (session.getFullText() != null) {
            preview = session.getFullText().length() > 300
                    ? session.getFullText().substring(0, 300) + "..."
                    : session.getFullText();
        }

        return """
<! DOCTYPE html>
<html lang='es'>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>Transcripción Disponible</title>
</head>
<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>
    <table width='100%%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>
        <tr>
            <td align='center'>
                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>
                    <tr>
                        <td style='background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); padding: 40px 20px; text-align: center;'>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>
                        </td>
                    </tr>
                    <tr>
                        <td style='padding: 40px 30px; color: #ffffff;'>
                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px; text-align: center;'>Transcripción Disponible</h2>
                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0; text-align: center;'>
                                Estimado/a <strong style='color: #aae16b;'>%s</strong>
                            </p>
                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0; text-align: center;'>
                                La transcripción automática de su sesión ha sido procesada exitosamente<br>y está disponible para su revisión y descarga.
                            </p>
                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>
                                <h3 style='color: #aae16b; margin-top: 0; font-size: 20px; text-align: center;'>%s</h3>
                                <table width='100%%' cellpadding='8' cellspacing='0' style='font-size: 14px; margin-top: 15px;'>
                                    <tr>
                                        <td style='color: #aae16b; width: 40%%; font-weight: 600;'>Estadísticas:</td>
                                        <td style='color: #ffffff;'></td>
                                    </tr>
                                    <tr>
                                        <td style='color: #b0b0b0; padding-left: 20px;'>Duración:</td>
                                        <td style='color: #ffffff; text-align: right;'>%d minutos</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #b0b0b0; padding-left: 20px;'>Palabras:</td>
                                        <td style='color: #ffffff; text-align: right;'>%,d palabras</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #b0b0b0; padding-left: 20px;'>Caracteres:</td>
                                        <td style='color: #ffffff; text-align: right;'>%,d caracteres</td>
                                    </tr>
                                </table>
                            </div>
                            <div style='background-color: #1e1e1e; padding: 20px; border-radius: 8px; margin: 25px 0; border: 1px solid #504ab7;'>
                                <h4 style='color: #aae16b; margin-top: 0; font-size: 14px; font-weight: 600;'>Vista Previa del Contenido:</h4>
                                <p style='color: #b0b0b0; font-size: 13px; line-height: 1.6; font-style: italic; margin: 10px 0;'>
                                    "%s"
                                </p>
                            </div>
                            <table width='100%%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>
                                <tr>
                                    <td align='center'>
                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%%, #8ec756 100%%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4); margin: 0 5px;'>Descargar TXT</a>
                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%%, #6b63d8 100%%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(80, 74, 183, 0.4); margin: 0 5px;'>Descargar PDF</a>
                                    </td>
                                </tr>
                            </table>
                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #504ab7;'>
                                <h4 style='color: #aae16b; margin-top: 0; font-size: 16px; font-weight: 600;'>Usos Recomendados</h4>
                                <ul style='color: #ffffff; font-size: 14px; line-height: 1.8; padding-left: 20px; margin: 10px 0;'>
                                    <li>Revisar y analizar los puntos clave de la sesión</li>
                                    <li>Compartir contenido con estudiantes participantes</li>
                                    <li>Crear material de estudio y documentación</li>
                                    <li>Evaluar y mejorar futuras presentaciones</li>
                                </ul>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>
                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>
                                © 2025 SkillSwap. Todos los derechos reservados.
                            </p>
                            <p style='margin: 10px 0 0 0; font-size: 11px; color: #888;'>
                                Transcripción procesada con inteligencia artificial | Groq Whisper Large V3
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
                durationMinutes,
                wordCount,
                charCount,
                preview != null ? preview : "",
                downloadTxtLink,
                downloadPdfLink
        );
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
    private void registerSuccessfulNotification(Person person, LearningSession session, String eventType) {
        try {
            Notification notification = new Notification();
            notification.setPerson(person);
            notification.setType(NotificationType.EMAIL);
            notification.setTitle(eventType + " - " + session.getTitle());
            notification.setMessage("Email enviado exitosamente para la sesión #" + session.getId());
            notification.setRead(false);
            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.info(" Error registrando notificación: " + e.getMessage());
        }
    }

    private void registerFailedNotification(Person person, String eventType, String reason) {
        try {
            Notification notification = new Notification();
            notification.setPerson(person);
            notification.setType(NotificationType.EMAIL);
            notification.setTitle("Email no enviado - " + eventType);
            notification.setMessage("Razón: " + reason);
            notification.setRead(false);
            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.info(" Error registrando notificación fallida: " + e.getMessage());
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        return sdf.format(date);
    }

    private String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", new Locale("es", "ES"));
        return sdf.format(date);
    }

    private String getLanguageName(String code) {
        return switch (code) {
            case "es" -> "Español";
            case "en" -> "English";
            default -> code;
        };
    }
}