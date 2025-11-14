package com.project.skillswap.logic.entity.Learner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Learner entity operations
 */
@Repository
public interface LearnerRepository extends JpaRepository<Learner, Integer> {

    /**
     * Finds a learner by person ID
     *
     * @param personId the person ID
     * @return Optional containing the learner if found
     */
    @Query("SELECT l FROM Learner l WHERE l.person.id = :personId")
    Optional<Learner> findByPersonId(@Param("personId") Long personId);
}