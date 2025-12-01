
package com.project.skillswap.rest.quiz;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Quiz.Quiz;
import com.project.skillswap.logic.entity.Quiz.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestión de cuestionarios
 */
@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizRestController {
    private static final Logger logger = LoggerFactory.getLogger(QuizRestController.class);

    //#region Dependencies
    private final QuizService quizService;
    //#endregion

    //#region Constructor
    @Autowired
    public QuizRestController(QuizService quizService) {
        this.quizService = quizService;
    }
    //#endregion

    //#region Endpoints
    /**
     * Obtiene o crea un cuestionario para un learner en una sesión
     *
     * @param sessionId ID de la sesión
     * @param learnerId ID del aprendiz
     * @return ResponseEntity con el cuestionario
     */
    @GetMapping("/session/{sessionId}/learner/{learnerId}")
    public ResponseEntity<Map<String, Object>> getOrCreateQuiz(
            @PathVariable Long sessionId,
            @PathVariable Long learnerId
    ) {
        try {
            Quiz quiz = quizService.getOrCreateQuiz(sessionId, learnerId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cuestionario obtenido exitosamente");
            response.put("data", quiz);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al obtener el cuestionario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtiene el detalle completo de un cuestionario
     *
     * @param quizId ID del cuestionario
     * @return ResponseEntity con el cuestionario y sus preguntas
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<Map<String, Object>> getQuizDetail(@PathVariable Long quizId) {
        try {
            Quiz quiz = quizService.getQuizWithQuestions(quizId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", quiz);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al obtener el cuestionario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Guarda una respuesta parcial del usuario
     *
     * @param quizId ID del cuestionario
     * @param answerData datos de la respuesta (questionNumber, userAnswer)
     * @return ResponseEntity con confirmación
     */
    @PostMapping("/{quizId}/answer")
    public ResponseEntity<Map<String, Object>> savePartialAnswer(
            @PathVariable Long quizId,
            @RequestBody Map<String, Object> answerData
    ) {
        try {
            Integer questionNumber = (Integer) answerData.get("questionNumber");
            String userAnswer = (String) answerData.get("userAnswer");

            quizService.savePartialAnswer(quizId, questionNumber, userAnswer);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Respuesta guardada");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al guardar la respuesta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Envía el cuestionario completo para calificación
     *
     * @param quizId ID del cuestionario
     * @return ResponseEntity con el resultado de la calificación
     */
    @PostMapping("/{quizId}/submit")
    public ResponseEntity<Map<String, Object>> submitQuiz(@PathVariable Long quizId) {
        try {
            Quiz gradedQuiz = quizService.submitQuiz(quizId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", gradedQuiz.getPassed() ?
                    "¡Felicitaciones! Has aprobado el cuestionario" :
                    "No has alcanzado el puntaje mínimo requerido");
            response.put("data", gradedQuiz);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al enviar el cuestionario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtiene el número de intentos restantes para un learner en una sesión
     *
     * @param sessionId ID de la sesión
     * @param learnerId ID del aprendiz
     * @return ResponseEntity con el número de intentos restantes
     */
    @GetMapping("/session/{sessionId}/learner/{learnerId}/attempts")
    public ResponseEntity<Map<String, Object>> getRemainingAttempts(
            @PathVariable Long sessionId,
            @PathVariable Long learnerId
    ) {
        try {
            Integer remainingAttempts = quizService.getRemainingAttempts(sessionId, learnerId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("remainingAttempts", remainingAttempts);
            response.put("maxAttempts", 2);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al obtener intentos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    //#endregion
}