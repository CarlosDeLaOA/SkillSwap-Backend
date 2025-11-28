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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Servicio para el envío de correos electrónicos relacionados con sesiones de aprendizaje.
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
     */
    public boolean sendSessionCreationEmail(LearningSession session, Person instructor) {
        try {
            System.out.println("[SessionEmailService] Iniciando envío para: " + instructor.getEmail());

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setFrom(from);
            helper.setTo(instructor.getEmail());
            helper.setSubject(" Sesión Publicada - " + session.getTitle());

            String htmlContent = buildSessionCreationTemplate(session, instructor);
            helper.setText(htmlContent, true);

            mailSender.send(msg);

            registerSuccessfulNotification(instructor, session, "SESSION_CREATED");
            System.out.println("[SessionEmailService] Email enviado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("[SessionEmailService] Error: " + e.getMessage());
            e.printStackTrace();
            registerFailedNotification(instructor, "SESSION_CREATED", "Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envía email cuando la transcripción está lista
     */
    public boolean sendTranscriptionReadyEmail(LearningSession session, Person instructor) {
        try {
            System.out.println("========================================");
            System.out.println(" ENVIANDO EMAIL DE TRANSCRIPCIÓN");
            System.out.println("   Sesión: " + session.getTitle());
            System.out.println("   Instructor: " + instructor.getEmail());
            System.out.println("========================================");

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                System.err.println(" No hay texto de transcripción para enviar");
                return false;
            }

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setFrom(from);
            helper.setTo(instructor.getEmail());
            helper.setSubject(" Transcripción Disponible - " + session.getTitle());

            String htmlContent = buildTranscriptionReadyTemplate(session, instructor);
            helper.setText(htmlContent, true);

            mailSender.send(msg);

            registerSuccessfulNotification(instructor, session, "TRANSCRIPTION_READY");

            System.out.println("========================================");
            System.out.println(" EMAIL DE TRANSCRIPCIÓN ENVIADO");
            System.out.println("========================================");

            return true;

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println(" ERROR ENVIANDO EMAIL DE TRANSCRIPCIÓN");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            registerFailedNotification(instructor, "TRANSCRIPTION_READY", "Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Construye el template HTML para el correo de creación de sesión.
     */
    private String buildSessionCreationTemplate(LearningSession session, Person instructor) {
        String formattedDate = formatDate(session.getScheduledDatetime());
        String formattedTime = formatTime(session.getScheduledDatetime());
        String editLink = frontendUrl + "/app/sessions/" + session.getId() + "/edit";
        String sessionLink = frontendUrl + "/app/sessions/" + session.getId();

        String videoCallLink = session.getVideoCallLink() != null && !session.getVideoCallLink().isEmpty()
                ? session.getVideoCallLink()
                : frontendUrl + "/app/video-call/" + session.getId();

        String googleCalendarLink = buildGoogleCalendarLink(session);

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
                    <tr>
                        <td style='background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); padding: 40px 20px; text-align: center;'>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'> SkillSwap</h1>
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
                                        <td style='color: #aae16b; width: 40%%;'><strong> Fecha:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong> Hora:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong> Duración:</strong></td>
                                        <td style='color: #ffffff;'>%d minutos</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong> Habilidad:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong> Categoría:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong> Idioma:</strong></td>
                                        <td style='color: #ffffff;'>%s</td>
                                    </tr>
                                    <tr>
                                        <td style='color: #aae16b;'><strong> Capacidad:</strong></td>
                                        <td style='color: #ffffff;'>%d participantes</td>
                                    </tr>
                                </table>
                            </div>

                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #4285F4;'>
                                <h4 style='color: #4285F4; margin-top: 0; margin-bottom: 10px; font-size: 16px;'> Agregar a Google Calendar</h4>
                                <p style='margin: 0 0 15px 0; font-size: 14px; color: #ffffff;'>
                                    Haz clic para agregar esta sesión a tu calendario de Google.
                                </p>
                                <a href='%s' style='display: inline-block; background-color: #4285F4; color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 5px; font-size: 14px; font-weight: bold;'>Agregar a Google Calendar</a>
                            </div>

                            <table width='100%%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>
                                <tr>
                                    <td align='center'>
                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold; margin-right: 10px;'>Ver Sesión</a>
                                        <a href='%s' style='display: inline-block; background-color: #39434b; color: #aae16b; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold; border: 2px solid #aae16b;'> Unirse a Videollamada</a>
                                    </td>
                                </tr>
                            </table>

                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #504ab7;'>
                                <h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'> Consejos para tu sesión:</h4>
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
                formattedDate,
                formattedTime,
                session.getDurationMinutes(),
                session.getSkill().getName(),
                session.getSkill().getKnowledgeArea().getName(),
                getLanguageName(session.getLanguage()),
                session.getMaxCapacity(),
                googleCalendarLink,
                sessionLink,
                videoCallLink
        );
    }

    /**
     *  Construye el link de Google Calendar
     */
    private String buildGoogleCalendarLink(LearningSession session) {
        try {
            String title = URLEncoder.encode(session.getTitle(), StandardCharsets.UTF_8.toString());
            String description = URLEncoder.encode(
                    session.getDescription() != null ? session.getDescription() : "",
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
                    "https://calendar.google.com/calendar/render?action=TEMPLATE&text=%s&dates=%s/%s&details=%s",
                    title, start, end, description
            );
        } catch (Exception e) {
            System.err.println("Error construyendo Google Calendar link: " + e.getMessage());
            return "https://calendar.google.com";
        }
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
<!DOCTYPE html>
<html lang='es'>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>Transcripción Disponible</title>
</head>
<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background-color: #39434b;'>
    <table width='100%%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>
        <tr>
            <td align='center'>
                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>
                    
                    <tr>
                        <td style='background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%); padding: 40px 20px; text-align: center;'>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'> SkillSwap</h1>
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
                                <h3 style='color: #aae16b; margin-top: 0; font-size: 18px; text-align: center;'>%s</h3>
                                
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
                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #aae16b 0%%, #8ec756 100%%); color: #141414; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4); margin: 0 5px;'>
                                             Descargar TXT
                                        </a>
                                        
                                        <a href='%s' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%%, #6b63d8 100%%); color: #ffffff; text-decoration: none; padding: 15px 35px; border-radius: 25px; font-size: 15px; font-weight: bold; box-shadow: 0 4px 15px rgba(80, 74, 183, 0.4); margin: 0 5px;'>
                                             Descargar PDF
                                        </a>
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

    private void registerSuccessfulNotification(Person person, LearningSession session, String eventType) {
        try {
            Notification notification = new Notification();
            notification.setPerson(person);
            notification.setType(NotificationType.EMAIL);
            notification.setTitle( eventType + " - " + session.getTitle());
            notification.setMessage("Email enviado exitosamente para la sesión #" + session.getId());
            notification.setRead(false);
            notificationRepository.save(notification);
        } catch (Exception e) {
            System.err.println(" Error registrando notificación: " + e.getMessage());
        }
    }

    private void registerFailedNotification(Person person, String eventType, String reason) {
        try {
            Notification notification = new Notification();
            notification.setPerson(person);
            notification.setType(NotificationType.EMAIL);
            notification.setTitle(" Email no enviado - " + eventType);
            notification.setMessage("Razón: " + reason);
            notification.setRead(false);
            notificationRepository.save(notification);
        } catch (Exception e) {
            System.err.println(" Error registrando notificación fallida: " + e.getMessage());
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
