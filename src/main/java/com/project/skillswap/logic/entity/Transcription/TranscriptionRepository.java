package com.project.skillswap.logic.entity.Transcription;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {
    Optional<Transcription> findByLearningSession(LearningSession learningSession);
}