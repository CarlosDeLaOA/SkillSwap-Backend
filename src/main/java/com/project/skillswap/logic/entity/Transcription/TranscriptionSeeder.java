package com.project.skillswap.logic.entity.Transcription;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial transcriptions in the database
 */
@Order(5)
@Component
public class TranscriptionSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final TranscriptionRepository transcriptionRepository;
    private final LearningSessionRepository learningSessionRepository;
    //#endregion

    //#region Constructor
    /**
     * Creates a new TranscriptionSeeder instance
     *
     * @param transcriptionRepository the transcription repository
     * @param learningSessionRepository the learning session repository
     */
    public TranscriptionSeeder(
            TranscriptionRepository transcriptionRepository,
            LearningSessionRepository learningSessionRepository) {
        this.transcriptionRepository = transcriptionRepository;
        this.learningSessionRepository = learningSessionRepository;
    }
    //#endregion

    //#region Event Handling
    /**
     * Handles the application context refreshed event to seed initial data
     *
     * @param event the context refreshed event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedTranscriptions();
    }
    //#endregion

    //#region Seeding Logic
    /**
     * Seeds transcriptions into the database
     */
    private void seedTranscriptions() {
        List<TranscriptionData> transcriptionsToCreate = createTranscriptionDataList();

        for (TranscriptionData transcriptionData : transcriptionsToCreate) {
            Optional<LearningSession> session = learningSessionRepository.findById(transcriptionData.learningSessionId);

            if (session.isEmpty()) {
                continue;
            }

            Optional<Transcription> existingTranscription = transcriptionRepository.findByLearningSession(session.get());

            if (existingTranscription.isPresent()) {
                continue;
            }

            Transcription transcription = createTranscription(transcriptionData, session.get());
            transcriptionRepository.save(transcription);
        }

        System.out.println("Transcriptions seeded successfully");
    }

    /**
     * Creates a Transcription entity from TranscriptionData
     *
     * @param data the transcription data
     * @param session the learning session
     * @return the created Transcription entity
     */
    private Transcription createTranscription(TranscriptionData data, LearningSession session) {
        Transcription transcription = new Transcription();
        transcription.setLearningSession(session);
        transcription.setFullText(data.fullText);
        transcription.setDurationSeconds(data.durationSeconds);
        return transcription;
    }

    /**
     * Creates the list of transcription data to be seeded
     *
     * @return list of TranscriptionData objects
     */
    private List<TranscriptionData> createTranscriptionDataList() {
        List<TranscriptionData> transcriptions = new ArrayList<>();

        transcriptions.add(new TranscriptionData(
                5L,
                "This is a complete transcription of the Java programming session. We covered variables, data types, control structures, and object-oriented programming basics. The session was very informative and interactive.",
                3600
        ));

        transcriptions.add(new TranscriptionData(
                6L,
                "React hooks session transcription. Topics covered: useState, useEffect, useContext, custom hooks, and best practices for functional components in React applications.",
                5400
        ));

        transcriptions.add(new TranscriptionData(
                7L,
                "Machine learning fundamentals session. Discussion about supervised learning, unsupervised learning, neural networks, and practical applications of ML in real-world scenarios.",
                7200
        ));


        return transcriptions;
    }
    //#endregion

    //#region Inner Class
    /**
     * Data class holding information for creating transcriptions
     */
    private static class TranscriptionData {
        Long learningSessionId;
        String fullText;
        Integer durationSeconds;

        TranscriptionData(Long learningSessionId, String fullText, Integer durationSeconds) {
            this.learningSessionId = learningSessionId;
            this.fullText = fullText;
            this.durationSeconds = durationSeconds;
        }
    }
    //#endregion
}