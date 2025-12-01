
package com.project.skillswap.logic.entity.verification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un token de verificación de correo electrónico.
 * Los tokens tienen una validez de 24 horas desde su creación.
 */
@Table(name = "verification_token", indexes = {
        @Index(name = "idx_verification_token", columnList = "token", unique = true),
        @Index(name = "idx_verification_person", columnList = "person_id")
})
@Entity
public class VerificationToken {
    private static final Logger logger = LoggerFactory.getLogger(VerificationToken.class);

    //#region Fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    //#endregion

    //#region Constructors
    public VerificationToken() {}

    /**
     * Constructor con parámetros para crear un nuevo token de verificación.
     *
     * @param token el token único generado
     * @param person la persona asociada al token
     * @param expiresAt fecha y hora de expiración
     */
    public VerificationToken(String token, Person person, LocalDateTime expiresAt) {
        this.token = token;
        this.person = person;
        this.expiresAt = expiresAt;
    }
    //#endregion

    //#region Business Logic
    /**
     * Verifica si el token ha expirado.
     *
     * @return true si el token ha expirado, false en caso contrario
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica si el token ya fue utilizado.
     *
     * @return true si el token ya fue verificado, false en caso contrario
     */
    public boolean isVerified() {
        return verifiedAt != null;
    }
    //#endregion

    //#region Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
    //#endregion
}
