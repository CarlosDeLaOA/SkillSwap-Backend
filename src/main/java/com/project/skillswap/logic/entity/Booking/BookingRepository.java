package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Booking entity operations
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Finds a booking by learning session and learner
     *
     * @param learningSession the learning session
     * @param learner the learner
     * @return Optional containing the booking if found
     */
    Optional<Booking> findByLearningSessionAndLearner(LearningSession learningSession, Learner learner);

    /**
     * Finds all bookings for a specific learner
     * Useful for getting all sessions a learner is enrolled in
     *
     * @param learnerId the learner ID
     * @return List of bookings
     */
    @Query("SELECT b FROM Booking b WHERE b.learner.id = :learnerId")
    List<Booking> findByLearnerId(@Param("learnerId") Integer learnerId);

    /**
     * Finds all bookings for a specific learning session
     *
     * @param learningSessionId the learning session ID
     * @return List of bookings
     */
    @Query("SELECT b FROM Booking b WHERE b.learningSession.id = :learningSessionId")
    List<Booking> findByLearningSessionId(@Param("learningSessionId") Long learningSessionId);

    /**
     * Finds all confirmed bookings for a learner
     *
     * @param learnerId the learner ID
     * @return List of confirmed bookings
     */
    @Query("SELECT b FROM Booking b WHERE b.learner.id = :learnerId AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedBookingsByLearnerId(@Param("learnerId") Integer learnerId);

    /**
     * Finds all attended bookings for a learner
     *
     * @param learnerId the learner ID
     * @return List of attended bookings
     */
    @Query("SELECT b FROM Booking b WHERE b.learner.id = :learnerId AND b.attended = true")
    List<Booking> findAttendedBookingsByLearnerId(@Param("learnerId") Integer learnerId);
}