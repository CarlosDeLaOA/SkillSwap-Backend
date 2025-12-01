
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

            helper.setText(buildEmailBody(recipient, session, summary), false);

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
     * Construye el cuerpo textual del email enviado al participante.
     *
     * @param recipient Persona destinataria
     * @param session   Sesión correspondiente
     * @param summary   Resumen textual generado por IA
     * @return Cuerpo del correo como texto plano
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

        return String.format("""
        Hola %s,

        Tu sesión de %s ha sido procesada correctamente.

        Hemos generado un resumen conciso de tu sesión reciente para que puedas revisar los puntos clave en cualquier momento.

        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        DETALLES DE LA SESIÓN
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        Título:      %s
        Instructor:  %s
        Habilidad:   %s
        Fecha:       %s

        Adjunto encontrarás un documento PDF con el resumen completo generado automáticamente.

        Saludos cordiales,
        El Equipo de %s

        ---
        Este es un mensaje automático. Por favor no respondas a este correo.
        """,
                recipient.getFullName(),
                appName,
                session.getTitle(),
                instructorName,
                skillName,
                sessionDate,
                appName
        );
    }

    //#endregion

}
