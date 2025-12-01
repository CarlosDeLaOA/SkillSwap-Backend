package com.project.skillswap.logic.entity.Feedback;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Test Seeder para crear feedbacks de prueba
 * Instructor ID: 17 (Mia Morales - moralescamila500@gmail.com)
 * Sesiones FINISHED: 3053, 3105, 3157, 3209, 3264
 * Learners: 1, 2, 3, 4, 5
 */
@Order(9)
@Component
public class TestFeedbackSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final FeedbackRepository feedbackRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final LearnerRepository learnerRepository;
    private final InstructorRepository instructorRepository;
    //#endregion

    //#region Constructor
    public TestFeedbackSeeder(
            FeedbackRepository feedbackRepository,
            LearningSessionRepository learningSessionRepository,
            LearnerRepository learnerRepository,
            InstructorRepository instructorRepository) {
        this.feedbackRepository = feedbackRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.learnerRepository = learnerRepository;
        this.instructorRepository = instructorRepository;
    }
    //#endregion

    //#region Event Handling
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedTestFeedbacks();
    }
    //#endregion

    //#region Seeding Logic
    private void seedTestFeedbacks() {
        // Verificar que el instructor (ID 17) exista
        Optional<Instructor> instructorOpt = instructorRepository.findById(17);
        if (instructorOpt.isEmpty()) {
            System.out.println("[TestFeedbackSeeder] Instructor ID 17 no encontrado.Saltando seed.");
            return;
        }

        System.out.println("[TestFeedbackSeeder]  Iniciando seed de feedbacks para Instructor 17 (Mia Morales)...");

        List<TestFeedbackData> feedbacksToCreate = createTestFeedbackDataList();

        int createdCount = 0;
        int skippedCount = 0;

        for (TestFeedbackData feedbackData : feedbacksToCreate) {
            Optional<LearningSession> session = learningSessionRepository.findById(feedbackData.learningSessionId);
            Optional<Learner> learner = learnerRepository.findById(feedbackData.learnerId);

            if (session.isEmpty() || learner.isEmpty()) {
                System.out.println("[TestFeedbackSeeder]   Sesión " + feedbackData.learningSessionId + " o Learner " + feedbackData.learnerId + " no encontrados.Saltando.");
                skippedCount++;
                continue;
            }

            Optional<Feedback> existingFeedback = feedbackRepository.findByLearningSessionAndLearner(
                    session.get(), learner.get()
            );

            if (existingFeedback.isPresent()) {
                System.out.println("[TestFeedbackSeeder] ️  Feedback ya existe para sesión " + feedbackData.learningSessionId + " y learner " + feedbackData.learnerId + ".Saltando.");
                skippedCount++;
                continue;
            }

            try {
                Feedback feedback = createTestFeedback(feedbackData, session.get(), learner.get());
                feedbackRepository.save(feedback);
                System.out.println("[TestFeedbackSeeder]  Feedback creado - Sesión: " + feedbackData.learningSessionId + ", Learner: " + feedbackData.learnerId + ", Rating: " + feedbackData.rating);
                createdCount++;
            } catch (Exception e) {
                System.out.println("[TestFeedbackSeeder]  Error al crear feedback: " + e.getMessage());
                skippedCount++;
            }
        }

        System.out.println("[TestFeedbackSeeder] Seed completado.Creados: " + createdCount + ", Saltados: " + skippedCount);
    }

    private Feedback createTestFeedback(TestFeedbackData data, LearningSession session, Learner learner) {
        Feedback feedback = new Feedback();
        feedback.setLearningSession(session);
        feedback.setLearner(learner);
        feedback.setRating(data.rating);
        feedback.setComment(data.comment);
        feedback.setAudioUrl(data.audioUrl);
        feedback.setAudioTranscription(data.audioTranscription);
        return feedback;
    }

    /**
     * Crea lista de feedbacks de prueba usando las sesiones reales que existen
     * Instructor ID: 17 (Mia Morales)
     * Sesiones FINISHED: 3053, 3105, 3157, 3209, 3264
     * Learners: 1, 2, 3, 4, 5
     */
    private List<TestFeedbackData> createTestFeedbackDataList() {
        List<TestFeedbackData> feedbacks = new ArrayList<>();

        // Sesión 3053: Álgebra Lineal para ML
        feedbacks.add(new TestFeedbackData(
                3053L, 1L, 5,
                "Excelente clase de álgebra lineal, muy clara y aplicada a Machine Learning.",
                "https://example.com/audio/feedback_3053_1.mp3",
                "Excellent linear algebra class, very clear and applied to Machine Learning."
        ));
        feedbacks.add(new TestFeedbackData(
                3053L, 2L, 5,
                "Perfecta.La instructora explica conceptos complejos de forma entendible.",
                "https://example.com/audio/feedback_3053_2.mp3",
                "Perfect.The instructor explains complex concepts in an understandable way."
        ));
        feedbacks.add(new TestFeedbackData(
                3053L, 3L, 4,
                "Muy buena clase, aunque algunos ejercicios fueron complicados.",
                "https://example.com/audio/feedback_3053_3.mp3",
                "Very good class, although some exercises were challenging."
        ));

        // Sesión 3105: Álgebra Lineal para ML
        feedbacks.add(new TestFeedbackData(
                3105L, 1L, 5,
                "Magistral.La mejor explicación de matrices que he recibido.",
                "https://example.com/audio/feedback_3105_1.mp3",
                "Masterclass.The best explanation of matrices I have ever received."
        ));
        feedbacks.add(new TestFeedbackData(
                3105L, 2L, 5,
                "Increíble, la instructora sabe mucho del tema y lo transmite muy bien.",
                "https://example.com/audio/feedback_3105_2.mp3",
                "Incredible, the instructor knows a lot about the subject and communicates it very well."
        ));

        // Sesión 3157: Álgebra Lineal para ML
        feedbacks.add(new TestFeedbackData(
                3157L, 1L, 5,
                "Excelente para aprender álgebra lineal aplicada a ML.",
                "https://example.com/audio/feedback_3157_1.mp3",
                "Excellent for learning linear algebra applied to ML."
        ));
        feedbacks.add(new TestFeedbackData(
                3157L, 3L, 5,
                "La mejor clase de matemáticas para ML que he tomado.",
                "https://example.com/audio/feedback_3157_3.mp3",
                "The best mathematics class for ML I have ever taken."
        ));

        // Sesión 3209: Álgebra Lineal para ML
        feedbacks.add(new TestFeedbackData(
                3209L, 2L, 5,
                "Excelente contenido y muy bien estructurado.",
                "https://example.com/audio/feedback_3209_2.mp3",
                "Excellent content and very well structured."
        ));
        feedbacks.add(new TestFeedbackData(
                3209L, 4L, 4,
                "Muy útil, me ayudó a entender los conceptos fundamentales.",
                "https://example.com/audio/feedback_3209_4.mp3",
                "Very useful, it helped me understand the fundamental concepts."
        ));

        // Sesión 3264: Álgebra Lineal para ML
        feedbacks.add(new TestFeedbackData(
                3264L, 3L, 5,
                "Recomendado 100%.Cambió mi comprensión del álgebra lineal.",
                "https://example.com/audio/feedback_3264_3.mp3",
                "Highly recommended.It changed my understanding of linear algebra."
        ));
        feedbacks.add(new TestFeedbackData(
                3264L, 5L, 5,
                "Perfecto, la instructora sabe explicar de forma clara y concisa.",
                "https://example.com/audio/feedback_3264_5.mp3",
                "Perfect, the instructor knows how to explain clearly and concisely."
        ));

        return feedbacks;
    }
    //#endregion

    //#region Inner Class
    private static class TestFeedbackData {
        Long learningSessionId;
        Long learnerId;
        Integer rating;
        String comment;
        String audioUrl;
        String audioTranscription;

        TestFeedbackData(Long learningSessionId, Long learnerId, Integer rating, String comment, String audioUrl, String audioTranscription) {
            this.learningSessionId = learningSessionId;
            this.learnerId = learnerId;
            this.rating = rating;
            this.comment = comment;
            this.audioUrl = audioUrl;
            this.audioTranscription = audioTranscription;
        }
    }
    //#endregion
}