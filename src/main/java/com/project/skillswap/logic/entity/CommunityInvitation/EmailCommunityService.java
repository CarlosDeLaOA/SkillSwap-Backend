package com.project.skillswap.logic.entity.CommunityInvitation;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio para el envío de correos electrónicos relacionados con comunidades de aprendizaje.
 */
@Service
public class EmailCommunityService {
    private static final Logger logger = LoggerFactory.getLogger(EmailCommunityService.class);

    //#region Dependencies
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailCommunityService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    //#endregion

    //#region Public Methods
    /**
     * Envía un correo de invitación a una comunidad.
     *
     * @param toEmail correo del destinatario
     * @param inviteeName nombre del invitado
     * @param communityName nombre de la comunidad
     * @param creatorName nombre del creador
     * @param token token de invitación
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendCommunityInvitation(String toEmail, String inviteeName, String communityName,
                                        String creatorName, String token) throws MessagingException {
        String invitationLink = frontendUrl + "/accept-community-invitation?token=" + token;
        String subject = "Invitación a comunidad de aprendizaje - " + communityName;
        String htmlContent = buildInvitationEmailTemplate(inviteeName, communityName, creatorName, invitationLink);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Envía un correo de confirmación después de unirse a una comunidad.
     *
     * @param toEmail correo del destinatario
     * @param memberName nombre del nuevo miembro
     * @param communityName nombre de la comunidad
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendWelcomeToCommunityEmail(String toEmail, String memberName, String communityName)
            throws MessagingException {
        String communityLink = frontendUrl + "/communities";
        String subject = "¡Bienvenido a " + communityName + "!";
        String htmlContent = buildWelcomeToCommunityTemplate(memberName, communityName, communityLink);

        sendHtmlEmail(toEmail, subject, htmlContent);
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
     * Construye el template HTML para el correo de invitación.
     *
     * @param inviteeName nombre del invitado
     * @param communityName nombre de la comunidad
     * @param creatorName nombre del creador
     * @param invitationLink link de invitación
     * @return contenido HTML del correo
     */
    private String buildInvitationEmailTemplate(String inviteeName, String communityName,
                                                String creatorName, String invitationLink) {
        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Invitación a Comunidad</title>" +
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
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola, " + inviteeName + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                <strong style='color: #aae16b;'>" + creatorName + "</strong> te ha invitado a unirte a la comunidad de aprendizaje <strong style='color: #aae16b;'>" + communityName + "</strong> en SkillSwap." +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Las comunidades de aprendizaje son espacios donde puedes compartir conocimientos, colaborar con otros y crecer juntos." +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Esta invitación es válida por <strong style='color: #aae16b;'>48 horas</strong>." +
                "                            </p>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + invitationLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Aceptar Invitación</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0;'>" +
                "                                Si no puedes hacer clic en el botón, copia y pega el siguiente enlace en tu navegador:" +
                "                            </p>" +
                "                            <p style='font-size: 12px; word-break: break-all; color: #504ab7; background-color: #39434b; padding: 10px; border-radius: 5px;'>" +
                "                                " + invitationLink +
                "                            </p>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Si no conoces a esta persona o no deseas unirte, puedes ignorar este correo de forma segura." +
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
     * Construye el template HTML para el correo de bienvenida a la comunidad.
     *
     * @param memberName nombre del nuevo miembro
     * @param communityName nombre de la comunidad
     * @param communityLink link a las comunidades
     * @return contenido HTML del correo
     */
    private String buildWelcomeToCommunityTemplate(String memberName, String communityName, String communityLink) {
        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Bienvenido a la Comunidad</title>" +
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
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Bienvenido, " + memberName + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Te has unido exitosamente a la comunidad <strong style='color: #aae16b;'>" + communityName + "</strong>." +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Ahora puedes colaborar con otros miembros, compartir conocimientos y participar en sesiones grupales de aprendizaje." +
                "                            </p>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + communityLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Mis Comunidades</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 18px;'>¿Qué puedes hacer ahora?</h3>" +
                "                                <ul style='color: #ffffff; font-size: 14px; line-height: 1.8; padding-left: 20px;'>" +
                "                                    <li>Conocer a los demás miembros de la comunidad</li>" +
                "                                    <li>Participar en sesiones grupales de aprendizaje</li>" +
                "                                    <li>Compartir documentos y recursos</li>" +
                "                                    <li>Colaborar en proyectos conjuntos</li>" +
                "                                </ul>" +
                "                            </div>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0 0 0;'>" +
                "                                Si tienes alguna pregunta, no dudes en contactar al administrador de la comunidad." +
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