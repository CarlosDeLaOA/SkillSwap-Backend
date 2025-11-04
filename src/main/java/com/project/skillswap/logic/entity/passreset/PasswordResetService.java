package com.project.skillswap.logic.entity.passreset;

// OJO: usa tu PersonRepository EXISTENTE (en entity.Person)
import com.project.skillswap.logic.entity.Person.PersonRepository;

import jakarta.transaction.Transactional;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.*;

@Service //
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

    /** Firma EXACTA que estás usando desde el controller */
    @Transactional
    public void requestReset(String email, @Nullable String requestIp, @Nullable String userAgent) {
        // implementación real la agregamos luego; por ahora deja un no-op seguro
        if (email == null || email.isBlank()) return;
        // Ejemplo mínimo que no hace nada pero compila.
    }
}
