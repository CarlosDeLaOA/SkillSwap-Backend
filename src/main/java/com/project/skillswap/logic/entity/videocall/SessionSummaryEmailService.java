package com.project.skillswap.logic.entity.videocall;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Servicio encargado de enviar a los participantes un correo con
 * el resumen de una sesión educativa, incluyendo el PDF generado.
 */
@Service
public class SessionSummaryEmailService {
    private static final Logger logger = LoggerFactory.getLogger(SessionSummaryEmailService.class);

    //#region Dependencies & Config

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@skillswap.com}")
    private String fromEmail;

    @Value("${app.name:SkillSwap}")
    private String appName;

    //#endregion



    //#region Public API

    /**
     * Envía el resumen de una sesión (PDF + detalles) a todos los participantes.
     *
     * @param session    Sesión educativa
     * @param summaryPdf PDF generado del resumen
     * @param summary    Contenido textual del resumen
     * @return true si al menos un correo fue enviado correctamente
     */
    public boolean sendSummaryToParticipants(
            LearningSession session,
            byte[] summaryPdf,
            String summary
    ) {
        try {
            List<Person> participants = getAllParticipants(session);

            if (participants.isEmpty()) {
                return false;
            }

            int successfulEmails = 0;

            for (Person participant : participants) {
                try {
                    if (sendSummaryEmail(participant, session, summaryPdf, summary)) {
                        successfulEmails++;
                    }
                } catch (Exception ignored) {
                    // Continuar con los demás participantes
                }
            }

            return successfulEmails > 0;

        } catch (Exception e) {
            return false;
        }
    }

    //#endregion



    //#region Participant Collection

    /**
     * Obtiene todos los participantes de la sesión:
     * - Instructor
     * - Learners con booking confirmado
     *
     * @param session Sesión
     * @return Lista de personas sin duplicados
     */
    private List<Person> getAllParticipants(LearningSession session) {
        Set<Person> participants = new HashSet<>();

        if (session.getInstructor() != null && session.getInstructor().getPerson() != null) {
            participants.add(session.getInstructor().getPerson());
        }

        if (session.getBookings() != null) {
            for (Booking booking : session.getBookings()) {
                if (booking.getLearner() != null && booking.getLearner().getPerson() != null) {
                    participants.add(booking.getLearner().getPerson());
                }
            }
        }

        return new ArrayList<>(participants);
    }

    //#endregion



    //#region Email Sending

    /**
     * Envía un correo individual a un participante con:
     * - PDF adjunto del resumen
     * - Información general de la sesión
     *
     * @param recipient Persona destinataria
     * @param session   Sesión correspondiente
     * @param summaryPdf PDF adjunto
     * @param summary   Resumen textual
     * @return true si se envió exitosamente
     */
    private boolean sendSummaryEmail(
            Person recipient,
            LearningSession session,
            byte[] summaryPdf,
            String summary
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipient.getEmail());
            helper.setSubject("Resumen de tu Sesión - " + session.getTitle());

            helper.setText(buildEmailBody(recipient, session, summary), true);

            String fileName = "resumen_sesion_" + session.getId() + "_" +
                    new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf";

            helper.addAttachment(fileName, new ByteArrayResource(summaryPdf), "application/pdf");

            mailSender.send(message);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    //#endregion



    //#region Email Body

    /**
     * Construye el cuerpo HTML del email enviado al participante.
     *
     * @param recipient Persona destinataria
     * @param session   Sesión correspondiente
     * @param summary   Resumen textual generado por IA
     * @return Cuerpo del correo como HTML
     */
    private String buildEmailBody(Person recipient, LearningSession session, String summary) {

        String instructorName = session.getInstructor() != null &&
                session.getInstructor().getPerson() != null
                ? session.getInstructor().getPerson().getFullName()
                : "N/A";

        String skillName = session.getSkill() != null
                ? session.getSkill().getName()
                : "N/A";

        String sessionDate = session.getScheduledDatetime() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(session.getScheduledDatetime())
                : "N/A";

        // Escapar el summary para HTML (reemplazar caracteres especiales)
        String escapedSummary = summary != null
                ? summary.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>")
                : "No hay resumen disponible.";

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Resumen de Sesión</title>" +
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
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola, " + recipient.getFullName() + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Tu sesión de <strong style='color: #aae16b;'>" + appName + "</strong> ha sido procesada correctamente." +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Hemos generado un resumen conciso de tu sesión reciente para que puedas revisar los puntos clave en cualquier momento." +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 30px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 18px; border-bottom: 2px solid #504ab7; padding-bottom: 10px;'>Detalles de la Sesión</h3>" +
                "                                <table width='100%' cellpadding='8' cellspacing='0' style='font-size: 14px;'>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold; width: 120px;'>Título:</td>" +
                "                                        <td style='color: #ffffff;'>" + session.getTitle() + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>Instructor:</td>" +
                "                                        <td style='color: #ffffff;'>" + instructorName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>Habilidad:</td>" +
                "                                        <td style='color: #ffffff;'>" + skillName + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>Fecha:</td>" +
                "                                        <td style='color: #ffffff;'>" + sessionDate + "</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 18px;'>Resumen de la Sesión</h3>" +
                "                                <p style='font-size: 14px; line-height: 1.8; color: #ffffff; margin: 0;'>" +
                escapedSummary +
                "                                </p>" +
                "                            </div>" +
                "                            <div style='background-color: #504ab7; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #aae16b;'>" +
                "                                <p style='font-size: 14px; line-height: 1.6; color: #ffffff; margin: 0;'>" +
                "                                    <strong>📄 Archivo Adjunto:</strong> Adjunto encontrarás un documento PDF con el resumen completo generado automáticamente." +
                "                                </p>" +
                "                            </div>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Saludos cordiales,<br>" +
                "                                <strong style='color: #aae16b;'>El Equipo de " + appName + "</strong>" +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>" +
                "                                Este es un mensaje automático. Por favor no respondas a este correo." +
                "                            </p>" +
                "                            <p style='margin: 10px 0 0 0; font-size: 12px; color: #b0b0b0;'>" +
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

    //#endregion

}