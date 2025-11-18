package com.project.skillswap.logic.entity.LearningSession.email;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Servicio para enviar emails de confirmación de sesiones
 * Notifica al instructor cuando una sesión es publicada exitosamente
 */
@Service
public class SessionEmailService {

    //#region Dependencies
    @Autowired
    private JavaMailSender mailSender;

    @Value("${mail.from:${spring.mail.username}}")
    private String from;
    //#endregion

    //#region Public Methods
    /**
     * Envía email de confirmación cuando una sesión es publicada
     *
     * @param session Sesión publicada
     * @param instructor Instructor propietario
     * @return true si se envió exitosamente
     */
    public boolean sendSessionCreationEmail(LearningSession session, Person instructor) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setFrom(from);
            helper.setTo(instructor.getEmail());
            helper.setSubject(buildSubject(session));

            String htmlContent = buildSessionConfirmationEmail(session, instructor);
            helper.setText(htmlContent, true);

            mailSender.send(msg);

            System.out.println(" [SessionEmailService] ✅ Email de confirmación enviado a: " + instructor.getEmail());
            return true;

        } catch (Exception e) {
            System.err.println(" [SessionEmailService] ❌ Error enviando email: " + e.getMessage());
            return false;
        }
    }
    //#endregion

    //#region Private Methods
    /**
     * Construye el asunto del email
     */
    private String buildSubject(LearningSession session) {
        return "✅ Tu sesión '" + session.getTitle() + "' ha sido publicada exitosamente";
    }

    /**
     * Construye el contenido HTML del email de confirmación
     * Usa el mismo estilo visual que SmtpMailService para consistencia
     */
    private String buildSessionConfirmationEmail(LearningSession session, Person instructor) {
        String formattedDate = formatDateTime(session.getScheduledDatetime());
        String skillName = session.getSkill().getName();
        String sessionLink = "https://app.skillswap.com/sessions/" + session.getId();

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Sesión Publicada</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <!-- Header -->" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>✅ SkillSwap</h1>" +
                "                            <p style='color: rgba(255, 255, 255, 0.8); margin: 8px 0 0 0; font-size: 14px;'>Sesión Publicada</p>" +
                "                        </td>" +
                "                    </tr>" +

                "                    <!-- Contenido Principal -->" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola " + instructor.getFullName() + "!</h2>" +

                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Tu sesión <strong style='color: #aae16b;'>\"" + session.getTitle() + "\"</strong> ha sido <strong style='color: #aae16b;'>publicada exitosamente</strong> y está disponible para que los estudiantes se inscriban." +
                "                            </p>" +

                "                            <!-- Detalles de la Sesión -->" +
                "                            <div style='background-color: #39434b; padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; margin-bottom: 15px; font-size: 20px;'>" + session.getTitle() + "</h3>" +

                "                                <table width='100%' cellpadding='10' cellspacing='0' style='font-size: 14px;'>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; width: 30%; vertical-align: top;'><strong>📅 Fecha y Hora:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + formattedDate + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; vertical-align: top;'><strong>⏱️ Duración:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + session.getDurationMinutes() + " minutos</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; vertical-align: top;'><strong>🎯 Habilidad:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + skillName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; vertical-align: top;'><strong>👥 Capacidad:</strong></td>" +
                "                                        <td style='color: #ffffff;'>" + session.getMaxCapacity() + " participantes</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +

                "                            <!-- Info de Inscripciones -->" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #504ab7;'>" +
                "                                <h4 style='color: #aae16b; margin-top: 0; margin-bottom: 10px; font-size: 16px;'>📊 Inscripciones</h4>" +
                "                                <p style='margin: 0; font-size: 14px; color: #ffffff;'>" +
                "                                    Los estudiantes ya pueden ver tu sesión y registrarse. Recibirás notificaciones de nuevas inscripciones." +
                "                                </p>" +
                "                            </div>" +

                "                            <!-- Botón de Acción -->" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + sessionLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Sesión</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +

                "                            <p style='font-size: 13px; color: #b0b0b0; margin-top: 30px; line-height: 1.6;'>" +
                "                                Si tienes preguntas, no dudes en contactar con nuestro equipo de soporte." +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +

                "                    <!-- Footer -->" +
                "                    <tr>" +
                "                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>" +
                "                                © 2025 SkillSwap. Todos los derechos reservados." +
                "                            </p>" +
                "                            <p style='margin: 10px 0 0 0; font-size: 11px; color: #888888;'>" +
                "                                Este es un correo automático, por favor no responder." +
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
     * Formatea fecha y hora
     */
    private String formatDateTime(java.util.Date dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy 'a las' HH:mm",
                new Locale("es", "ES"));
        return sdf.format(dateTime);
    }
    //#endregion
}