package com.project.skillswap.logic.entity.CommunityInvitation;

import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un token de invitación a una comunidad de aprendizaje.
 * Los tokens tienen una validez de 48 horas desde su creación.
 */
@Table(name = "community_invitation_token", indexes = {
        @Index(name = "idx_invitation_token", columnList = "token", unique = true),
        @Index(name = "idx_invitation_email", columnList = "invitee_email"),
        @Index(name = "idx_invitation_community", columnList = "community_id")
})
@Entity
public class CommunityInvitationToken {

    //#region Fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private LearningCommunity community;

    @Column(name = "invitee_email", nullable = false, length = 255)
    private String inviteeEmail;

    @Column(name = "invitee_name", length = 150)
    private String inviteeName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "active")
    private Boolean active = true;
    //#endregion

    //#region Constructors
    public CommunityInvitationToken() {}

    /**
     * Constructor con parámetros para crear un nuevo token de invitación.
     *
     * @param token el token único generado
     * @param community la comunidad a la que se invita
     * @param inviteeEmail email del invitado
     * @param inviteeName nombre del invitado
     * @param expiresAt fecha y hora de expiración
     */
    public CommunityInvitationToken(String token, LearningCommunity community, String inviteeEmail,
                                    String inviteeName, LocalDateTime expiresAt) {
        this.token = token;
        this.community = community;
        this.inviteeEmail = inviteeEmail;
        this.inviteeName = inviteeName;
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
     * Verifica si el token ya fue aceptado.
     *
     * @return true si el token ya fue aceptado, false en caso contrario
     */
    public boolean isAccepted() {
        return acceptedAt != null;
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

    public LearningCommunity getCommunity() {
        return community;
    }

    public void setCommunity(LearningCommunity community) {
        this.community = community;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public String getInviteeName() {
        return inviteeName;
    }

    public void setInviteeName(String inviteeName) {
        this.inviteeName = inviteeName;
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

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
    //#endregion
}