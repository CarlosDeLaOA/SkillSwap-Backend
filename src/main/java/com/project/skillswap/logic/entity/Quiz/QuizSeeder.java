package com.project.skillswap.logic.entity.Quiz;

import jakarta.transaction.Transactional;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
@Order(9)
@Component
public class QuizSeeder {

    private static final Logger logger = LoggerFactory.getLogger(QuizSeeder.class);

    private final QuizRepository quizRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final LearnerRepository learnerRepository;
    private final BookingRepository bookingRepository;
    private final Random random = new Random();

    public QuizSeeder(QuizRepository quizRepository,
                      LearningSessionRepository learningSessionRepository,
                      LearnerRepository learnerRepository,
                      BookingRepository bookingRepository) {
        this.quizRepository = quizRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.learnerRepository = learnerRepository;
        this.bookingRepository = bookingRepository;
    }

    @TransactionalEventListener
    @Order(9)
    public void seedQuizzesAfterTransaction(ContextRefreshedEvent event) {
        if (quizRepository.count() > 0) {
            logger.info("QuizSeeder: Ya existen quizzes, omitiendo seed");
            return;
        }

        List<Learner> learners = learnerRepository.findAll();
        if (learners.isEmpty()) {
            logger.warn("No hay learners para crear quizzes");
            return;
        }

        int totalQuizzes = 0;

        for (Learner learner : learners) {
            List<Booking> bookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getLearner().getId().equals(learner.getId()))
                    .filter(b -> b.getAttended() != null && b.getAttended())
                    .toList();

            List<Booking> attendedFinishedBookings = new ArrayList<>();
            for (Booking booking : bookings) {
                // Cargar sesión explícitamente dentro de la transacción
                LearningSession session = learningSessionRepository
                        .findById(booking.getLearningSession().getId())
                        .orElseThrow();

                if (session.getStatus() == SessionStatus.FINISHED) {
                    attendedFinishedBookings.add(booking);
                }
            }

            for (Booking booking : attendedFinishedBookings) {
                Quiz quiz = createQuiz(learner, learningSessionRepository
                        .findById(booking.getLearningSession().getId())
                        .orElseThrow());
                quizRepository.save(quiz);
                totalQuizzes++;
            }
        }

        logger.info("QuizSeeder: " + totalQuizzes + " quizzes creados");
    }

    private Quiz createQuiz(Learner learner, LearningSession session) {
        Quiz quiz = new Quiz();
        quiz.setLearner(learner);
        quiz.setLearningSession(session);

        int scoreObtained = 70 + random.nextInt(31);
        quiz.setScoreObtained(scoreObtained);
        quiz.setPassed(scoreObtained >= 70);

        Calendar completionCal = Calendar.getInstance();
        completionCal.setTime(session.getScheduledDatetime());
        completionCal.add(Calendar.DAY_OF_MONTH, random.nextInt(3));
        completionCal.add(Calendar.HOUR_OF_DAY, random.nextInt(24));
        quiz.setCompletionDate(completionCal.getTime());

        return quiz;
    }
}
