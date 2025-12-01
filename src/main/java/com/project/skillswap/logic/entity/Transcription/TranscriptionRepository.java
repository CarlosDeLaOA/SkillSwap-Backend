package com.project.skillswap.logic.entity.Transcription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {
    Optional<Transcription> findByLearningSession(LearningSession learningSession);
}