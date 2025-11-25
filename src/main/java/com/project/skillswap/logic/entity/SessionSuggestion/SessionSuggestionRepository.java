package com.project.skillswap.logic.entity.SessionSuggestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/*
 Repositorio para gestionar SessionSuggestion en la base de datos. ***
*/
@Repository
public interface SessionSuggestionRepository extends JpaRepository<SessionSuggestion, Long> {

    @Query("SELECT ss FROM SessionSuggestion ss " +
            "WHERE ss.person.id = :personId " +
            "AND ss.viewed = false " +
            "ORDER BY ss.matchScore DESC")
    List<SessionSuggestion> findTop5UnviewedSuggestions(@Param("personId") Long personId);

    List<SessionSuggestion> findByPersonId(Long personId);

    Optional<SessionSuggestion> findByPersonIdAndLearningSessionId(
            @Param("personId") Long personId,
            @Param("sessionId") Long sessionId);

    long countByPersonIdAndViewed(Long personId, Boolean viewed);
}