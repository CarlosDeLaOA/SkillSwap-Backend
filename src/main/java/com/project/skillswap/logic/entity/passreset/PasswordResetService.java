package com.project.skillswap.logic.entity.passreset;
import com.project.skillswap.logic.entity.Person.PersonRepository;

import jakarta.transaction.Transactional;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.*;

@Service
public class PasswordResetService {

    public static class TooManyResetRequestsException extends RuntimeException {
        public TooManyResetRequestsException(String msg) { super(msg); }
    }

    private final PersonRepository personRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordResetService(
            PersonRepository personRepo,
            PasswordResetTokenRepository tokenRepo,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.personRepo = personRepo;
        this.tokenRepo = tokenRepo;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public void requestReset(String email, @Nullable String requestIp, @Nullable String userAgent) {
        if (email == null || email.isBlank()) return;
    }
}
