package com.project.skillswap.logic.entity.Booking;

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

/**
 * Servicio para el envío de correos electrónicos relacionados con bookings.
 */
@Service
public class BookingEmailService {

    //#region Dependencies
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public BookingEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    //#endregion

    //#region Public Methods
    /**
     * Envía un correo de confirmación de booking.
     *
     * @param booking el booking creado
     * @param person la persona que hizo el booking
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendBookingConfirmationEmail(Booking booking, Person person) throws MessagingException {
        String subject = "Confirmación de registro - " + booking.getLearningSession().getTitle();
        String htmlContent = buildBookingConfirmationTemplate(booking, person);

        sendHtmlEmail(person.getEmail(), subject, htmlContent);
    }

    /**
     * Envía un correo de cancelación de booking.
     *
     * @param booking el booking cancelado
     * @param person la persona que canceló
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendBookingCancellationEmail(Booking booking, Person person) throws MessagingException {
        String subject = "Cancelación de registro - " + booking.getLearningSession().getTitle();
        String htmlContent = buildBookingCancellationTemplate(booking, person);

        sendHtmlEmail(person.getEmail(), subject, htmlContent);
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
     * Construye el template HTML para el correo de confirmación de booking.
     */
    private String buildBookingConfirmationTemplate(Booking booking, Person person) {
        LearningSession session = booking.getLearningSession();

        String sessionTitle = session.getTitle();
        String sessionDescription = session.getDescription();
        String instructorName = session.getInstructor().getPerson().getFullName();
        String skillName = session.getSkill().getName();
        String categoryName = session.getSkill().getKnowledgeArea().getName();

        // Formatear fecha y hora
        String formattedDate = formatDate(session.getScheduledDatetime());
        String formattedTime = formatTime(session.getScheduledDatetime());
        String duration = session.getDurationMinutes() + " minutos";

        String accessLink = booking.getAccessLink();
        String mySessionsLink = frontendUrl + "/app/my-sessions";

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
                "                                Hola <strong style='color: #aae16b;'>" + person.getFullName() + "</strong>," +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Te has registrado exitosamente en la siguiente sesión de aprendizaje:" +
                "                            </p>" +

                "                            <!-- Detalles de la sesión -->" +
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
                "                                        <td style='color: #ffffff;'>" + instructorName + "</td>" +
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

                "                            <!-- Enlace de acceso -->" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #504ab7;'>" +
                "                                <h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'>Enlace de acceso a la sesión:</h4>" +
                "                                <p style='font-size: 14px; word-break: break-all; color: #aae16b; background-color: #141414; padding: 15px; border-radius: 5px; margin: 10px 0;'>" +
                "                                    <a href='" + accessLink + "' style='color: #aae16b; text-decoration: none;'>" + accessLink + "</a>" +
                "                                </p>" +
                "                                <p style='font-size: 12px; color: #b0b0b0; margin: 5px 0 0 0;'>" +
                "                                    Guarda este enlace en un lugar seguro. Lo necesitarás para acceder a la sesión." +
                "                                </p>" +
                "                            </div>" +

                "                            <!-- Botón para ver mis sesiones -->" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + mySessionsLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Mis Sesiones</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +

                "                            <!-- Recordatorios -->" +
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
                "                                Si necesitas cancelar tu registro, puedes hacerlo desde tu panel de sesiones." +
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
     * Construye el template HTML para el correo de cancelación de booking.
     */
    private String buildBookingCancellationTemplate(Booking booking, Person person) {
        LearningSession session = booking.getLearningSession();
        String sessionTitle = session.getTitle();
        String formattedDate = formatDate(session.getScheduledDatetime());
        String sessionsLink = frontendUrl + "/app/sessions";

        return "<!DOCTYPE html>" +
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
                "                                Hola <strong style='color: #aae16b;'>" + person.getFullName() + "</strong>," +
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
                "                                        <a href='" + sessionsLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Explorar Sesiones</a>" +
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

    /**
     * Formatea una fecha en español.
     */
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        return sdf.format(date);
    }

    /**
     * Formatea la hora.
     */
    private String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", new Locale("es", "ES"));
        return sdf.format(date);
    }
    //#endregion

    /**
     * Envía un correo de confirmación de booking grupal.
     *
     * @param booking el booking creado
     * @param person la persona que recibe el email
     * @param communityName nombre de la comunidad
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendGroupBookingConfirmationEmail(Booking booking, Person person, String communityName) throws MessagingException {
        String subject = "Confirmación de registro grupal - " + booking.getLearningSession().getTitle();
        String htmlContent = buildGroupBookingConfirmationTemplate(booking, person, communityName);

        sendHtmlEmail(person.getEmail(), subject, htmlContent);
    }
    /**
     * Construye el template HTML para el correo de confirmación de booking grupal.
     */
    private String buildGroupBookingConfirmationTemplate(Booking booking, Person person, String communityName) {
        LearningSession session = booking.getLearningSession();

        String sessionTitle = session.getTitle();
        String sessionDescription = session.getDescription();
        String instructorName = session.getInstructor().getPerson().getFullName();
        String skillName = session.getSkill().getName();
        String categoryName = session.getSkill().getKnowledgeArea().getName();

        String formattedDate = formatDate(session.getScheduledDatetime());
        String formattedTime = formatTime(session.getScheduledDatetime());
        String duration = session.getDurationMinutes() + " minutos";

        String accessLink = booking.getAccessLink();
        String mySessionsLink = frontendUrl + "/app/my-sessions";

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Confirmación de Registro Grupal</title>" +
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
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>Registro Grupal Confirmado</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Hola <strong style='color: #aae16b;'>" + person.getFullName() + "</strong>," +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Tu comunidad <strong style='color: #aae16b;'>" + communityName + "</strong> se ha registrado exitosamente en la siguiente sesión de aprendizaje:" +
                "                            </p>" +

                "                            <!-- Detalles de la sesión -->" +
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
                "                                        <td style='color: #ffffff;'>" + instructorName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Habilidad:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + skillName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Categoría:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + categoryName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Comunidad:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + communityName + "</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +

                "                            <!-- Enlace de acceso -->" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #504ab7;'>" +
                "                                <h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'>Enlace de acceso a la sesión:</h4>" +
                "                                <p style='font-size: 14px; word-break: break-all; color: #aae16b; background-color: #141414; padding: 15px; border-radius: 5px; margin: 10px 0;'>" +
                "                                    <a href='" + accessLink + "' style='color: #aae16b; text-decoration: none;'>" + accessLink + "</a>" +
                "                                </p>" +
                "                                <p style='font-size: 12px; color: #b0b0b0; margin: 5px 0 0 0;'>" +
                "                                    Este es tu enlace personal de acceso. Cada miembro de tu comunidad recibirá su propio enlace." +
                "                                </p>" +
                "                            </div>" +

                "                            <!-- Botón para ver mis sesiones -->" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + mySessionsLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Mis Sesiones</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +

                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Asegúrate de coordinar con los demás miembros de tu comunidad para asistir juntos." +
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
     * Envía un correo de confirmación de booking grupal usando datos pre-cargados.
     * Este método es seguro para usar en hilos asíncronos.
     *
     * @param data mapa con todos los datos necesarios
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendGroupBookingConfirmationEmailFromData(Map<String, Object> data) throws MessagingException {
        String subject = "Confirmación de registro grupal - " + data.get("sessionTitle");
        String htmlContent = buildGroupBookingConfirmationTemplateFromData(data);

        sendHtmlEmail((String) data.get("personEmail"), subject, htmlContent);
    }

    /**
     * Construye el template HTML usando datos pre-cargados.
     */
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
        String mySessionsLink = frontendUrl + "/app/my-sessions";

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Confirmación de Registro Grupal</title>" +
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
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>Registro Grupal Confirmado</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Hola <strong style='color: #aae16b;'>" + personFullName + "</strong>," +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Tu comunidad <strong style='color: #aae16b;'>" + communityName + "</strong> se ha registrado exitosamente en la siguiente sesión de aprendizaje:" +
                "                            </p>" +

                "                            <!-- Detalles de la sesión -->" +
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
                "                                        <td style='color: #ffffff;'>" + instructorName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Habilidad:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + skillName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Categoría:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + categoryName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Comunidad:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + communityName + "</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +

                "                            <!-- Enlace de acceso -->" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #504ab7;'>" +
                "                                <h4 style='color: #aae16b; margin-top: 0; font-size: 16px;'>Enlace de acceso a la sesión:</h4>" +
                "                                <p style='font-size: 14px; word-break: break-all; color: #aae16b; background-color: #141414; padding: 15px; border-radius: 5px; margin: 10px 0;'>" +
                "                                    <a href='" + accessLink + "' style='color: #aae16b; text-decoration: none;'>" + accessLink + "</a>" +
                "                                </p>" +
                "                                <p style='font-size: 12px; color: #b0b0b0; margin: 5px 0 0 0;'>" +
                "                                    Este es tu enlace personal de acceso. Cada miembro de tu comunidad recibirá su propio enlace." +
                "                                </p>" +
                "                            </div>" +

                "                            <!-- Botón para ver mis sesiones -->" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + mySessionsLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Mis Sesiones</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +

                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Asegúrate de coordinar con los demás miembros de tu comunidad para asistir juntos." +
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
     * Envía un correo al instructor notificando la cancelación de un registro.
     *
     * @param session la sesión afectada
     * @param instructorPerson persona del instructor
     * @param learnerName nombre del estudiante que canceló
     * @param isGroup si fue cancelación grupal
     * @param spotsFreed cupos liberados
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendInstructorNotificationEmail(LearningSession session, Person instructorPerson,
                                                String learnerName, boolean isGroup, int spotsFreed) throws MessagingException {
        String subject = "Cancelación de registro - " + session.getTitle();
        String htmlContent = buildInstructorNotificationTemplate(session, instructorPerson, learnerName, isGroup, spotsFreed);

        sendHtmlEmail(instructorPerson.getEmail(), subject, htmlContent);
    }

    /**
     * Construye el template HTML para notificar al instructor.
     */
    private String buildInstructorNotificationTemplate(LearningSession session, Person instructorPerson,
                                                       String learnerName, boolean isGroup, int spotsFreed) {
        String sessionTitle = session.getTitle();
        String formattedDate = formatDate(session.getScheduledDatetime());
        String formattedTime = formatTime(session.getScheduledDatetime());

        long confirmedBookings = session.getCurrentBookings() - spotsFreed; // Aproximación
        int availableSpots = session.getMaxCapacity() - (int)confirmedBookings;

        String cancellationType = isGroup ? "grupal" : "individual";
        String spotsText = spotsFreed == 1 ? "1 cupo ha" : spotsFreed + " cupos han";

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Notificación de Cancelación</title>" +
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
                "                            <h2 style='color: #ffa500; margin-top: 0; font-size: 24px;'>📢 Cancelación de Registro</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Hola <strong style='color: #aae16b;'>" + instructorPerson.getFullName() + "</strong>," +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Te informamos que <strong style='color: #aae16b;'>" + learnerName + "</strong> ha cancelado su registro " + cancellationType + " en tu sesión:" +
                "                            </p>" +

                "                            <!-- Detalles de la sesión -->" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #ffa500;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 18px;'>" + sessionTitle + "</h3>" +
                "                                <table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px; margin-top: 15px;'>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; width: 40%;'><strong>Fecha:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + formattedDate + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b;'><strong>Hora:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + formattedTime + "</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +

                "                            <!-- Información de cupos -->" +
                "                            <div style='background-color: #2a2a2a; padding: 20px; border-radius: 8px; margin: 25px 0;'>" +
                "                                <h4 style='color: #aae16b; margin: 0 0 15px 0; font-size: 16px;'>Estado de la sesión:</h4>" +
                "                                <table width='100%' cellpadding='5' cellspacing='0' style='font-size: 14px;'>" +
                "                                    <tr>" +
                "                                        <td style='color: #b0b0b0;'><strong>Cupos liberados:</strong></td>" +
                "                                        <td style='color: #ffffff; text-align: right;'>" + spotsFreed + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #b0b0b0;'><strong>Cupos disponibles:</strong></td>" +
                "                                        <td style='color: #aae16b; text-align: right; font-weight: bold;'>" + availableSpots + "/" + session.getMaxCapacity() + "</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +

                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0;'>" +
                "                                " + spotsText + " sido liberado(s) y ahora está(n) disponible(s) para nuevos registros." +
                "                            </p>" +

                "                            <!-- Botón -->" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + frontendUrl + "/app/my-sessions' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Mis Sesiones</a>" +
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

}