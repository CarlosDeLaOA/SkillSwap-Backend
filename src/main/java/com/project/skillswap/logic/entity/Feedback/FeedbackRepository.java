package com.project.skillswap.logic.entity.Feedback;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Learner.Learner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /**
     * Busca feedback por objetos completos
     */
    Optional<Feedback> findByLearningSessionAndLearner(LearningSession learningSession, Learner learner);

    /**
     * Busca todos los feedbacks de una sesión por ID
     */
    List<Feedback> findByLearningSessionId(Long sessionId);
}