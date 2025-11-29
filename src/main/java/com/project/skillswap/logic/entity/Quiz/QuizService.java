package com.project.skillswap.logic.entity.Quiz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Question.Question;
import com.project.skillswap.logic.entity.Question.QuestionRepository;
import com.project.skillswap.logic.entity.Quiz.Quiz;
import com.project.skillswap.logic.entity.Quiz.QuizRepository;
import com.project.skillswap.logic.entity.Quiz.QuizStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de cuestionarios de evaluación
 */
@Service
public class QuizService {

    //#region Constants
    private static final int MAX_ATTEMPTS = 2;
    private static final int TOTAL_QUESTIONS = 10;
    private static final int TOTAL_OPTIONS = 12;
    private static final double PASSING_SCORE = 70.0;
    //#endregion

    //#region Dependencies
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final LearnerRepository learnerRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;
    //#endregion

    //#region Constructor
    @Autowired
    public QuizService(
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            LearnerRepository learnerRepository,
            LearningSessionRepository learningSessionRepository,
            BookingRepository bookingRepository,
            ObjectMapper objectMapper
    ) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.learnerRepository = learnerRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.bookingRepository = bookingRepository;
        this.objectMapper = objectMapper;
    }
    //#endregion

    //#region Public Methods
    /**
     * Obtiene o crea un cuestionario para un learner en una sesión específica
     *
     * @param sessionId ID de la sesión de aprendizaje
     * @param learnerId ID del aprendiz
     * @return el cuestionario encontrado o creado
     * @throws IllegalArgumentException si el learner no asistió o excedió intentos
     */
    @Transactional
    public Quiz getOrCreateQuiz(Long sessionId, Long learnerId) {
        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new IllegalArgumentException("Aprendiz no encontrado"));

        validateLearnerAttendance(session, learner);

        Optional<Quiz> inProgressQuiz = quizRepository
                .findFirstByLearnerAndLearningSessionOrderByAttemptNumberDesc(learner, session)
                .filter(q -> q.getStatus() == QuizStatus.IN_PROGRESS);

        if (inProgressQuiz.isPresent()) {
            return inProgressQuiz.get();
        }

        Long attemptCount = quizRepository.countAttemptsByLearnerAndSession(learnerId, sessionId);

        if (attemptCount >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Has alcanzado el número máximo de intentos (2)");
        }

        return createNewQuiz(session, learner, attemptCount.intValue() + 1);
    }

    /**
     * Guarda una respuesta parcial del usuario
     *
     * @param quizId ID del cuestionario
     * @param questionNumber número de la pregunta
     * @param userAnswer respuesta del usuario
     * @throws IllegalArgumentException si el quiz o pregunta no existen
     */
    @Transactional
    public void savePartialAnswer(Long quizId, Integer questionNumber, String userAnswer) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado"));

        if (quiz.getStatus() != QuizStatus.IN_PROGRESS) {
            throw new IllegalStateException("El cuestionario ya ha sido enviado");
        }

        Question question = quiz.getQuestions().stream()
                .filter(q -> q.getNumber().equals(questionNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));

        question.setUserAnswer(userAnswer);
        questionRepository.save(question);
    }

    /**
     * Envía el cuestionario completo para calificación
     *
     * @param quizId ID del cuestionario
     * @return el cuestionario calificado
     * @throws IllegalArgumentException si el quiz no existe
     * @throws IllegalStateException si no están todas las respuestas
     */
    @Transactional
    public Quiz submitQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado"));

        if (quiz.getStatus() != QuizStatus.IN_PROGRESS) {
            throw new IllegalStateException("El cuestionario ya ha sido enviado");
        }

        validateAllAnswersProvided(quiz);

        gradeQuiz(quiz);

        quiz.setStatus(QuizStatus.GRADED);
        quiz.setCompletionDate(new Date());

        return quizRepository.save(quiz);
    }

    /**
     * Obtiene el detalle completo de un cuestionario con sus preguntas
     *
     * @param quizId ID del cuestionario
     * @return el cuestionario con todas sus preguntas
     * @throws IllegalArgumentException si el quiz no existe
     */
    @Transactional(readOnly = true)
    public Quiz getQuizWithQuestions(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado"));

        quiz.getQuestions().size();

        return quiz;
    }

    /**
     * Verifica cuántos intentos le quedan a un learner para una sesión
     *
     * @param sessionId ID de la sesión
     * @param learnerId ID del aprendiz
     * @return número de intentos restantes (0-2)
     */
    @Transactional(readOnly = true)
    public Integer getRemainingAttempts(Long sessionId, Long learnerId) {
        Long attemptCount = quizRepository.countAttemptsByLearnerAndSession(learnerId, sessionId);
        return Math.max(0, MAX_ATTEMPTS - attemptCount.intValue());
    }
    //#endregion

    //#region Private Methods
    /**
     * Valida que el learner haya asistido a la sesión
     */
    private void validateLearnerAttendance(LearningSession session, Learner learner) {
        boolean attended = session.getBookings().stream()
                .anyMatch(booking ->
                        booking.getLearner().getId().equals(learner.getId()) &&
                                Boolean.TRUE.equals(booking.getAttended())
                );

        if (!attended) {
            throw new IllegalArgumentException("No tienes permitido realizar este cuestionario. " +
                    "Solo los participantes que asistieron a la sesión pueden realizarlo.");
        }
    }

    /**
     * Crea un nuevo cuestionario con preguntas y opciones generadas
     */
    private Quiz createNewQuiz(LearningSession session, Learner learner, int attemptNumber) {
        Quiz quiz = new Quiz();
        quiz.setLearningSession(session);
        quiz.setSkill(session.getSkill());
        quiz.setLearner(learner);
        quiz.setAttemptNumber(attemptNumber);
        quiz.setStatus(QuizStatus.IN_PROGRESS);

        Map<String, Object> questionsAndOptions = generateQuestionsAndOptions(session.getFullText());

        quiz.setOptionsJson(serializeOptions(questionsAndOptions.get("options")));

        quiz = quizRepository.save(quiz);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> questionsList = (List<Map<String, String>>) questionsAndOptions.get("questions");
        createQuestions(quiz, questionsList);

        return quiz;
    }

    /**
     * Genera las preguntas y opciones basadas en la transcripción
     * PLACEHOLDER: Aquí se integrará la generación con IA (Groq AI)
     */
    private Map<String, Object> generateQuestionsAndOptions(String transcription) {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, String>> questions = new ArrayList<>();
        for (int i = 1; i <= TOTAL_QUESTIONS; i++) {
            Map<String, String> question = new HashMap<>();
            question.put("number", String.valueOf(i));
            question.put("text", "Pregunta " + i + " (generada por IA)");
            question.put("correctAnswer", "Respuesta " + i);
            questions.add(question);
        }
        result.put("questions", questions);

        List<String> options = new ArrayList<>();
        for (int i = 1; i <= TOTAL_OPTIONS; i++) {
            options.add("Opción " + i);
        }
        Collections.shuffle(options);
        result.put("options", options);

        return result;
    }

    /**
     * Crea las entidades Question asociadas al Quiz
     */
    private void createQuestions(Quiz quiz, List<Map<String, String>> questionsList) {
        List<Question> questions = questionsList.stream()
                .map(qData -> {
                    Question question = new Question();
                    question.setQuiz(quiz);
                    question.setNumber(Integer.parseInt(qData.get("number")));
                    question.setText(qData.get("text"));
                    question.setCorrectAnswer(qData.get("correctAnswer"));
                    question.setIsCorrect(false);
                    return question;
                })
                .collect(Collectors.toList());

        questionRepository.saveAll(questions);
        quiz.setQuestions(questions);
    }

    /**
     * Serializa las opciones a JSON
     */
    private String serializeOptions(Object options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar opciones", e);
        }
    }

    /**
     * Valida que todas las preguntas tengan respuesta
     */
    private void validateAllAnswersProvided(Quiz quiz) {
        boolean allAnswered = quiz.getQuestions().stream()
                .allMatch(q -> q.getUserAnswer() != null && !q.getUserAnswer().trim().isEmpty());

        if (!allAnswered) {
            throw new IllegalStateException("Debes responder todas las preguntas antes de enviar");
        }
    }

    /**
     * Califica el cuestionario comparando respuestas
     */
    private void gradeQuiz(Quiz quiz) {
        int correctCount = 0;

        for (Question question : quiz.getQuestions()) {
            boolean isCorrect = question.getUserAnswer() != null &&
                    question.getUserAnswer().trim().equalsIgnoreCase(
                            question.getCorrectAnswer().trim()
                    );
            question.setIsCorrect(isCorrect);
            if (isCorrect) {
                correctCount++;
            }
        }

        questionRepository.saveAll(quiz.getQuestions());

        quiz.setScoreObtained(correctCount);
        double percentage = (correctCount * 100.0) / TOTAL_QUESTIONS;
        quiz.setPassed(percentage >= PASSING_SCORE);
    }
    //#endregion
}