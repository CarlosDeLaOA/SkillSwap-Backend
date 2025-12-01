package com.project.skillswap.logic.entity.passreset;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.auth.PasswordResetToken;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PasswordResetService {

    //#region Constantes
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;

    private static final int MAX_REQUESTS_PER_HOUR = 3;
    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);
    private static final Duration REQUEST_COOLDOWN = Duration.ofSeconds(90);
    //#endregion

    //#region Dependencias
    private final PersonRepository personRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom rng = new SecureRandom();
    //#endregion

    public PasswordResetService(
            PersonRepository personRepo,
            PasswordResetTokenRepository tokenRepo,
            MailService mailService,
            PasswordEncoder passwordEncoder
    ) {
        this.personRepo = personRepo;
        this.tokenRepo = tokenRepo;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    //#region Lógica principal

    @Transactional
    public void requestReset(String email, String requestIp, String userAgent) {
        var personOpt = personRepo.findByEmailIgnoreCase(email);
        if (personOpt.isEmpty()) {
            return;
        }

        Person person = personOpt.get();

        Instant since = Instant.now().minus(REQUEST_WINDOW);
        long recent = tokenRepo.countRequestsSince(person, since);

        if (recent >= MAX_REQUESTS_PER_HOUR) {
            throw new TooManyResetRequestsException(
                    "Has superado el límite de solicitudes de restablecimiento de contraseña. Intenta más tarde."
            );
        }

        tokenRepo.findTopByPersonOrderByCreatedAtDesc(person).ifPresent(last -> {
            if (last.getCreatedAt() != null &&
                    last.getCreatedAt().isAfter(Instant.now().minus(REQUEST_COOLDOWN))) {
                throw new CooldownException("Debes esperar antes de solicitar un nuevo código.");
            }
        });

        String code = generate6DigitsCode();
        String hash = passwordEncoder.encode(code);

        PasswordResetToken token = new PasswordResetToken();
        token.setPerson(person);
        token.setTokenHash(hash);
        token.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        token.setRequestIp(requestIp);
        token.setUserAgent(truncate(userAgent, 512));
        tokenRepo.save(token);

        mailService.sendPasswordResetCode(person.getEmail(), code, (int) TOKEN_TTL.toMinutes());
    }

    @Transactional
    public void verifyCode(String email, String code) {
        var person = personRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundOrExpiredException("Código inválido o expirado."));

        var activeTokens = activeTokens(person);
        if (activeTokens.isEmpty()) throw new NotFoundOrExpiredException("Código inválido o expirado.");

        PasswordResetToken match = findMatchingToken(activeTokens, code);
        if (match == null) {
            var last = activeTokens.get(0);
            last.registerFailedAttempt();
            if (last.getAttempts() >= MAX_ATTEMPTS) last.markUsed();
            tokenRepo.save(last);
            throw new InvalidCodeException("El código ingresado no es válido.");
        }

        match.markVerified();
        tokenRepo.save(match);
    }

    @Transactional
    public void confirmReset(String email, String code, String newPassword) {
        var person = personRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundOrExpiredException("Código inválido o expirado."));

        var activeTokens = activeTokens(person);
        if (activeTokens.isEmpty()) throw new NotFoundOrExpiredException("Código inválido o expirado.");

        PasswordResetToken match = findMatchingToken(activeTokens, code);
        if (match == null) {
            var last = activeTokens.get(0);
            last.registerFailedAttempt();
            if (last.getAttempts() >= MAX_ATTEMPTS) last.markUsed();
            tokenRepo.save(last);
            throw new InvalidCodeException("El código ingresado es incorrecto.");
        }

        if (!isPasswordStrong(newPassword)) {
            throw new WeakPasswordException("La contraseña no cumple con los requisitos mínimos.");
        }

        //  LOG ANTES (temporal para debugging)
        System.out.println(" Password hash ANTES: " + person.getPasswordHash());

        //  Actualiza contraseña del usuario
        String newHash = passwordEncoder.encode(newPassword);
        person.setPasswordHash(newHash);

        //  LOG DESPUÉS (temporal para debugging)
        System.out.println(" Password hash DESPUÉS: " + person.getPasswordHash());
        System.out.println(" New hash generado: " + newHash);

        //  Guarda el cambio y fuerza escritura inmediata
        Person saved = personRepo.save(person);
        personRepo.flush();

        //  LOG GUARDADO (temporal para debugging)
        System.out.println(" Password hash GUARDADO: " + saved.getPasswordHash());
        System.out.println(" Person ID guardado: " + saved.getId());

        //  Invalida el token usado
        match.markUsed();
        tokenRepo.save(match);
        tokenRepo.flush();

        //  Invalida todos los demás tokens activos
        tokenRepo.consumeAllActive(person, Instant.now());
    }
    //#endregion

    //#region Helpers

    private List<PasswordResetToken> activeTokens(Person person) {
        return tokenRepo.findByPersonAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(person, Instant.now());
    }

    private PasswordResetToken findMatchingToken(List<PasswordResetToken> tokens, String code) {
        for (PasswordResetToken t : tokens) {
            if (t.isExpired() || t.isUsed()) continue;
            if (passwordEncoder.matches(code, t.getTokenHash())) return t;
        }
        return null;
    }

    private String generate6DigitsCode() {
        int n = rng.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private boolean isPasswordStrong(String pwd) {
        if (pwd == null) return false;
        boolean lengthOk = pwd.length() >= 8;
        boolean hasDigit = pwd.chars().anyMatch(Character::isDigit);
        boolean hasUpper = pwd.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = pwd.chars().anyMatch(Character::isLowerCase);
        boolean hasSpecial = pwd.chars().anyMatch(ch -> "@$!%*?&".indexOf(ch) >= 0);
        return lengthOk && hasDigit && hasUpper && hasLower && hasSpecial;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
    //#endregion

    //#region Excepciones personalizadas
    public static class NotFoundOrExpiredException extends RuntimeException {
        public NotFoundOrExpiredException(String m) { super(m); }
    }

    public static class InvalidCodeException extends RuntimeException {
        public InvalidCodeException(String m) { super(m); }
    }

    public static class WeakPasswordException extends RuntimeException {
        public WeakPasswordException(String m) { super(m); }
    }

    public static class CooldownException extends RuntimeException {
        public CooldownException(String m) { super(m); }
    }

    public static class TooManyResetRequestsException extends RuntimeException {
        public TooManyResetRequestsException(String m) { super(m); }
    }
    //#endregion
}