package com.project.skillswap.logic.entity.Feedback;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial feedbacks in the database
 */
@Order(8)
@Component
public class FeedbackSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final FeedbackRepository feedbackRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final LearnerRepository learnerRepository;
    //#endregion

    //#region Constructor
    public FeedbackSeeder(
            FeedbackRepository feedbackRepository,
            LearningSessionRepository learningSessionRepository,
            LearnerRepository learnerRepository) {
        this.feedbackRepository = feedbackRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.learnerRepository = learnerRepository;
    }
    //#endregion

    //#region Event Handling
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedFeedbacks();
    }
    //#endregion

    //#region Seeding Logic
    private void seedFeedbacks() {
        List<FeedbackData> feedbacksToCreate = createFeedbackDataList();

        for (FeedbackData feedbackData : feedbacksToCreate) {
            Optional<LearningSession> session = learningSessionRepository.findById(feedbackData.learningSessionId);
            Optional<Learner> learner = learnerRepository.findById(feedbackData.learnerId.intValue());

            if (session.isEmpty() || learner.isEmpty()) {
                continue;
            }

            Optional<Feedback> existingFeedback = feedbackRepository.findByLearningSessionAndLearner(
                    session.get(), learner.get()
            );

            if (existingFeedback.isPresent()) {
                continue;
            }

            Feedback feedback = createFeedback(feedbackData, session.get(), learner.get());
            feedbackRepository.save(feedback);
        }

        System.out.println("✅ Feedbacks seeded successfully");
    }

    private Feedback createFeedback(FeedbackData data, LearningSession session, Learner learner) {
        Feedback feedback = new Feedback();
        feedback.setLearningSession(session);
        feedback.setLearner(learner);
        feedback.setRating(data.rating);
        feedback.setComment(data.comment);
        return feedback;
    }

    /**
     * Creates the list of feedback data to be seeded
     */
    private List<FeedbackData> createFeedbackDataList() {
        List<FeedbackData> feedbacks = new ArrayList<>();

        // Feedbacks genéricos existentes
        feedbacks.add(new FeedbackData(5L, 2L, 5, "Excellent session! Very clear explanation and great examples."));
        feedbacks.add(new FeedbackData(6L, 5L, 5, "Amazing instructor! Learned a lot about React hooks."));
        feedbacks.add(new FeedbackData(7L, 2L, 4, "Good content, would like more practical exercises next time."));
        feedbacks.add(new FeedbackData(5L, 8L, 5, "Perfect pace and very informative. Highly recommend!"));
        feedbacks.add(new FeedbackData(6L, 12L, 4, "Great session but could use more interactive elements."));
        feedbacks.add(new FeedbackData(7L, 5L, 5, "Outstanding! The best ML introduction I've attended."));
        feedbacks.add(new FeedbackData(5L, 5L, 4, "Very knowledgeable instructor. Some topics went a bit fast."));
        feedbacks.add(new FeedbackData(6L, 2L, 5, "Fantastic session! Clear explanations and answered all questions."));


        return feedbacks;
    }
    //#endregion

    //#region Inner Class
    private static class FeedbackData {
        Long learningSessionId;
        Long learnerId;
        Integer rating;
        String comment;

        FeedbackData(Long learningSessionId, Long learnerId, Integer rating, String comment) {
            this.learningSessionId = learningSessionId;
            this.learnerId = learnerId;
            this.rating = rating;
            this.comment = comment;
        }
    }
    //#endregion
}
