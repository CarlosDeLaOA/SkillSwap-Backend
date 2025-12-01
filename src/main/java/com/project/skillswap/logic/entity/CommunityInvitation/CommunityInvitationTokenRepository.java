package com.project.skillswap.logic.entity.CommunityInvitation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar tokens de invitación a comunidades.
 */
@Repository
public interface CommunityInvitationTokenRepository extends JpaRepository<CommunityInvitationToken, Long> {

    /**
     * Busca un token de invitación por su valor.
     *
     * @param token el token a buscar
     * @return Optional con el token si existe
     */
    Optional<CommunityInvitationToken> findByToken(String token);

    /**
     * Busca tokens activos (no aceptados y no expirados) para un email específico en una comunidad.
     *
     * @param communityId ID de la comunidad
     * @param inviteeEmail email del invitado
     * @param now fecha y hora actual para comparar expiración
     * @return Optional con el token si existe
     */
    Optional<CommunityInvitationToken> findByCommunityIdAndInviteeEmailAndAcceptedAtIsNullAndExpiresAtAfterAndActiveTrue(
            Long communityId, String inviteeEmail, LocalDateTime now);

    /**
     * Busca todas las invitaciones de una comunidad.
     *
     * @param communityId ID de la comunidad
     * @return lista de tokens
     */
    List<CommunityInvitationToken> findByCommunityId(Long communityId);

    /**
     * Busca invitaciones pendientes de un email.
     *
     * @param inviteeEmail email del invitado
     * @param now fecha y hora actual
     * @return lista de tokens pendientes
     */
    List<CommunityInvitationToken> findByInviteeEmailAndAcceptedAtIsNullAndExpiresAtAfterAndActiveTrue(
            String inviteeEmail, LocalDateTime now);

    /**
     * Elimina todos los tokens expirados.
     *
     * @param now fecha y hora actual para comparar expiración
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}