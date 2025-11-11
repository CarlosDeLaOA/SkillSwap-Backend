package com.project.skillswap.logic.entity.LearningSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    //<editor-fold desc="Available Sessions Queries">
    /**
     * Obtiene todas las sesiones que están programadas (SCHEDULED)
     * o que están activas y comenzaron hace menos de 5 minutos
     * INCLUYE: instructor, person, skill, knowledgeArea y bookings
     */
    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings b " +
            "WHERE (ls.status = 'SCHEDULED' AND ls.scheduledDatetime > :currentDate) " +
            "OR (ls.status = 'ACTIVE' AND ls.scheduledDatetime >= :fiveMinutesAgo) " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findAvailableSessions(
            @Param("currentDate") Date currentDate,
            @Param("fiveMinutesAgo") Date fiveMinutesAgo
    );
    //</editor-fold>

    //<editor-fold desc="Filtered Sessions Queries">
    /**
     * Filtra sesiones disponibles por categoría (KnowledgeArea)
     */
    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings b " +
            "WHERE ((ls.status = 'SCHEDULED' AND ls.scheduledDatetime > :currentDate) " +
            "OR (ls.status = 'ACTIVE' AND ls.scheduledDatetime >= :fiveMinutesAgo)) " +
            "AND ka.id = :categoryId " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findSessionsByCategory(
            @Param("currentDate") Date currentDate,
            @Param("fiveMinutesAgo") Date fiveMinutesAgo,
            @Param("categoryId") Long categoryId
    );

    /**
     * Filtra sesiones disponibles por idioma
     */
    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings b " +
            "WHERE ((ls.status = 'SCHEDULED' AND ls.scheduledDatetime > :currentDate) " +
            "OR (ls.status = 'ACTIVE' AND ls.scheduledDatetime >= :fiveMinutesAgo)) " +
            "AND ls.language = :language " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findSessionsByLanguage(
            @Param("currentDate") Date currentDate,
            @Param("fiveMinutesAgo") Date fiveMinutesAgo,
            @Param("language") String language
    );

    /**
     * Filtra sesiones disponibles por categoría Y idioma
     */
    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings b " +
            "WHERE ((ls.status = 'SCHEDULED' AND ls.scheduledDatetime > :currentDate) " +
            "OR (ls.status = 'ACTIVE' AND ls.scheduledDatetime >= :fiveMinutesAgo)) " +
            "AND ka.id = :categoryId " +
            "AND ls.language = :language " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findSessionsByCategoryAndLanguage(
            @Param("currentDate") Date currentDate,
            @Param("fiveMinutesAgo") Date fiveMinutesAgo,
            @Param("categoryId") Long categoryId,
            @Param("language") String language
    );
    //</editor-fold>
}