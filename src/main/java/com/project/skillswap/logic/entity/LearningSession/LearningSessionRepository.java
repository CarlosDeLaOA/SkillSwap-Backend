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

    //<editor-fold desc="Schedule Conflict Queries">
    /// *** CRITERIO 1 & 2: Queries para detectar conflictos y sugerir horarios
    /**
     * Busca todas las sesiones de un instructor en un rango de tiempo específico
     * Utilizado para detectar conflictos de horario
     * CRITERIO 1: Validar disponibilidad del SkillSwapper
     */
    @Query("SELECT ls FROM LearningSession ls " +
            "WHERE ls.instructor.id = :instructorId " +
            "AND ls.status IN ('SCHEDULED', 'ACTIVE') " +
            "AND ls.scheduledDatetime < :endTime " +
            "AND DATE_ADD(ls.scheduledDatetime, INTERVAL ls.durationMinutes MINUTE) > :startTime")
    List<LearningSession> findConflictingSessions(
            @Param("instructorId") Long instructorId,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );

    /**
     * Busca todas las sesiones ocupadas de un instructor en un rango de fechas
     * Utilizado para sugerir horarios alternativos
     * CRITERIO 1: Sugerir horarios alternos
     */
    @Query("SELECT ls FROM LearningSession ls " +
            "WHERE ls.instructor.id = :instructorId " +
            "AND ls.status IN ('SCHEDULED', 'ACTIVE') " +
            "AND ls.scheduledDatetime >= :startDate " +
            "AND ls.scheduledDatetime <= :endDate " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findInstructorScheduledSessions(
            @Param("instructorId") Long instructorId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
    //</editor-fold>

    //<editor-fold desc="Reminder Queries">
    /// *** CRITERIO 6: Queries para recordatorios automáticos
    /**
     * Busca sesiones programadas dentro de un rango de fechas
     * Utilizado para encontrar sesiones próximas a 24 horas
     * CRITERIO 6: Recordatorio automático 24 horas antes vía email
     */
    @Query("SELECT ls FROM LearningSession ls " +
            "WHERE ls.status IN ('SCHEDULED', 'ACTIVE') " +
            "AND ls.scheduledDatetime >= :startDate " +
            "AND ls.scheduledDatetime <= :endDate " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findScheduledSessionsInDateRange(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
    //</editor-fold>
}