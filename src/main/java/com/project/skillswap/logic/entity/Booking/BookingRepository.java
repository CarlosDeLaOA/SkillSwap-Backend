package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Learner.Learner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {



    Optional<Booking> findByLearningSessionAndLearner(LearningSession learningSession, Learner learner);

    List<Booking> findByLearnerId(Long learnerId);

    List<Booking> findByLearningSessionId(Long sessionId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.learningSession.id = :sessionId AND b.status = 'CONFIRMED'")
    long countConfirmedBookingsBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.learningSession.id = :sessionId AND b.learner.id = :learnerId AND b.status != 'CANCELLED'")
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
}
