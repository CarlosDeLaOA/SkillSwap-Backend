package com.project.skillswap.logic.entity.passreset;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpMailService implements MailService {
    private final JavaMailSender mailSender;

    // Usa el from del properties; por defecto cae al username si no lo definís
    @Value("${mail.from:${spring.mail.username}}")
    private String from;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public SmtpMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetCode(String toEmail, String code, int ttlMinutes) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");

            h.setFrom(from);
            h.setTo(toEmail);
            h.setSubject("Restablece tu contraseña en SkillSwap");

            String htmlContent = buildPasswordResetEmailTemplate(code, ttlMinutes);
            h.setText(htmlContent, true);

            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Error sending reset email: " + e.getMessage(), e);
        }
    }

    /**
     * Construye el template HTML para el correo de restablecimiento de contraseña.
     *
     * @param code código de restablecimiento
     * @param ttlMinutes minutos de validez del código
     * @return contenido HTML del correo
     */
    private String buildPasswordResetEmailTemplate(String code, int ttlMinutes) {
        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Restablecer Contraseña</title>" +
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
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>Restablecer Contraseña</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Recibimos una solicitud para restablecer tu contraseña en <strong style='color: #aae16b;'>SkillSwap</strong>. Usa el código a continuación para completar el proceso." +
                "                            </p>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Este código es válido por <strong style='color: #aae16b;'>" + ttlMinutes + " minutos</strong>." +
                "                            </p>" +
                "                            <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                                <tr>" +
                "                                    <td align='center'>" +
                "                                        <div style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 20px 40px; border-radius: 5px; display: inline-block;'>" +
                "                                            <p style='margin: 0; color: #ffffff; font-size: 32px; font-weight: bold; letter-spacing: 5px;'>" + code + "</p>" +
                "                                        </div>" +
                "                                    </td>" +
                "                                </tr>" +
                "                            </table>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 20px 0;'>" +
                "                                Copia el código anterior y pégalo en la pantalla de restablecimiento de contraseña." +
                "                            </p>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Si no solicitaste restablecer tu contraseña, puedes ignorar este correo de forma segura. Tu cuenta seguirá siendo segura." +
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
}