package com.project.skillswap.logic.entity.passreset;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    // Bean “no-op” para desarrollo: evita el error de arranque aunque no haya SMTP configurado.
    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl(); // sin host/puerto: no enviará, pero permite que la app levante
    }
}
