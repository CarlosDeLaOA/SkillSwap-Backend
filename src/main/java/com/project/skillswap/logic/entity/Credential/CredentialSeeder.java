package com.project.skillswap.logic.entity.Credential;

import jakarta.transaction.Transactional;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Quiz.Quiz;
import com.project.skillswap.logic.entity.Quiz.QuizRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Skill.Skill;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Order(10)
@Component
public class CredentialSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CredentialSeeder.class);

    private final CredentialRepository credentialRepository;
    private final LearnerRepository learnerRepository;
    private final QuizRepository quizRepository;
    private final Random random = new Random();

    public CredentialSeeder(CredentialRepository credentialRepository,
                            LearnerRepository learnerRepository,
                            QuizRepository quizRepository) {
        this.credentialRepository = credentialRepository;
        this.learnerRepository = learnerRepository;
        this.quizRepository = quizRepository;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (credentialRepository.count() > 0) {
            logger.info("CredentialSeeder: Ya existen credenciales, omitiendo seed");
            return;
        }
        this.seedCredentials();
    }

    private void seedCredentials() {
        List<Learner> learners = learnerRepository.findAll();

        if (learners.isEmpty()) {
            logger.warn("No hay learners para crear credenciales");
            return;
        }

        int totalCredentials = 0;

        for (Learner learner : learners) {
            // Obtener todos los quizzes aprobados del learner
            List<Quiz> learnerQuizzes = quizRepository.findAll().stream()
                    .filter(q -> q.getLearner().getId().equals(learner.getId()))
                    .filter(q -> q.getPassed() != null && q.getPassed())
                    .toList();

            int credentialsCreated = 0;

            // Crear una credencial por cada quiz aprobado
            for (Quiz quiz : learnerQuizzes) {
                LearningSession session = quiz.getLearningSession();
                Skill skill = session.getSkill();

                Credential credential = createCredential(learner, skill, session, quiz);
                credentialRepository.save(credential);
                credentialsCreated++;
                totalCredentials++;
            }

            // Verificar que el learner tenga al menos 60 credenciales
            // Si no tiene suficientes, crear credenciales adicionales usando quizzes existentes
            int minCredentials = learner.getCredentialsObtained();
            if (credentialsCreated < minCredentials && !learnerQuizzes.isEmpty()) {
                int additionalNeeded = minCredentials - credentialsCreated;

                for (int i = 0; i < additionalNeeded; i++) {
                    // Reutilizar quizzes aleatoriamente para alcanzar el mínimo
                    Quiz randomQuiz = learnerQuizzes.get(random.nextInt(learnerQuizzes.size()));
                    LearningSession session = randomQuiz.getLearningSession();
                    Skill skill = session.getSkill();

                    Credential credential = createCredential(learner, skill, session, randomQuiz);
                    credentialRepository.save(credential);
                    credentialsCreated++;
                    totalCredentials++;
                }
            }

            logger.info("Learner " + learner.getId() + " (" + learner.getPerson().getFullName() +
                    "): " + credentialsCreated + " credenciales creadas");
        }

        logger.info("CredentialSeeder: " + totalCredentials + " credenciales creadas en total");
    }

    private Credential createCredential(Learner learner, Skill skill, LearningSession session, Quiz quiz) {
        Credential credential = new Credential();

        credential.setLearner(learner);
        credential.setSkill(skill);
        credential.setLearningSession(session);
        credential.setQuiz(quiz);
        BigDecimal percentage = BigDecimal.valueOf(quiz.getScoreObtained());
        credential.setPercentageAchieved(percentage);

        // Badge URL según el porcentaje
        String badgeUrl = getBadgeUrl(percentage);
        credential.setBadgeUrl(badgeUrl);

        // Fecha de obtención (mismo día que completó el quiz o 1 día después)
        Calendar obtainedCal = Calendar.getInstance();
        obtainedCal.setTime(quiz.getCompletionDate());
        obtainedCal.add(Calendar.DAY_OF_MONTH, random.nextInt(2)); // 0-1 días después
        credential.setObtainedDate(obtainedCal.getTime());

        return credential;
    }

    private String getBadgeUrl(BigDecimal percentage) {
        int percent = percentage.intValue();

        if (percent >= 95) {
            return "https://img.icons8.com/color/96/medal2--v1.png"; // Oro
        } else if (percent >= 85) {
            return "https://img.icons8.com/color/96/medal2--v2.png"; // Plata
        } else if (percent >= 75) {
            return "https://img.icons8.com/color/96/medal2.png"; // Bronce
        } else {
            return "https://img.icons8.com/color/96/achievement.png"; // Básico
        }
    }
}