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
 * Provides endpoints to retrieve, create and publish learning sessions
 */
@RestController
@RequestMapping("/learning-sessions")
@CrossOrigin(origins = "*")
public class LearningSessionRestController {

    //#region Dependencies
    @Autowired
    private LearningSessionService learningSessionService;
    //#endregion

    //#region GET Endpoints
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
     * GET /learning-sessions/{id}
     * Obtiene una sesión por ID
     *
     * Requires: Valid JWT token in Authorization header
     *
     * @param id ID de la sesión
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with session data
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSessionById(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            LearningSession session = learningSessionService.getSessionById(id, authenticatedPerson);

            return new GlobalResponseHandler().handleResponse(
                    "Session retrieved successfully",
                    session,
                    HttpStatus.OK,
                    request
            );

        } catch (IllegalArgumentException e) {
            System.err.println(" Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Not found", e.getMessage()));

        } catch (ClassCastException e) {
            System.err.println(" Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));

        } catch (Exception e) {
            System.err.println(" Error getting session: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving session",
                            "Error al obtener la sesión: " + e.getMessage()));
        }
    }
    //#endregion

    //#region POST Endpoints
    /**
     * POST /learning-sessions
     * Crea una nueva sesión de aprendizaje en estado DRAFT
     *
     * Requires: Valid JWT token in Authorization header
     * Requires: User must have INSTRUCTOR role
     *
     * @param session Datos de la sesión a crear
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with created session
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createSession(
            @RequestBody LearningSession session,
            HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            System.out.println(" [LearningSessionController] Creating session for user: " +
                    authenticatedPerson.getId());

            LearningSession createdSession = learningSessionService.createSession(
                    session,
                    authenticatedPerson
            );

            System.out.println(" [LearningSessionController] Session created in DRAFT: " +
                    createdSession.getId());

            return new GlobalResponseHandler().handleResponse(
                    "Sesión creada en borrador",
                    createdSession,
                    HttpStatus.CREATED,
                    request
            );

        } catch (IllegalStateException e) {
            System.err.println(" Error: Unauthorized role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Rol no autorizado", e.getMessage()));

        } catch (IllegalArgumentException e) {
            System.err.println(" Error: Validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Validation error", e.getMessage()));

        } catch (ClassCastException e) {
            System.err.println(" Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));

        } catch (Exception e) {
            System.err.println(" Error creating session: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error creating session",
                            "Error al crear la sesión: " + e.getMessage()));
        }
    }
    //#endregion

    //#region PUT Endpoints
    /**
     * PUT /learning-sessions/{id}/publish
     * Publica una sesión, cambiando su estado y haciéndola visible
     *
     * Requires: Valid JWT token in Authorization header
     * Requires: User must be the session owner (instructor)
     *
     * @param id ID de la sesión a publicar
     * @param minorEditsRequest Ediciones menores opcionales
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with published session
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> publishSession(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> minorEditsRequest,
            HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            System.out.println(" [LearningSessionController] Publishing session: " + id);

            LearningSession publishedSession = learningSessionService.publishSession(
                    id,
                    authenticatedPerson,
                    minorEditsRequest
            );

            System.out.println(" [LearningSessionController] Session published: " +
                    publishedSession.getId() + " with status: " + publishedSession.getStatus());

            return new GlobalResponseHandler().handleResponse(
                    "Sesión publicada exitosamente",
                    publishedSession,
                    HttpStatus.OK,
                    request
            );

        } catch (IllegalStateException e) {
            System.err.println(" Error: Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("No autorizado", e.getMessage()));

        } catch (IllegalArgumentException e) {
            System.err.println(" Error: Validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Validation error", e.getMessage()));

        } catch (ClassCastException e) {
            System.err.println(" Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));

        } catch (Exception e) {
            System.err.println(" Error publishing session: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error publishing session",
                            "Error al publicar la sesión: " + e.getMessage()));
        }
    }
    //#endregion

    //#region Utility Endpoints
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
    //#endregion

    //#region Private Helper Methods
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
    //#endregion
}