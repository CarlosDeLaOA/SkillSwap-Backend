package com.project.skillswap.rest.dashboard;

import com.project.skillswap.logic.entity.dashboard.SessionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para el historial de sesiones de SkillSeekers.
 * Permite a los estudiantes consultar las sesiones en las que han participado.
 *
 * Endpoints:
 * - GET /api/dashboard/session-history/learner/{learnerId} : Lista sesiones históricas
 * - GET /api/dashboard/session-history/learner/{learnerId}/session/{sessionId} : Detalles de sesión
 *
 * @author Byte&Bite Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/dashboard/session-history")
@CrossOrigin(origins = "*")
public class SessionHistoryController {

    @Autowired
    private SessionHistoryService sessionHistoryService;

    /**
     * Lista las sesiones históricas de un SkillSeeker (estudiante).
     *
     * @param learnerId ID del estudiante
     * @param page número de página (default: 0)
     * @param size tamaño de página (default: 10)
     * @return respuesta con lista paginada de sesiones
     */
    @GetMapping("/learner/{learnerId}")
    public ResponseEntity<Map<String, Object>> getLearnerHistoricalSessions(
            @PathVariable Long learnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> response = sessionHistoryService
                .getLearnerHistoricalSessions(learnerId, page, size);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene los detalles de una sesión específica para un estudiante.
     *
     * @param learnerId ID del estudiante
     * @param sessionId ID de la sesión
     * @return respuesta con detalles de la sesión y número de participantes
     */
    @GetMapping("/learner/{learnerId}/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionDetails(
            @PathVariable Long learnerId,
            @PathVariable Long sessionId) {

        Map<String, Object> response = sessionHistoryService
                .getSessionDetails(sessionId, learnerId);

        return ResponseEntity.ok(response);
    }
}