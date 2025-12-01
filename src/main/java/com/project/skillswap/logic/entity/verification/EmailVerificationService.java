
package com.project.skillswap.logic.entity.verification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio para el envío de correos electrónicos de verificación personalizados.
 */
@Service
public class EmailVerificationService {

    //#region Dependencies
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailVerificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    //#endregion

    //#region Public Methods
    /**
     * Envía un correo de verificación con el link y token correspondiente.
     *
     * @param toEmail correo del destinatario
     * @param fullName nombre completo del destinatario
     * @param token token de verificación
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendVerificationEmail(String toEmail, String fullName, String token) throws MessagingException {
        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        String subject = "Verifica tu cuenta en SkillSwap";
        String htmlContent = buildVerificationEmailTemplate(fullName, verificationLink);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Envía un correo de confirmación después de verificación exitosa.
     *
     * @param toEmail correo del destinatario
     * @param fullName nombre completo del destinatario
     * @throws MessagingException si hay un error al enviar el correo
     */
    public void sendWelcomeEmail(String toEmail, String fullName) throws MessagingException {
        String loginLink = frontendUrl + "/login";
        String subject = "¡Bienvenido a SkillSwap!";
        String htmlContent = buildWelcomeEmailTemplate(fullName, loginLink);

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
     * Construye el template HTML para el correo de verificación.
     *
     * @param fullName nombre del usuario
     * @param verificationLink link de verificación
     * @return contenido HTML del correo
     */
    private String buildVerificationEmailTemplate(String fullName, String verificationLink) {
        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Verificación de Cuenta</title>" +
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
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola, " + fullName + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Gracias por registrarte en <strong style='color: #aae16b;'>SkillSwap</strong>. Para completar tu registro y activar tu cuenta, por favor verifica tu correo electrónico haciendo clic en el botón de abajo." +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Este enlace es válido por <strong style='color: #aae16b;'>24 horas</strong>." +
                "                            </p>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + verificationLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Verificar mi cuenta</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0;'>" +
                "                                Si no puedes hacer clic en el botón, copia y pega el siguiente enlace en tu navegador:" +
                "                            </p>" +
                "                            <p style='font-size: 12px; word-break: break-all; color: #504ab7; background-color: #39434b; padding: 10px; border-radius: 5px;'>" +
                "                                " + verificationLink +
                "                            </p>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Si no creaste esta cuenta, puedes ignorar este correo de forma segura." +
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
     * Construye el template HTML para el correo de bienvenida.
     *
     * @param fullName nombre del usuario
     * @param loginLink link de inicio de sesión
     * @return contenido HTML del correo
     */
    private String buildWelcomeEmailTemplate(String fullName, String loginLink) {
        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Bienvenido a SkillSwap</title>" +
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
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Bienvenido, " + fullName + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Tu cuenta ha sido verificada exitosamente. Ahora puedes acceder a todas las funcionalidades de <strong style='color: #aae16b;'>SkillSwap</strong>." +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Estamos emocionados de tenerte en nuestra comunidad de aprendizaje e intercambio de habilidades." +
                "                            </p>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <a href='" + loginLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Iniciar Sesión</a>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 18px;'>¿Qué puedes hacer ahora?</h3>" +
                "                                <ul style='color: #ffffff; font-size: 14px; line-height: 1.8; padding-left: 20px;'>" +
                "                                    <li>Explora habilidades y encuentra instructores</li>" +
                "                                    <li>Completa tu perfil para destacar</li>" +
                "                                    <li>Únete a comunidades de aprendizaje</li>" +
                "                                    <li>Comienza a compartir tus conocimientos</li>" +
                "                                </ul>" +
                "                            </div>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0 0 0;'>" +
                "                                Si tienes alguna pregunta, no dudes en contactarnos." +
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