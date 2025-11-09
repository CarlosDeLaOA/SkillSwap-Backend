package com.project.skillswap.logic.entity.Passreset;

import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    //#region dependencies
    private final JavaMailSender mailSender;
    private final String from;
    //#endregion

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    //#region API
    public void sendResetCode(String to, String code) {
        String subject = "Tu código de recuperación";
        String html = """
      <div style="font-family:Inter,Arial,sans-serif;line-height:1.4">
        <h2 style="margin:0 0 12px">Recuperar contraseña</h2>
        <p>Usa este código para continuar:</p>
        <div style="font-size:22px;font-weight:700;letter-spacing:3px;margin:8px 0 14px">%s</div>
        <p style="color:#666;margin:0">Expira en 30 minutos.</p>
      </div>
    """.formatted(code);
        sendHtml(to, subject, html);
    }
    //#endregion

    //#region internals
    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException("No se pudo enviar el correo", e);
        }
    }
    //#endregion
}