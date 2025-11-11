package com.project.skillswap.logic.entity.LearningSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings " +
            "WHERE (ls.status = 'SCHEDULED' AND ls.scheduledDatetime > :currentDate) " +
            "OR (ls.status = 'ACTIVE' AND ls.scheduledDatetime >= :fiveMinutesAgo) " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findAvailableSessions(
            @Param("currentDate") Date currentDate,
            @Param("fiveMinutesAgo") Date fiveMinutesAgo
    );

    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings " +
            "WHERE ((ls.status = 'SCHEDULED' AND ls.scheduledDatetime > :currentDate) " +
            "OR (ls.status = 'ACTIVE' AND ls.scheduledDatetime >= :fiveMinutesAgo)) " +
            "AND (:categoryId IS NULL OR ka.id = :categoryId) " +
            "AND (:language IS NULL OR ls.language = :language) " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findFilteredSessions(
            @Param("currentDate") Date currentDate,
            @Param("fiveMinutesAgo") Date fiveMinutesAgo,
            @Param("categoryId") Long categoryId,
            @Param("language") String language
    );
}