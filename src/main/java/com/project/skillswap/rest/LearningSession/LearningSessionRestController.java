package com.project.skillswap.rest.LearningSession;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionService;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.http.GlobalResponseHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Learning Session operations
 * Provides endpoints to retrieve available learning sessions
 */
@RestController
@RequestMapping("/learning-sessions")
@CrossOrigin(origins = "*")
public class LearningSessionRestController {

    //<editor-fold desc="Dependencies">
    @Autowired
    private LearningSessionService learningSessionService;
    //</editor-fold>

    //<editor-fold desc="GET Endpoints">
    /**
     * GET /learning-sessions/available
     * Obtiene todas las sesiones disponibles (SCHEDULED o ACTIVE recientes)
     *
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with list of available sessions
     */
    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAvailableSessions(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            // Verificar que el usuario tenga un rol válido
            validateUserRole(authenticatedPerson);

            List<LearningSession> sessions = learningSessionService.getAvailableSessions();

            return new GlobalResponseHandler().handleResponse(
                    "Available sessions retrieved successfully",
                    sessions,
                    HttpStatus.OK,
                    request
            );
        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (IllegalStateException e) {
            System.err.println("Error: Invalid user role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Invalid user role", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error getting available sessions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving available sessions",
                            "Error retrieving available sessions: " + e.getMessage()));
        }
    }

    /**
     * GET /learning-sessions/filter
     * Filtra sesiones por categoría y/o idioma
     *
     * Requires: Valid JWT token in Authorization header
     *
     * @param categoryId ID de la categoría (opcional)
     * @param language Idioma de la sesión (opcional)
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with filtered sessions list
     */
    @GetMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFilteredSessions(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String language,
            HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            // Verificar que el usuario tenga un rol válido
            validateUserRole(authenticatedPerson);

            List<LearningSession> sessions = learningSessionService.getFilteredSessions(categoryId, language);

            return new GlobalResponseHandler().handleResponse(
                    "Filtered sessions retrieved successfully",
                    sessions,
                    HttpStatus.OK,
                    request
            );
        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (IllegalStateException e) {
            System.err.println("Error: Invalid user role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Invalid user role", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error getting filtered sessions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving filtered sessions",
                            "Error retrieving filtered sessions: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint to verify service status
     *
     * Endpoint: GET /learning-sessions/health
     * No authentication required
     *
     * @return ResponseEntity with service status
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Learning Sessions");
        response.put("message", "Learning Session Controller is running");
        return ResponseEntity.ok(response);
    }
    //</editor-fold>

    //<editor-fold desc="Private Helper Methods">
    /**
     * Validates that user has either instructor or learner role
     *
     * @param person Authenticated person
     * @throws IllegalStateException If user has no valid role
     */
    private void validateUserRole(Person person) {
        if (person.getInstructor() == null && person.getLearner() == null) {
            throw new IllegalStateException("User must have either instructor or learner role");
        }
    }

    /**
     * Creates error response map
     *
     * @param error Error type
     * @param message Error message
     * @return Map containing error information
     */
    private Map<String, String> createErrorResponse(String error, String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return errorResponse;
    }
    //</editor-fold>
}