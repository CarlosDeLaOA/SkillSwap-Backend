package com.project.skillswap.logic.entity.verification;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio que gestiona la lógica de verificación de correo electrónico.
 */
@Service
public class VerificationService {

    //#region Constants
    private static final int TOKEN_EXPIRATION_HOURS = 24;
    //#endregion

    //#region Dependencies
    private final VerificationTokenRepository tokenRepository;
    private final PersonRepository personRepository;
    private final EmailVerificationService emailVerificationService;

    public VerificationService(VerificationTokenRepository tokenRepository,
                               PersonRepository personRepository,
                               EmailVerificationService emailVerificationService) {
        this.tokenRepository = tokenRepository;
        this.personRepository = personRepository;
        this.emailVerificationService = emailVerificationService;
    }
    //#endregion

    //#region Public Methods
    /**
     * Crea y envía un token de verificación para un usuario.
     *
     * @param person la persona para la cual crear el token
     * @throws MessagingException si hay un error al enviar el correo
     */
    @Transactional
    public void createAndSendVerificationToken(Person person) throws MessagingException {
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);

        VerificationToken verificationToken = new VerificationToken(token, person, expiresAt);
        tokenRepository.save(verificationToken);

        emailVerificationService.sendVerificationEmail(person.getEmail(), person.getFullName(), token);
    }

    /**
     * Verifica un token y activa la cuenta del usuario si es válido.
     *
     * @param token el token a verificar
     * @return resultado de la verificación
     */
    @Transactional
    public VerificationResult verifyToken(String token) {
        Optional<VerificationToken> optionalToken = tokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            return new VerificationResult(false, "Token inválido o no encontrado", VerificationStatus.INVALID_TOKEN);
        }

        VerificationToken verificationToken = optionalToken.get();
        Person person = verificationToken.getPerson();

        if (person.getEmailVerified()) {
            return new VerificationResult(false, "Esta cuenta ya ha sido verificada anteriormente", VerificationStatus.ALREADY_VERIFIED);
        }

        if (verificationToken.isVerified()) {
            return new VerificationResult(false, "Este token ya ha sido utilizado", VerificationStatus.TOKEN_USED);
        }

        if (verificationToken.isExpired()) {
            return new VerificationResult(false, "El token ha expirado. Solicita un nuevo enlace de verificación", VerificationStatus.EXPIRED_TOKEN);
        }

        verificationToken.setVerifiedAt(LocalDateTime.now());
        person.setEmailVerified(true);

        tokenRepository.save(verificationToken);
        personRepository.save(person);

        try {
            emailVerificationService.sendWelcomeEmail(person.getEmail(), person.getFullName());
        } catch (MessagingException e) {
            System.err.println("Error enviando correo de bienvenida: " + e.getMessage());
        }

        return new VerificationResult(true, "Cuenta verificada exitosamente", VerificationStatus.SUCCESS);
    }

    /**
     * Reenvía un token de verificación para un usuario.
     *
     * @param email correo del usuario
     * @return resultado del reenvío
     */
    @Transactional
    public ResendResult resendVerificationToken(String email) {
        Optional<Person> optionalPerson = personRepository.findByEmail(email);

        if (optionalPerson.isEmpty()) {
            return new ResendResult(false, "No se encontró una cuenta asociada a este correo", null);
        }

        Person person = optionalPerson.get();

        if (person.getEmailVerified()) {
            return new ResendResult(false, "Esta cuenta ya está verificada", null);
        }

        Optional<VerificationToken> existingToken = tokenRepository
                .findByPersonIdAndVerifiedAtIsNullAndExpiresAtAfter(person.getId(), LocalDateTime.now());

        existingToken.ifPresent(tokenRepository::delete);

        try {
            createAndSendVerificationToken(person);
            return new ResendResult(true, "Se ha enviado un nuevo correo de verificación", person.getEmail());
        } catch (MessagingException e) {
            return new ResendResult(false, "Error al enviar el correo. Intenta nuevamente", null);
        }
    }

    /**
     * Limpia tokens expirados de la base de datos.
     */
    @Transactional
    public void cleanExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
    //#endregion

    //#region Private Methods
    /**
     * Genera un token seguro y único.
     *
     * @return token generado
     */
    private String generateSecureToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }
    //#endregion

    //#region Inner Classes
    /**
     * Clase que representa el resultado de una verificación.
     */
    public static class VerificationResult {
        private final boolean success;
        private final String message;
        private final VerificationStatus status;

        public VerificationResult(boolean success, String message, VerificationStatus status) {
            this.success = success;
            this.message = message;
            this.status = status;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public VerificationStatus getStatus() {
            return status;
        }
    }

    /**
     * Clase que representa el resultado de un reenvío.
     */
    public static class ResendResult {
        private final boolean success;
        private final String message;
        private final String email;

        public ResendResult(boolean success, String message, String email) {
            this.success = success;
            this.message = message;
            this.email = email;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getEmail() {
            return email;
        }
    }

    /**
     * Enum con los estados posibles de verificación.
     */
    public enum VerificationStatus {
        SUCCESS,
        INVALID_TOKEN,
        EXPIRED_TOKEN,
        TOKEN_USED,
        ALREADY_VERIFIED
    }
    //#endregion
}