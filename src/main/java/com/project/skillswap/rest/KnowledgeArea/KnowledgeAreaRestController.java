package com.project.skillswap.rest.KnowledgeArea;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaService;
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
 * REST Controller for Knowledge Area operations
 * Provides endpoints to retrieve knowledge areas (categories)
 */
@RestController
@RequestMapping("/knowledge-areas")
@CrossOrigin(origins = "*")
public class KnowledgeAreaRestController {

    //<editor-fold desc="Dependencies">
    @Autowired
    private KnowledgeAreaService knowledgeAreaService;
    //</editor-fold>

    //<editor-fold desc="GET Endpoints">
    /**
     * GET /knowledge-areas
     * Obtiene todas las áreas de conocimiento activas
     *
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with list of active knowledge areas
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllKnowledgeAreas(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            // Verificar que el usuario tenga un rol válido
            validateUserRole(authenticatedPerson);

            List<KnowledgeArea> knowledgeAreas = knowledgeAreaService.getAllActiveKnowledgeAreas();

            return new GlobalResponseHandler().handleResponse(
                    "Knowledge areas retrieved successfully",
                    knowledgeAreas,
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
            System.err.println("Error getting knowledge areas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving knowledge areas",
                            "Error retrieving knowledge areas: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint to verify service status
     *
     * Endpoint: GET /knowledge-areas/health
     * No authentication required
     *
     * @return ResponseEntity with service status
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Knowledge Areas");
        response.put("message", "Knowledge Area Controller is running");
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