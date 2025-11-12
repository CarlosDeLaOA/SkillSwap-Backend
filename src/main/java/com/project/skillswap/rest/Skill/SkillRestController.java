package com.project.skillswap.rest.Skill;

import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillService;
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
 * REST Controller for Skill operations
 * Provides endpoints to retrieve skills
 */
@RestController
@RequestMapping("/skills")
@CrossOrigin(origins = "*")
public class SkillRestController {

    //<editor-fold desc="Dependencies">
    @Autowired
    private SkillService skillService;
    //</editor-fold>

    //<editor-fold desc="GET Endpoints">
    /**
     * GET /skills/knowledge-area/{knowledgeAreaId}
     * Gets all active skills for a specific knowledge area
     *
     * Requires: Valid JWT token in Authorization header
     *
     * @param knowledgeAreaId ID of the knowledge area
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with list of active skills
     */
    @GetMapping("/knowledge-area/{knowledgeAreaId}")
    public ResponseEntity<?> getSkillsByKnowledgeArea(
            @PathVariable Long knowledgeAreaId,
            HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication.getPrincipal();

            if (principal instanceof Person authenticatedPerson) {

                validateUserRole(authenticatedPerson);
            }

            List<Skill> skills = skillService.getActiveSkillsByKnowledgeAreaId(knowledgeAreaId);

            return new GlobalResponseHandler().handleResponse(
                    "Skills retrieved successfully",
                    skills,
                    HttpStatus.OK,
                    request
            );
        } catch (IllegalStateException e) {
            System.err.println("Error: Invalid user role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Invalid user role", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error getting skills: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving skills",
                            "Error retrieving skills: " + e.getMessage()));
        }
    }


    /**
     * GET /skills
     * Gets all active skills
     *
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with list of all active skills
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllSkills(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            validateUserRole(authenticatedPerson);

            List<Skill> skills = skillService.getAllActiveSkills();

            return new GlobalResponseHandler().handleResponse(
                    "All skills retrieved successfully",
                    skills,
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
            System.err.println("Error getting all skills: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving all skills",
                            "Error retrieving all skills: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint to verify service status
     *
     * Endpoint: GET /skills/health
     * No authentication required
     *
     * @return ResponseEntity with service status
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Skills");
        response.put("message", "Skill Controller is running");
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