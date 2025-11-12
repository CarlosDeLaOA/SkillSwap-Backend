package com.project.skillswap.rest.UserSkill;

import com.project.skillswap.logic.entity.UserSkill.UserSkill;
import com.project.skillswap.logic.entity.UserSkill.UserSkillService;
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
 * REST Controller for UserSkill operations
 * Provides endpoints to manage user skills
 */
@RestController
@RequestMapping("/user-skills")
@CrossOrigin(origins = "*")
public class UserSkillRestController {

    //<editor-fold desc="Dependencies">
    @Autowired
    private UserSkillService userSkillService;
    //</editor-fold>

    //<editor-fold desc="POST Endpoints">
    /**
     * POST /user-skills
     * Saves user skills during onboarding
     *
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @param requestBody Map containing skillIds
     * @return ResponseEntity with saved user skills
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> saveUserSkills(
            HttpServletRequest request,
            @RequestBody Map<String, List<Long>> requestBody) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            validateUserRole(authenticatedPerson);

            List<Long> skillIds = requestBody.get("skillIds");

            if (skillIds == null || skillIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Invalid request", "skillIds cannot be empty"));
            }

            List<UserSkill> savedSkills = userSkillService.saveUserSkills(authenticatedPerson, skillIds);

            return new GlobalResponseHandler().handleResponse(
                    "User skills saved successfully",
                    savedSkills,
                    HttpStatus.CREATED,
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
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error saving user skills: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error saving user skills",
                            "Error saving user skills: " + e.getMessage()));
        }
    }
    //</editor-fold>

    //<editor-fold desc="GET Endpoints">
    /**
     * GET /user-skills
     * Gets all active user skills for the authenticated user
     *
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with list of active user skills
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserSkills(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            validateUserRole(authenticatedPerson);

            List<UserSkill> userSkills = userSkillService.getActiveUserSkillsByPersonId(authenticatedPerson.getId());

            return new GlobalResponseHandler().handleResponse(
                    "User skills retrieved successfully",
                    userSkills,
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
            System.err.println("Error getting user skills: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving user skills",
                            "Error retrieving user skills: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint to verify service status
     *
     * Endpoint: GET /user-skills/health
     * No authentication required
     *
     * @return ResponseEntity with service status
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "User Skills");
        response.put("message", "User Skill Controller is running");
        return ResponseEntity.ok(response);
    }
    //</editor-fold>

    //<editor-fold desc="DELETE Endpoints">
    /**
     * DELETE /user-skills/{id}
     * Deactivates a user skill
     *
     * Requires: Valid JWT token in Authorization header
     *
     * @param id the user skill ID
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with result
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deactivateUserSkill(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            validateUserRole(authenticatedPerson);

            userSkillService.deactivateUserSkill(id);

            return new GlobalResponseHandler().handleResponse(
                    "User skill deactivated successfully",
                    null,
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
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error deactivating user skill: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error deactivating user skill",
                            "Error deactivating user skill: " + e.getMessage()));
        }
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