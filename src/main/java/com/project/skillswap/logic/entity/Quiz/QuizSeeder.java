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

import java.util.*;

@Order(9)
@Component
public class QuizSeeder implements ApplicationListener<ContextRefreshedEvent> {
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

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (quizRepository.count() > 0) {
            logger.info("QuizSeeder: Ya existen quizzes, omitiendo seed");
            return;
        }
        this.seedQuizzes();
    }

    private void seedQuizzes() {
        List<Learner> learners = learnerRepository.findAll();
        List<LearningSession> allFinishedSessions = learningSessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.FINISHED)
                .toList();

        if (learners.isEmpty()) {
            logger.warn("No hay learners para crear quizzes");
            return;
        }

        if (allFinishedSessions.isEmpty()) {
            logger.warn("No hay sesiones finalizadas para crear quizzes");
            return;
        }

        int totalQuizzes = 0;

        for (Learner learner : learners) {
            int quizzesCreated = 0;
            int minQuizzesNeeded = learner.getCredentialsObtained(); // Mínimo 60

            // ESTRATEGIA 1: Crear quizzes basados en bookings atendidos
            List<Booking> attendedBookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getLearner().getId().equals(learner.getId()))
                    .filter(b -> b.getAttended() != null && b.getAttended())
                    .toList();

            for (Booking booking : attendedBookings) {
                LearningSession session = booking.getLearningSession();
                // Verificar que la sesión esté FINISHED
                if (session.getStatus() == SessionStatus.FINISHED) {
                    Quiz quiz = createQuiz(learner, session);
                    quizRepository.save(quiz);
                    quizzesCreated++;
                    totalQuizzes++;
                }

                // Si ya alcanzamos el mínimo, salir del loop
                if (quizzesCreated >= minQuizzesNeeded) {
                    break;
                }
            }

            // ESTRATEGIA 2: Si aún no alcanza el mínimo, crear quizzes adicionales
            if (quizzesCreated < minQuizzesNeeded) {
                int additionalNeeded = minQuizzesNeeded - quizzesCreated;
                logger.info("Learner " + learner.getId() + " (" + learner.getPerson().getFullName() +
                        ") necesita " + additionalNeeded + " quizzes adicionales");

                // Crear quizzes adicionales usando sesiones finalizadas aleatorias
                List<LearningSession> shuffledSessions = new ArrayList<>(allFinishedSessions);
                Collections.shuffle(shuffledSessions);

                for (int i = 0; i < additionalNeeded && i < shuffledSessions.size(); i++) {
                    LearningSession session = shuffledSessions.get(i);
                    Quiz quiz = createQuiz(learner, session);
                    quizRepository.save(quiz);
                    quizzesCreated++;
                    totalQuizzes++;
                }
            }

            logger.info("Learner " + learner.getId() + " (" + learner.getPerson().getFullName() +
                    "): " + quizzesCreated + " quizzes creados (requerido: " + minQuizzesNeeded + ")");
        }

        logger.info("QuizSeeder: " + totalQuizzes + " quizzes creados en total");
    }

    private Quiz createQuiz(Learner learner, LearningSession session) {
        Quiz quiz = new Quiz();

        quiz.setLearner(learner);
        quiz.setLearningSession(session);

        // CRITICAL: Establecer el skill desde la session
        quiz.setSkill(session.getSkill());

        // Score (70-100) - SIEMPRE APROBADO para garantizar credenciales
        int scoreObtained = 70 + random.nextInt(31);
        quiz.setScoreObtained(scoreObtained);
        quiz.setPassed(true); // SIEMPRE TRUE

        // Fecha de finalización (mismo día de la sesión o hasta 2 días después)
        Calendar completionCal = Calendar.getInstance();
        completionCal.setTime(session.getScheduledDatetime());
        completionCal.add(Calendar.DAY_OF_MONTH, random.nextInt(3)); // 0-2 días después
        completionCal.add(Calendar.HOUR_OF_DAY, random.nextInt(24));
        quiz.setCompletionDate(completionCal.getTime());

        return quiz;
    }
}