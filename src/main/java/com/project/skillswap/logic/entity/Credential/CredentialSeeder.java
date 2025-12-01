package com.project.skillswap.logic.entity.Credential;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Quiz.Quiz;
import com.project.skillswap.logic.entity.Quiz.QuizRepository;
import com.project.skillswap.logic.entity.Quiz.QuizStatus;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial credentials in the database
 */
@Order(6)
@Component
public class CredentialSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CredentialSeeder.class);

    //#region Dependencies
    private final CredentialRepository credentialRepository;
    private final LearnerRepository learnerRepository;
    private final SkillRepository skillRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final QuizRepository quizRepository;
    //#endregion

    //#region Constructor
    /**
     * Creates a new CredentialSeeder instance
     *
     * @param credentialRepository the credential repository
     * @param learnerRepository the learner repository
     * @param skillRepository the skill repository
     * @param learningSessionRepository the learning session repository
     * @param quizRepository the quiz repository
     */
    public CredentialSeeder(
            CredentialRepository credentialRepository,
            LearnerRepository learnerRepository,
            SkillRepository skillRepository,
            LearningSessionRepository learningSessionRepository,
            QuizRepository quizRepository) {
        this.credentialRepository = credentialRepository;
        this.learnerRepository = learnerRepository;
        this.skillRepository = skillRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.quizRepository = quizRepository;
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
        this.seedCredentials();
    }
    //#endregion

    //#region Seeding Logic
    /**
     * Seeds credentials into the database
     */
    private void seedCredentials() {
        List<CredentialData> credentialsToCreate = createCredentialDataList();

        for (CredentialData credentialData : credentialsToCreate) {
            Optional<Learner> learner = learnerRepository.findById(credentialData.learnerId);
            Optional<Skill> skill = skillRepository.findById(credentialData.skillId);
            Optional<LearningSession> session = learningSessionRepository.findById(credentialData.learningSessionId);

            if (learner.isEmpty() || skill.isEmpty() || session.isEmpty()) {
                continue;
            }

            Optional<Quiz> existingQuiz = quizRepository.findFirstByLearnerAndLearningSessionOrderByAttemptNumberDesc(
                    learner.get(), session.get()
            );

            if (existingQuiz.isPresent()) {
                continue;
            }

            Quiz quiz = createQuiz(learner.get(), skill.get(), session.get(), credentialData.percentageAchieved);
            Quiz savedQuiz = quizRepository.save(quiz);

            Credential credential = createCredential(credentialData, learner.get(), skill.get(), session.get(), savedQuiz);
            credentialRepository.save(credential);
        }
    }

    /**
     * Creates a Quiz entity for the credential
     *
     * @param learner the learner
     * @param skill the skill
     * @param learningSession the learning session
     * @param percentageAchieved the percentage achieved
     * @return the created Quiz entity
     */
    private Quiz createQuiz(Learner learner, Skill skill, LearningSession learningSession,
                            BigDecimal percentageAchieved) {
        Quiz quiz = new Quiz();
        quiz.setLearner(learner);
        quiz.setSkill(skill);
        quiz.setLearningSession(learningSession);
        quiz.setAttemptNumber(1);
        quiz.setStatus(QuizStatus.GRADED);

        int scoreObtained = (int) Math.round(10 * percentageAchieved.doubleValue() / 100);
        quiz.setScoreObtained(scoreObtained);
        quiz.setPassed(percentageAchieved.compareTo(new BigDecimal("70.0")) >= 0);
        quiz.setCompletionDate(new Date());

        String[] sampleOptions = {
                "Option 1", "Option 2", "Option 3", "Option 4",
                "Option 5", "Option 6", "Option 7", "Option 8",
                "Option 9", "Option 10", "Option 11", "Option 12"
        };
        quiz.setOptionsJson(convertToJson(sampleOptions));

        return quiz;
    }

    /**
     * Creates a Credential entity from CredentialData
     *
     * @param data the credential data
     * @param learner the learner
     * @param skill the skill
     * @param session the learning session
     * @param quiz the quiz
     * @return the created Credential entity
     */
    private Credential createCredential(CredentialData data, Learner learner, Skill skill,
                                        LearningSession session, Quiz quiz) {
        Credential credential = new Credential();
        credential.setLearner(learner);
        credential.setSkill(skill);
        credential.setLearningSession(session);
        credential.setQuiz(quiz);
        credential.setPercentageAchieved(data.percentageAchieved);
        credential.setBadgeUrl(data.badgeUrl);
        return credential;
    }

    /**
     * Converts array to JSON string
     *
     * @param options array of options
     * @return JSON string
     */
    private String convertToJson(String[] options) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < options.length; i++) {
            json.append("\"").append(options[i]).append("\"");
            if (i < options.length - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Creates the list of credential data to be seeded
     *
     * @return list of CredentialData objects
     */
    private List<CredentialData> createCredentialDataList() {
        List<CredentialData> credentials = new ArrayList<>();

        credentials.add(new CredentialData(
                2L, 2L, 2L,
                new BigDecimal("92.5"), "https://example.com/badges/python-intermedio.png"
        ));
        credentials.add(new CredentialData(
                2L, 5L, 5L,
                new BigDecimal("88.0"), "https://example.com/badges/react-avanzado.png"
        ));
        credentials.add(new CredentialData(
                5L, 18L, 4L,
                new BigDecimal("95.0"), "https://example.com/badges/caligrafia-japonesa.png"
        ));
        credentials.add(new CredentialData(
                2L, 21L, 20L,
                new BigDecimal("90.0"), "https://example.com/badges/machine-learning.png"
        ));
        credentials.add(new CredentialData(2L, 3L, 30L, new BigDecimal("89.0"), "https://example.com/badges/js-moderno.png"));
        credentials.add(new CredentialData(2L, 12L, 31L, new BigDecimal("91.5"), "https://example.com/badges/espanol-viajes.png"));
        credentials.add(new CredentialData(5L, 18L, 32L, new BigDecimal("94.0"), "https://example.com/badges/origami-avanzado.png"));
        credentials.add(new CredentialData(1L, 21L, 33L, new BigDecimal("87.0"), "https://example.com/badges/pandas-basico.png"));
        credentials.add(new CredentialData(8L, 16L, 34L, new BigDecimal("90.0"), "https://example.com/badges/guitarra-electrica.png"));
        credentials.add(new CredentialData(1L, 8L, 35L, new BigDecimal("88.5"), "https://example.com/badges/sql-optimizacion.png"));
        credentials.add(new CredentialData(2L, 7L, 36L, new BigDecimal("92.0"), "https://example.com/badges/algebra-lineal.png"));
        credentials.add(new CredentialData(1L, 17L, 37L, new BigDecimal("85.0"), "https://example.com/badges/pinyin-basico.png"));
        credentials.add(new CredentialData(17L, 8L, 38L, new BigDecimal("93.0"), "https://example.com/badges/figma-uiux.png"));
        credentials.add(new CredentialData(20L, 3L, 39L, new BigDecimal("89.0"), "https://example.com/badges/yoga-iniciacion.png"));

        credentials.add(new CredentialData(5L, 12L, 40L, new BigDecimal("90.0"), "https://example.com/badges/espanol-negocios.png"));
        credentials.add(new CredentialData(8L, 3L, 41L, new BigDecimal("87.5"), "https://example.com/badges/dom-avanzado.png"));
        credentials.add(new CredentialData(2L, 18L, 42L, new BigDecimal("95.0"), "https://example.com/badges/ikebana-moderna.png"));
        credentials.add(new CredentialData(13L, 21L, 43L, new BigDecimal("88.0"), "https://example.com/badges/seaborn-visual.png"));
        credentials.add(new CredentialData(15L, 16L, 44L, new BigDecimal("91.0"), "https://example.com/badges/fingerstyle.png"));
        credentials.add(new CredentialData(17L, 8L, 45L, new BigDecimal("89.5"), "https://example.com/badges/sql-bi.png"));
        credentials.add(new CredentialData(1L, 7L, 46L, new BigDecimal("90.0"), "https://example.com/badges/calculo-diferencial.png"));
        credentials.add(new CredentialData(2L, 17L, 47L, new BigDecimal("86.0"), "https://example.com/badges/mandarin-conversacion.png"));
        credentials.add(new CredentialData(5L, 8L, 48L, new BigDecimal("92.0"), "https://example.com/badges/branding-visual.png"));
        credentials.add(new CredentialData(8L, 3L, 49L, new BigDecimal("88.0"), "https://example.com/badges/vinyasa-flow.png"));
        credentials.add(new CredentialData(1L, 3L, 50L, new BigDecimal("91.0"), "https://example.com/badges/foto-movil.png"));
        credentials.add(new CredentialData(1L, 7L, 51L, new BigDecimal("89.5"), "https://example.com/badges/microservicios.png"));
        credentials.add(new CredentialData(8L, 1L, 52L, new BigDecimal("93.0"), "https://example.com/badges/pasta-casera.png"));
        credentials.add(new CredentialData(13L, 10L, 53L, new BigDecimal("87.0"), "https://example.com/badges/seo-tecnico.png"));
        credentials.add(new CredentialData(1L, 13L, 54L, new BigDecimal("90.0"), "https://example.com/badges/frances-viajes.png"));
        credentials.add(new CredentialData(17L, 4L, 55L, new BigDecimal("88.0"), "https://example.com/badges/gtd.png"));
        credentials.add(new CredentialData(20L, 10L, 56L, new BigDecimal("92.0"), "https://example.com/badges/git-avanzado.png"));
        credentials.add(new CredentialData(2L, 2L, 57L, new BigDecimal("94.0"), "https://example.com/badges/automatizacion-python.png"));
        credentials.add(new CredentialData(5L, 8L, 58L, new BigDecimal("90.0"), "https://example.com/badges/tipografia.png"));
        credentials.add(new CredentialData(8L, 3L, 59L, new BigDecimal("89.0"), "https://example.com/badges/yoga-restaurativo.png"));
        credentials.add(new CredentialData(13L, 3L, 60L, new BigDecimal("88.5"), "https://example.com/badges/foto-nocturna.png"));
        credentials.add(new CredentialData(15L, 7L, 61L, new BigDecimal("91.0"), "https://example.com/badges/clean-arch.png"));
        credentials.add(new CredentialData(17L, 1L, 62L, new BigDecimal("95.0"), "https://example.com/badges/pizza-napoletana.png"));
        credentials.add(new CredentialData(1L, 10L, 63L, new BigDecimal("87.0"), "https://example.com/badges/google-ads.png"));
        credentials.add(new CredentialData(2L, 13L, 64L, new BigDecimal("89.0"), "https://example.com/badges/frances-comercial.png"));
        credentials.add(new CredentialData(5L, 4L, 65L, new BigDecimal("92.0"), "https://example.com/badges/oratoria.png"));
        credentials.add(new CredentialData(8L, 10L, 66L, new BigDecimal("90.0"), "https://example.com/badges/github-actions.png"));
        credentials.add(new CredentialData(13L, 2L, 67L, new BigDecimal("93.0"), "https://example.com/badges/python-finanzas.png"));
        credentials.add(new CredentialData(15L, 8L, 68L, new BigDecimal("91.0"), "https://example.com/badges/motion-graphics.png"));
        credentials.add(new CredentialData(1L, 3L, 69L, new BigDecimal("88.0"), "https://example.com/badges/yoga-oficina.png"));
        credentials.add(new CredentialData(
                1L,
                2L,
                75L,
                new BigDecimal("91.5"),
                "https://example.com/badges/john-python-intermediate.png"
        ));

        credentials.add(new CredentialData(
                1L,
                8L,
                76L,
                new BigDecimal("88.0"),
                "https://example.com/badges/john-sql-analysis.png"
        ));

        credentials.add(new CredentialData(
                2L,
                10L,
                77L,
                new BigDecimal("93.5"),
                "https://example.com/badges/john-git-workflows.png"
        ));

        credentials.add(new CredentialData(
                1L,
                3L,
                78L,
                new BigDecimal("89.0"),
                "https://example.com/badges/john-nodejs-backend.png"
        ));
        return credentials;
    }
    //#endregion

    //#region Inner Class
    /**
     * Data class holding information for creating credentials
     */
    private static class CredentialData {
        Long learnerId;
        Long skillId;
        Long learningSessionId;
        BigDecimal percentageAchieved;
        String badgeUrl;

        CredentialData(Long learnerId, Long skillId, Long learningSessionId,
                       BigDecimal percentageAchieved, String badgeUrl) {
            this.learnerId = learnerId;
            this.skillId = skillId;
            this.learningSessionId = learningSessionId;
            this.percentageAchieved = percentageAchieved;
            this.badgeUrl = badgeUrl;
        }
    }
    //#endregion
}