package com.project.skillswap.logic.entity.passreset;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import jakarta.transaction.Transactional;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetService {

    public static class TooManyResetRequestsException extends RuntimeException {
        public TooManyResetRequestsException(String message) { super(message); }
    }

    private final PersonRepository personRepository;
    private final PasswordResetTokenRepository tokenRepo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    // Token válido por 30 minutos (ajusta si ocupás)
    private static final Duration TTL = Duration.ofMinutes(30);
    // Ventana simple para rate-limiting (p.ej. 5 min)
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(5);
    // Máximo de solicitudes en la ventana
    private static final long MAX_REQUESTS_IN_WINDOW = 3L;

    public PasswordResetService(
            PersonRepository personRepository,
            PasswordResetTokenRepository tokenRepo,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.personRepository = personRepository;
        this.tokenRepo = tokenRepo;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Firma EXACTA que usas desde el controller */
    @Transactional
    public void requestReset(String email, @Nullable String requestIp, @Nullable String userAgent) {
        if (email == null || email.isBlank()) return;

        // 1) Buscar persona (si no existe, salimos silenciosamente)
        Person person = personRepository.findByEmailIgnoreCase(email).orElse(null);
        if (person == null) return;

        // 2) Rate limiting simple
        Instant since = Instant.now(clock).minus(RATE_LIMIT_WINDOW);
        long count = tokenRepo.countRequestsSince(person, since);
        if (count >= MAX_REQUESTS_IN_WINDOW) {
            throw new TooManyResetRequestsException("Rate limit excedido");
        }

        // 3) TODO implementar:
        // - generar token claro (para email) + hash (para BD)
        // - guardar entidad PasswordResetToken (expira en TTL)
        // - enviar correo con enlace

        // Dejo no-op para que compile y no dispare inserts aún.
    }
}
