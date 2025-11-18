package com.project.skillswap.logic.entity.LearningSession.reminders;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Constructor de emails de recordatorio para sesiones
 * Genera HTML para emails de recordatorio 24 horas antes
 * CRITERIO 6: Email de recordatorio automático
 *
 * Usa el mismo formato y diseño que SmtpMailService para consistencia visual
 */
@Component
public class SessionReminderEmailBuilder {

    //#region Public Methods
    /**
     * Construye el asunto del email de recordatorio
     *
     * @param session Sesión
     * @return Asunto del email
     */
    public String buildSubject(LearningSession session) {
        return "🔔 Recordatorio: Tu sesión '" + session.getTitle() + "' comienza mañana";
    }

    /**
     * Construye el contenido HTML del email de recordatorio
     * Usa el mismo estilo visual que SmtpMailService para consistencia
     *
     * @param session Sesión
     * @return HTML formateado
     */
    public String buildReminderEmail(LearningSession session) {
        String formattedDate = formatDateTime(session.getScheduledDatetime());
        String instructorName = session.getInstructor().getPerson().getFullName();
        String skillName = session.getSkill().getName();
        String sessionLink = "https://app.skillswap.com/sessions/" + session.getId();

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Recordatorio de Sesión</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <!-- Header con gradiente SkillSwap -->" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>🔔 SkillSwap</h1>" +
                "                            <p style='color: rgba(255, 255, 255, 0.8); margin: 8px 0 0 0; font-size: 14px;'>Recordatorio de Sesión</p>" +
                "                        </td>" +
                "                    </tr>" +

                "                    <!-- Contenido Principal -->" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola " + instructorName + "!</h2>" +

                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Este es un recordatorio de que tu sesión <strong style='color: #aae16b;'>comienza mañana</strong>." +
                "                            </p>" +

                "                            <!-- Caja de Detalles de la Sesión -->" +
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

                "                            <!-- Checklist de Preparación -->" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #504ab7;'>" +
                "                                <h4 style='color: #aae16b; margin-top: 0; margin-bottom: 15px; font-size: 16px;'>✅ Antes de comenzar:</h4>" +
                "                                <ul style='color: #ffffff; font-size: 14px; line-height: 1.8; padding-left: 20px; margin: 0;'>" +
                "                                    <li>Verifica tu conexión a internet</li>" +
                "                                    <li>Prueba micrófono y cámara</li>" +
                "                                    <li>Prepara tu material y recursos</li>" +
                "                                    <li>Llega 5 minutos antes</li>" +
                "                                </ul>" +
                "                            </div>" +

                "                            <!-- Botón de Acción -->" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + sessionLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Sesión</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +

                "                            <!-- Información de Seguridad -->" +
                "                            <div style='background-color: #2e1a1a; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #ff4444;'>" +
                "                                <p style='margin: 0; font-size: 14px; color: #ffffff;'>" +
                "                                    <strong style='color: #ff4444;'>🔒 Nota:</strong> Este es un correo automático de recordatorio. Si no reconoces esta sesión, contacta con nuestro equipo de soporte." +
                "                                </p>" +
                "                            </div>" +

                "                            <p style='font-size: 13px; color: #b0b0b0; margin-top: 30px; line-height: 1.6;'>" +
                "                                Si tienes preguntas o necesitas ayuda, no dudes en contactarnos. Estamos aquí para apoyarte." +
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
    //#endregion

    //#region Private Methods
    /**
     * Formatea fecha y hora para mostrar en email
     *
     * @param dateTime Fecha/hora a formatear
     * @return String formateado (ej: "martes, 19 de noviembre de 2025 a las 10:30")
     */
    private String formatDateTime(java.util.Date dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy 'a las' HH:mm",
                new Locale("es", "ES"));
        return sdf.format(dateTime);
    }
    //#endregion
}
