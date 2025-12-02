package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Learner.Learner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {



    Optional<Booking> findByLearningSessionAndLearner(LearningSession learningSession, Learner learner);

    List<Booking> findByLearnerId(Long learnerId);

    List<Booking> findByLearningSessionId(Long sessionId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.learningSession.id = :sessionId AND b.status = 'CONFIRMED'")
    long countConfirmedBookingsBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.learningSession.id = :sessionId " +
            "AND b.learner.id = :learnerId " +
            "AND (b.status = 'CONFIRMED' OR b.status = 'WAITING')")
    boolean existsActiveBookingBySessionAndLearner(@Param("sessionId") Long sessionId, @Param("learnerId") Long learnerId);

    /**
     * Cuenta bookings por sesión y estado
     */
    long countByLearningSessionIdAndStatus(Long sessionId, BookingStatus status);

    /**
     * Encuentra bookings por sesión y estado ordenados por fecha
     */
    @Query("SELECT b FROM Booking b WHERE b.learningSession.id = :sessionId AND b.status = :status ORDER BY b.bookingDate ASC")
    List<Booking> findByLearningSessionIdAndStatusOrderByBookingDateAsc(
            @Param("sessionId") Long sessionId,
            @Param("status") BookingStatus status
    );

    /**
     * Encuentra un booking activo (CONFIRMED o WAITING) por sesión y learner
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.learningSession.id = :sessionId " +
            "AND b.learner.id = :learnerId " +
            "AND (b.status = 'CONFIRMED' OR b.status = 'WAITING')")
    Optional<Booking> findActiveBookingBySessionAndLearner(@Param("sessionId") Long sessionId,
                                                           @Param("learnerId") Long learnerId);

    /**
     * Encuentra bookings por learner y sesión
     */
    List<Booking> findByLearnerIdAndLearningSessionId(Long learnerId, Long sessionId);

    @Query("SELECT b FROM Booking b WHERE b.learningSession.id = :sessionId AND b.community.id = :communityId AND b.status != 'CANCELLED'")
    List<Booking> findByLearningSessionIdAndCommunityId(@Param("sessionId") Long sessionId, @Param("communityId") Long communityId);

    /**
     * Cuenta los participantes confirmados de una sesión.
     *
     * @param sessionId ID de la sesión
     * @return número de participantes confirmados
     */
    @Query("""
    SELECT COUNT(b) FROM Booking b 
    WHERE b.learningSession.id = :sessionId 
    AND b.status = 'CONFIRMED'
    """)
    Integer countParticipantsBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Verifica si un estudiante participó en una sesión específica.
     *
     * @param sessionId ID de la sesión
     * @param learnerId ID del estudiante
     * @return true si el estudiante tiene un booking en la sesión
     */
    boolean existsByLearningSessionIdAndLearnerId(Long sessionId, Long learnerId);


    /**
     * Encuentra bookings activos en un rango de fechas
     */
    @Query("""
    SELECT b
    FROM Booking b
    JOIN FETCH b.learningSession ls
    JOIN FETCH b.learner l
    JOIN FETCH l.person p
    JOIN FETCH ls.skill s
    JOIN FETCH ls.instructor i
    JOIN FETCH i.person ip
    WHERE b.status = 'CONFIRMED'
    AND ls.status = 'SCHEDULED'
    AND ls.scheduledDatetime BETWEEN :startDate AND :endDate
    ORDER BY ls.scheduledDatetime ASC
""")
    List<Booking> findActiveBookingsInDateRange(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

}