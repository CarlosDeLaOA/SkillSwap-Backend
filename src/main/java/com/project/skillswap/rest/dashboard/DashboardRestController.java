package com.project.skillswap.rest.dashboard;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.dashboard.*;
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
 * REST Controller for dashboard operations
 * Provides endpoints to retrieve dashboard data based on user role
 */
@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*")
public class DashboardRestController {

    //#region Dependencies
    @Autowired
    private DashboardService dashboardService;
    //#endregion

    //#region Endpoints
    /**
     * Gets total learning hours for authenticated user
     * Returns hours based on user role (INSTRUCTOR or LEARNER)
     *
     * Endpoint: GET /dashboard/learning-hours
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with learning hours data
     */
    @GetMapping("/learning-hours")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getLearningHours(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            String role = determineUserRole(authenticatedPerson);

            LearningHoursResponse response = dashboardService.getLearningHours(
                    authenticatedPerson.getId(),
                    role
            );

            return new GlobalResponseHandler().handleResponse(
                    "Learning hours retrieved successfully",
                    response,
                    HttpStatus.OK,
                    request
            );
        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (Exception e) {
            System.err.println("Error getting learning hours: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving learning hours",
                            "Error retrieving learning hours: " + e.getMessage()));
        }
    }

    /**
     * Gets upcoming 5 sessions for authenticated user
     * Returns sessions based on user role (INSTRUCTOR or LEARNER)
     *
     * Endpoint: GET /dashboard/upcoming-sessions
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with upcoming sessions list
     */
    @GetMapping("/upcoming-sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUpcomingSessions(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            String role = determineUserRole(authenticatedPerson);

            List<UpcomingSessionResponse> sessions = dashboardService.getUpcomingSessions(
                    authenticatedPerson.getId(),
                    role
            );

            return new GlobalResponseHandler().handleResponse(
                    "Upcoming sessions retrieved successfully",
                    sessions,
                    HttpStatus.OK,
                    request
            );
        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (Exception e) {
            System.err.println("Error getting upcoming sessions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving upcoming sessions",
                            "Error retrieving upcoming sessions: " + e.getMessage()));
        }
    }

    /**
     * Gets recent achievements for authenticated user
     * Returns credentials for LEARNER or feedbacks for INSTRUCTOR
     *
     * Endpoint: GET /dashboard/recent-achievements
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with recent achievements data
     */
    @GetMapping("/recent-achievements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRecentAchievements(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            String role = determineUserRole(authenticatedPerson);

            if ("LEARNER".equals(role)) {
                List<CredentialResponse> credentials = dashboardService.getRecentCredentials(
                        authenticatedPerson.getId()
                );

                return new GlobalResponseHandler().handleResponse(
                        "Recent credentials retrieved successfully",
                        credentials,
                        HttpStatus.OK,
                        request
                );
            } else {
                List<FeedbackResponse> feedbacks = dashboardService.getRecentFeedbacks(
                        authenticatedPerson.getId()
                );

                return new GlobalResponseHandler().handleResponse(
                        "Recent feedbacks retrieved successfully",
                        feedbacks,
                        HttpStatus.OK,
                        request
                );
            }
        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (Exception e) {
            System.err.println("Error getting recent achievements: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving recent achievements",
                            "Error retrieving recent achievements: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint to verify service status
     *
     * Endpoint: GET /dashboard/health
     * No authentication required
     *
     * @return ResponseEntity with service status
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Dashboard");
        response.put("message", "Dashboard Controller is running");
        return ResponseEntity.ok(response);
    }
    //#endregion

    //#region Private Methods
    /**
     * Determines user role (INSTRUCTOR or LEARNER)
     *
     * @param person Authenticated person
     * @return User role as string
     * @throws IllegalStateException If user has no valid role
     */
    private String determineUserRole(Person person) {
        if (person.getInstructor() != null) {
            return "INSTRUCTOR";
        } else if (person.getLearner() != null) {
            return "LEARNER";
        }
        throw new IllegalStateException("User must have either instructor or learner role");
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