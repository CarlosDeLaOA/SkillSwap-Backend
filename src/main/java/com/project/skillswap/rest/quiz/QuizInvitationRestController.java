package com.project.skillswap.rest.quiz;

import com.project.skillswap.logic.entity.LearningSession.SessionCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestión de invitaciones de quiz
 */
@RestController
@RequestMapping("/api/quiz/invitations")
@CrossOrigin(origins = "*")
public class QuizInvitationRestController {

    //#region Properties
    private static final Logger logger = LoggerFactory.getLogger(QuizInvitationRestController.class);

    private final SessionCompletionService sessionCompletionService;
    //#endregion

    //#region Constructor
    @Autowired
    public QuizInvitationRestController(SessionCompletionService sessionCompletionService) {
        this.sessionCompletionService = sessionCompletionService;
    }
    //#endregion

    //#region Endpoints
    /**
     * Envía invitaciones de quiz para una sesión específica
     * Endpoint manual para enviar invitaciones cuando sea necesario
     *
     * @param sessionId ID de la sesión
     * @return ResponseEntity con el resultado
     */
    @PostMapping("/send/{sessionId}")
    public ResponseEntity<Map<String, Object>> sendQuizInvitations(@PathVariable Long sessionId) {
        logger.info("========================================");
        logger.info(" ENDPOINT: Enviar invitaciones de quiz");
        logger.info("   Session ID: {}", sessionId);
        logger.info("========================================");

        try {
            sessionCompletionService.sendQuizInvitationsForSession(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Invitaciones de quiz enviadas exitosamente");
            response.put("sessionId", sessionId);

            logger.info(" Invitaciones enviadas exitosamente para session {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error(" Sesión no encontrada: {}", sessionId);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (IllegalStateException e) {
            logger.error(" Error de estado: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            logger.error(" Error al enviar invitaciones: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al enviar invitaciones: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Endpoint de prueba para verificar que el servicio está funcionando
     *
     * @return ResponseEntity con información del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "QuizInvitationService");
        response.put("status", "running");
        response.put("endpoints", Map.of(
                "send", "POST /api/quiz/invitations/send/{sessionId}",
                "health", "GET /api/quiz/invitations/health"
        ));
        return ResponseEntity.ok(response);
    }
    //#endregion
}