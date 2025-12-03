package com.project.skillswap.logic.entity.auth;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "ix_prt_person_time", columnList = "person_id, created_at")
        }
)
public class PasswordResetToken {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetToken.class);

    // id uuid NOT NULL PRIMARY KEY
    // #NEW: UUID sin @GeneratedValue; lo asignamos en @PrePersist
    @Id
    @JdbcTypeCode(SqlTypes.UUID) // Hibernate 6
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    private UUID id;

    // FK person_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "person_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FKom6cmlnvcsq8653qo1nnt8cib") // opcional: coincide con tu DDL actual
    )
    private Person person;

    // token_hash varchar(64) NOT NULL (tu DDL)
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    // #NEW: columna legacy 'token' varchar(40) NOT NULL UNIQUE
    // Para no guardar el código plano, rellenamos con un random de 40 chars (hex) solo para cumplir el UNIQUE/NOT NULL.
    @Column(name = "token", nullable = false, length = 40, unique = true)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    // DDL: request_ip varchar(255), user_agent varchar(255)
    @Column(name = "request_ip", length = 255)
    private String requestIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // ===== Lifecycle =====
    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();              // #NEW
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.token == null || this.token.isBlank()) this.token = randomHex40(); // #NEW
    }

    // ===== Helpers =====
    @Transient
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public void markUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }

    public void registerFailedAttempt() {
        this.attempts++;
    }

    public void markVerified() {
        this.verifiedAt = Instant.now();
    }

    // #NEW: generador para 'token' legacy (40 chars hex aleatorios)
    private String randomHex40() {
        // 20 bytes -> 40 chars hex
        byte[] bytes = new byte[20];
        new java.security.SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(40);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ===== Getters/Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getToken() { return token; }              // legacy
    public void setToken(String token) { this.token = token; } // legacy

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public String getRequestIp() { return requestIp; }
    public void setRequestIp(String requestIp) { this.requestIp = requestIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
