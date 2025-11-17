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

    @Query("SELECT b FROM Booking b WHERE b.learningSession.id = :sessionId AND b.community.id = :communityId AND b.status != 'CANCELLED'")
    List<Booking> findByLearningSessionIdAndCommunityId(@Param("sessionId") Long sessionId, @Param("communityId") Long communityId);
}