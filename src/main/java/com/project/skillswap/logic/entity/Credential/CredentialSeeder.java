package com.project.skillswap.logic.entity.Credential;

import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Quiz.Quiz;
import com.project.skillswap.logic.entity.Quiz.QuizRepository;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import com.project.skillswap.logic.entity.Transcription.Transcription;
import com.project.skillswap.logic.entity.Transcription.TranscriptionRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial credentials in the database
 */
@Order(6)
@Component
public class CredentialSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final CredentialRepository credentialRepository;
    private final LearnerRepository learnerRepository;
    private final SkillRepository skillRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final QuizRepository quizRepository;
    private final TranscriptionRepository transcriptionRepository;
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
     * @param transcriptionRepository the transcription repository
     */
    public CredentialSeeder(
            CredentialRepository credentialRepository,
            LearnerRepository learnerRepository,
            SkillRepository skillRepository,
            LearningSessionRepository learningSessionRepository,
            QuizRepository quizRepository,
            TranscriptionRepository transcriptionRepository) {
        this.credentialRepository = credentialRepository;
        this.learnerRepository = learnerRepository;
        this.skillRepository = skillRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.quizRepository = quizRepository;
        this.transcriptionRepository = transcriptionRepository;
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
            Optional<Learner> learner = learnerRepository.findById(credentialData.learnerId.intValue());
            Optional<Skill> skill = skillRepository.findById(credentialData.skillId);
            Optional<LearningSession> session = learningSessionRepository.findById(credentialData.learningSessionId);
            Optional<Transcription> transcription = transcriptionRepository.findByLearningSession(session.orElse(null));

            if (learner.isEmpty() || skill.isEmpty() || session.isEmpty() || transcription.isEmpty()) {
                continue;
            }

            Quiz quiz = createQuiz(learner.get(), skill.get(), transcription.get());
            Quiz savedQuiz = quizRepository.save(quiz);

            Credential credential = createCredential(credentialData, learner.get(), skill.get(), session.get(), savedQuiz);
            credentialRepository.save(credential);
        }

        System.out.println("✅ Credentials seeded successfully");
    }

    /**
     * Creates a Quiz entity for the credential
     *
     * @param learner the learner
     * @param skill the skill
     * @param transcription the transcription
     * @return the created Quiz entity
     */
    private Quiz createQuiz(Learner learner, Skill skill, Transcription transcription) {
        Quiz quiz = new Quiz();
        quiz.setLearner(learner);
        quiz.setSkill(skill);
        quiz.setTranscription(transcription);
        quiz.setScoreObtained(85);
        quiz.setPassed(true);
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
     * Creates the list of credential data to be seeded
     *
     * @return list of CredentialData objects
     */
    private List<CredentialData> createCredentialDataList() {
        List<CredentialData> credentials = new ArrayList<>();

        credentials.add(new CredentialData(
                1L, 2L, 5L,
                new BigDecimal("95.5"),
                "https://example.com/badges/python-advanced.png"
        ));

        credentials.add(new CredentialData(
                1L, 1L, 5L,
                new BigDecimal("88.0"),
                "https://example.com/badges/java-fundamentals.png"
        ));

        credentials.add(new CredentialData(
                1L, 4L, 6L,
                new BigDecimal("92.3"),
                "https://example.com/badges/react-expert.png"
        ));

        credentials.add(new CredentialData(
                1L, 21L, 7L,
                new BigDecimal("90.0"),
                "https://example.com/badges/ml-basics.png"
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