
package com.project.skillswap.logic.entity.passreset;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpMailService implements MailService {
    private static final Logger logger = LoggerFactory.getLogger(SmtpMailService.class);
    private final JavaMailSender mailSender;

    // Usa el from del properties; por defecto cae al username si no lo definís
    @Value("${mail.from:${spring.mail.username}}")
    private String from;

    public SmtpMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetCode(String toEmail, String code, int ttlMinutes) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            // constructor con encoding; no hace multipart
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");

            h.setFrom(from);
            h.setTo(toEmail);
            h.setSubject("Tu código de restablecimiento");

            String html = """
                <p>Tu código es <b>%s</b>. Vence en %d minutos.</p>
                <p>Si no lo solicitaste, ignora este correo.</p>
            """.formatted(code, ttlMinutes);

            h.setText(html, true);

            // opcional pero útil
            // h.setReplyTo(from);

            mailSender.send(msg);
        } catch (Exception e) {
            // Aquí conviene loggear la causa real (SMTP 535, etc.)
            throw new RuntimeException("Error sending reset email: " + e.getMessage(), e);
        }
    }
}
