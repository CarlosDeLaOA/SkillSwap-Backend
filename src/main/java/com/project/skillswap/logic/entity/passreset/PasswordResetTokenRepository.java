package com.project.skillswap.logic.entity.passreset;

import com.project.skillswap.logic.entity.Person.PasswordResetToken;
import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("""
     SELECT COUNT(t) FROM PasswordResetToken t
     WHERE t.person = :person AND t.createdAt >= :since
  """)
    long countRequestsSince(@Param("person") Person person, @Param("since") Instant since);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
