package com.project.skillswap.logic.entity.Person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    boolean existsByTokenHash(String tokenHash);
    long countByPersonAndCreatedAtAfter(Person person, Instant since);

    @Query("""
        select count(t) from PasswordResetToken t
        where t.person = :person and t.createdAt >= :since
    """)
    long countRequestsSince(@Param("person") Person person, @Param("since") Instant since);
}
