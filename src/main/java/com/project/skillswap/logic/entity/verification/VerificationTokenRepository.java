package com.project.skillswap.logic.entity.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio para gestionar tokens de verificación de correo electrónico.
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * Busca un token de verificación por su valor.
     *
     * @param token el token a buscar
     * @return Optional con el token si existe
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Busca un token activo (no verificado y no expirado) para una persona específica.
     *
     * @param personId ID de la persona
     * @param now fecha y hora actual para comparar expiración
     * @return Optional con el token si existe
     */
    Optional<VerificationToken> findByPersonIdAndVerifiedAtIsNullAndExpiresAtAfter(Long personId, LocalDateTime now);

    /**
     * Elimina todos los tokens expirados.
     *
     * @param now fecha y hora actual para comparar expiración
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}
