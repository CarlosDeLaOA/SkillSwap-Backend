package com.project.skillswap.logic.entity.Person;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

    /**
     * Finds a person by ID with all relationships eagerly loaded.
     * Includes: userSkills, instructor, learner, and nested relationships.
     *
     * @param id the person ID to search for
     * @return an Optional containing the person with loaded relationships if found
     */
    @EntityGraph(attributePaths = {
            "userSkills",
            "userSkills.skill",
            "userSkills.skill.knowledgeArea",
            "instructor",
            "learner"
    })
    @Override
    Optional<Person> findById(Long id);
}