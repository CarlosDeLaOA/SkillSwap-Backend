package com.project.skillswap.logic.entity.LearningSession;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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

    //<editor-fold desc="Instructor Session Management">
    /**
     * Lista todas las sesiones de un instructor con filtro por estado
     *
     * @param instructorId ID del instructor
     * @param status Estado de la sesión (null para traer todos)
     * @param pageable Configuración de paginación
     * @return Página de sesiones
     */
    @Query("""
    SELECT new com.project.skillswap.logic.entity.LearningSession.SessionListResponse(
        ls.id,
        ls.title,
        ls.description,
        ls.scheduledDatetime,
        ls.durationMinutes,
        CAST(ls.status AS string),
        ls.videoCallLink,
        s.name,
        ls.maxCapacity,
        COUNT(b.id),
        ls.isPremium,
        ls.creationDate
    )
    FROM LearningSession ls
    INNER JOIN ls.skill s
    INNER JOIN ls.instructor i
    LEFT JOIN Booking b ON b.learningSession.id = ls.id AND b.status = 'CONFIRMED'
    WHERE i.id = :instructorId
    AND (:status IS NULL OR ls.status = :status)
    GROUP BY ls.id, ls.title, ls.description, ls.scheduledDatetime, 
             ls.durationMinutes, ls.status, ls.videoCallLink, s.name, 
             ls.maxCapacity, ls.isPremium, ls.creationDate
    ORDER BY ls.scheduledDatetime DESC
""")
    Page<SessionListResponse> findInstructorSessions(
            @Param("instructorId") Long instructorId,
            @Param("status") SessionStatus status,
            Pageable pageable
    );

    /**
     * Busca una sesión por ID y verifica que pertenezca al instructor
     *
     * @param sessionId ID de la sesión
     * @param instructorId ID del instructor
     * @return Optional con la sesión si existe y pertenece al instructor
     */
    @Query("""
        SELECT ls FROM LearningSession ls
        WHERE ls.id = :sessionId
        AND ls.instructor.id = :instructorId
    """)
    Optional<LearningSession> findByIdAndInstructor(
            @Param("sessionId") Long sessionId,
            @Param("instructorId") Long instructorId
    );


    /**
     * Obtiene el historial de sesiones para un SkillSeeker (estudiante).
     * Retorna sesiones completadas o canceladas en las que el estudiante participó.
     *
     * @param learnerId ID del estudiante
     * @param pageable configuración de paginación
     * @return página de sesiones históricas
     */
    @Query("""
    SELECT DISTINCT ls FROM LearningSession ls
    INNER JOIN Booking b ON b.learningSession.id = ls.id
    WHERE b.learner.id = :learnerId
    AND ls.status IN ('COMPLETED', 'CANCELLED')
    ORDER BY ls.scheduledDatetime DESC
    """)
    Page<LearningSession> findHistoricalSessionsByLearnerId(
            @Param("learnerId") Long learnerId,
            Pageable pageable
    );
    //</editor-fold>
}