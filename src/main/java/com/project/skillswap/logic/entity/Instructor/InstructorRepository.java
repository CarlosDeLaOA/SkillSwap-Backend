package com.project.skillswap.logic.entity.Instructor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Instructor entity operations
 */
@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Integer> {

    /**
     * Finds an instructor by person ID
     *
     * @param personId the person ID
     * @return Optional containing the instructor if found
     */
    @Query("SELECT i FROM Instructor i WHERE i.person.id = :personId")
    Optional<Instructor> findByPersonId(@Param("personId") Long personId);
}