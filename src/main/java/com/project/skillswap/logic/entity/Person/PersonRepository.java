package com.project.skillswap.logic.entity.Person;

import org.springframework.data.jpa.repository.JpaRepository;
import com.project.skillswap.logic.entity.Person.Person;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    /**
     * Finds a person by their email address.
     *
     * @param email the email to search for
     * @return an Optional containing the person if found
     */
    Optional<Person> findByEmail(String email);

    /**
     * Finds a person by their Google OAuth ID.
     *
     * @param googleOauthId the Google OAuth ID to search for
     * @return an Optional containing the person if found
     */
    Optional<Person> findByGoogleOauthId(String googleOauthId);

    // ===== Added for password reset & case-insensitive lookups =====

    /**
     * Finds a person by their email address (case-insensitive).
     *
     * @param email the email to search for (case-insensitive)
     * @return an Optional containing the person if found
     */
    Optional<Person> findByEmailIgnoreCase(String email);

    /**
     * Checks if a person exists with the given email address (case-insensitive).
     *
     * @param email the email to check
     * @return true if a person exists with that email, false otherwise
     */
    boolean existsByEmailIgnoreCase(String email);
}
