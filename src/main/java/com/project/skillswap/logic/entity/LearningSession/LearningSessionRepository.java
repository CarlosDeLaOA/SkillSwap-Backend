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

    /**
     * Busca sesiones programadas en un rango de fechas.
     * Incluye fetch de instructor, person, skill, knowledgeArea y bookings.
     *
     * Usado por SessionReminderService para encontrar sesiones que comienzan
     * dentro de una ventana determinada (por ejemplo 24 horas ± ventana).
     */
    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings b " +
            "WHERE (ls.status = 'SCHEDULED' OR ls.status = 'ACTIVE') " +
            "AND ls.scheduledDatetime BETWEEN :start AND :end " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findScheduledSessionsInDateRange(
            @Param("start") Date start,
            @Param("end") Date end
    );
    //</editor-fold>

    //<editor-fold desc="Conflict detection & Instructor sessions">
    /**
     * Busca sesiones del instructor que conflijan/solapen con el intervalo [start, end].
     *
     * Nota: esta consulta está escrita como nativeQuery usando la sintaxis de MySQL:
     *   (scheduled_datetime + INTERVAL duration_minutes MINUTE)
     *
     * Si usas PostgreSQL, H2 u otra BD, reemplaza la expresión por la sintaxis adecuada
     * (ej. en Postgres: scheduled_datetime + (duration_minutes || ' minutes')::interval).
     */
    @Query(value = "SELECT * FROM learning_session ls " +
            "WHERE ls.instructor_id = :instructorId " +
            "AND ls.status IN ('SCHEDULED','ACTIVE') " +
            "AND ls.scheduled_datetime < :end " +
            "AND (ls.scheduled_datetime + INTERVAL ls.duration_minutes MINUTE) > :start",
            nativeQuery = true)
    List<LearningSession> findConflictingSessions(
            @Param("instructorId") Long instructorId,
            @Param("start") Date start,
            @Param("end") Date end
    );

    /**
     * Obtiene todas las sesiones de un instructor dentro de un rango (fetch para evitar lazy loading).
     * Usado por el generador de sugerencias.
     */
    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings b " +
            "WHERE i.id = :instructorId " +
            "AND ls.scheduledDatetime BETWEEN :start AND :end " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findInstructorScheduledSessions(
            @Param("instructorId") Long instructorId,
            @Param("start") Date start,
            @Param("end") Date end
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
    //</editor-fold>

    //<editor-fold desc="Added for SessionSuggestionService compatibility">
    /**
     * Busca sesiones por una lista de estados e incluye fetch para evitar lazy loading.
     * Añadido para compatibilidad con SessionSuggestionService. ***
     */
    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings b " +
            "WHERE ls.status IN :statuses " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findByStatusIn(@Param("statuses") List<SessionStatus> statuses); // ***

    /**
     * Obtiene sesiones por instructorId (sin range). Añadido para compatibilidad con SessionSuggestionService. ***
     */
    @Query("SELECT DISTINCT ls FROM LearningSession ls " +
            "LEFT JOIN FETCH ls.instructor i " +
            "LEFT JOIN FETCH i.person p " +
            "LEFT JOIN FETCH ls.skill s " +
            "LEFT JOIN FETCH s.knowledgeArea ka " +
            "LEFT JOIN FETCH ls.bookings b " +
            "WHERE i.id = :instructorId " +
            "ORDER BY ls.scheduledDatetime ASC")
    List<LearningSession> findByInstructorId(@Param("instructorId") Long instructorId); // ***
    //</editor-fold>
}