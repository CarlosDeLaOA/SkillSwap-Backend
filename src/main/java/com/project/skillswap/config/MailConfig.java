package com.project.skillswap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuración del JavaMailSender.
 *
 * Estrategia de configuración:
 * - Si hay configuración SMTP en application.properties, la usa
 * - Si no hay configuración, crea un sender "no-op" para que la app funcione sin SMTP
 *
 * Esto permite que la aplicación funcione tanto en desarrollo (sin SMTP)
 * como en producción (con SMTP configurado).
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean starttlsEnable;

    /**
     * Crea el bean JavaMailSender.
     *
     * Si hay configuración SMTP válida, la usa.
     * Si no, crea un sender vacío (no-op) para evitar errores de arranque.
     *
     * @return JavaMailSender configurado o no-op
     */
    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Verificar si hay configuración SMTP válida
        boolean hasValidConfig = mailHost != null && !mailHost.trim().isEmpty()
                && mailUsername != null && !mailUsername.trim().isEmpty()
                && mailPassword != null && !mailPassword.trim().isEmpty();

        if (hasValidConfig) {
            // Configuración completa con SMTP
            mailSender.setHost(mailHost);
            mailSender.setPort(mailPort);
            mailSender.setUsername(mailUsername);
            mailSender.setPassword(mailPassword);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", String.valueOf(smtpAuth));
            props.put("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
            props.put("mail.smtp.starttls.required", String.valueOf(starttlsEnable));
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");

        }

        return mailSender;
    }
}