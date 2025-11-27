package com.project.skillswap.logic.entity.GroupSessionDocument;

import com.project.skillswap.logic.entity.CommunityMember.CommunityMember;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMemberRepository;
import com. project.skillswap.logic.entity.Person.Person;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para enviar notificaciones por email cuando se sube un nuevo documento.
 */
@Service
public class EmailDocumentNotificationService {

    //#region Dependencies
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private CommunityMemberRepository memberRepository;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;
    //#endregion

    //#region Public Methods

    /**
     * Envía notificación a todos los miembros de la comunidad sobre un nuevo documento.
     *
     * @param document documento subido
     */
    @Async
    public void sendNewDocumentNotification(GroupSessionDocument document) {
        Long communityId = document.getLearningCommunity().getId();
        String communityName = document.getLearningCommunity().getName();
        String uploaderName = document.getUploadedBy().getFullName();
        Long uploaderId = document.getUploadedBy().getId();

        // Obtener todos los miembros activos de la comunidad
        List<CommunityMember> members = memberRepository.findActiveMembersByCommunityId(communityId);

        for (CommunityMember member : members) {
            Person person = member.getLearner().getPerson();

            // No enviar notificación al que subió el documento
            if (person.getId().equals(uploaderId)) {
                continue;
            }

            try {
                sendDocumentEmail(person.getEmail(), person.getFullName(), document, communityName, uploaderName);
                System.out.println("Notificación enviada a: " + person.getEmail());
            } catch (MessagingException e) {
                System.err.println("Error enviando notificación a " + person.getEmail() + ": " + e.getMessage());
            }
        }
    }

    //#endregion

    //#region Private Methods

    /**
     * Envía el correo de notificación a un miembro.
     */
    private void sendDocumentEmail(String toEmail, String memberName, GroupSessionDocument document,
                                   String communityName, String uploaderName) throws MessagingException {
        String subject = "Nuevo documento en " + communityName + " - SkillSwap";
        String htmlContent = buildDocumentEmailTemplate(memberName, document, communityName, uploaderName);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Construye el template HTML para el correo de notificación de documento.
     */
    private String buildDocumentEmailTemplate(String memberName, GroupSessionDocument document,
                                              String communityName, String uploaderName) {
        String documentLink = frontendUrl + "/app/community/" + document.getLearningCommunity().getId() + "? tab=documents";
        String sessionDateFormatted = document.getSessionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String fileSizeFormatted = document.getFormattedFileSize();

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Nuevo Documento</title>" +
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
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola, " + memberName + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                <strong style='color: #aae16b;'>" + uploaderName + "</strong> ha subido un nuevo documento a la comunidad <strong style='color: #aae16b;'>" + communityName + "</strong>." +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 10px; margin: 20px 0;'>" +
                "                                <table width='100%' cellpadding='0' cellspacing='0'>" +
                "                                    <tr>" +
                "                                        <td style='padding: 10px 0;'>" +
                "                                            <span style='color: rgba(255,255,255,0.7); font-size: 14px;'>Documento:</span><br>" +
                "                                            <span style='color: #ffffff; font-size: 16px; font-weight: bold;'>" + document.getOriginalFileName() + "</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='padding: 10px 0;'>" +
                "                                            <span style='color: rgba(255,255,255,0.7); font-size: 14px;'>Fecha de sesión:</span><br>" +
                "                                            <span style='color: #ffffff; font-size: 16px;'>" + sessionDateFormatted + "</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='padding: 10px 0;'>" +
                "                                            <span style='color: rgba(255,255,255,0. 7); font-size: 14px;'>Tamaño:</span><br>" +
                "                                            <span style='color: #ffffff; font-size: 16px;'>" + fileSizeFormatted + "</span>" +
                "                                        </td>" +
                "                                    </tr>" +
                (document.getDescription() != null && ! document.getDescription().isEmpty() ?
                        "                                    <tr>" +
                                "                                        <td style='padding: 10px 0;'>" +
                                "                                            <span style='color: rgba(255,255,255,0. 7); font-size: 14px;'>Descripción:</span><br>" +
                                "                                            <span style='color: #ffffff; font-size: 16px;'>" + document.getDescription() + "</span>" +
                                "                                        </td>" +
                                "                                    </tr>" : "") +
                "                                </table>" +
                "                            </div>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + documentLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Documentos</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0 0 0;'>" +
                "                                Puedes visualizar y descargar este documento desde la sección de documentos de tu comunidad." +
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

    //#endregion
}