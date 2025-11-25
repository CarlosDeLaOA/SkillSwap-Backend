package com.project.skillswap.logic.entity.PassReset;

import com.project.skillswap.logic.entity.auth.PasswordResetToken;
import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, java.util.UUID> {

    // Último token creado (para cooldown)
    Optional<PasswordResetToken> findTopByPersonOrderByCreatedAtDesc(Person person);

    // Tokens activos (no usados y no expirados)
    List<PasswordResetToken> findByPersonAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            Person person, Instant now);

    // Cuántas solicitudes recientes (para rate limit de request)
    @Query("select count(t) from PasswordResetToken t " +
            "where t.person = :person and t.createdAt >= :since")
    long countRequestsSince(@Param("person") Person person, @Param("since") Instant since);

    // Marcar usados todos los tokens activos (defensa lateral)
    @Modifying
    @Query("update PasswordResetToken t set t.used = true, t.usedAt = :now " +
            "where t.person = :person and t.used = false")
    int consumeAllActive(@Param("person") Person person, @Param("now") Instant now);
}
