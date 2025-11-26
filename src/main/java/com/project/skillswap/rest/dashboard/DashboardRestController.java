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
     * Gets skill session statistics for authenticated user
     * Returns completed and pending sessions grouped by skill
     *
     * Endpoint: GET /dashboard/skill-session-stats
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with skill session statistics
     */
    @GetMapping("/skill-session-stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSkillSessionStats(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            String role = determineUserRole(authenticatedPerson);

            List<SkillSessionStatsResponse> stats = dashboardService.getSkillSessionStats(
                    authenticatedPerson.getId(),
                    role
            );

            return new GlobalResponseHandler().handleResponse(
                    "Skill session stats retrieved successfully",
                    stats,
                    HttpStatus.OK,
                    request
            );

        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (Exception e) {
            System.err.println("Error getting skill session stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving skill session stats",
                            "Error retrieving skill session stats: " + e.getMessage()));
        }
    }

    /**
     * Gets monthly achievements for authenticated learner
     * Returns credentials and certificates obtained per month
     *
     * Endpoint: GET /dashboard/monthly-achievements
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with monthly achievements data
     */
    @GetMapping("/monthly-achievements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMonthlyAchievements(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            List<MonthlyAchievementResponse> achievements = dashboardService.getMonthlyAchievements(
                    authenticatedPerson.getId()
            );

            return new GlobalResponseHandler().handleResponse(
                    "Monthly achievements retrieved successfully",
                    achievements,
                    HttpStatus.OK,
                    request
            );

        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (Exception e) {
            System.err.println("Error getting monthly achievements: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving monthly achievements",
                            "Error retrieving monthly achievements: " + e.getMessage()));
        }
    }

    /**
     * Gets monthly attendance for authenticated instructor
     * Returns present vs registered attendees per month
     *
     * Endpoint: GET /dashboard/monthly-attendance
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with monthly attendance data
     */
    @GetMapping("/monthly-attendance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMonthlyAttendance(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            List<MonthlyAttendanceResponse> attendance = dashboardService.getMonthlyAttendance(
                    authenticatedPerson.getId()
            );

            return new GlobalResponseHandler().handleResponse(
                    "Monthly attendance retrieved successfully",
                    attendance,
                    HttpStatus.OK,
                    request
            );

        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (Exception e) {
            System.err.println("Error getting monthly attendance: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving monthly attendance",
                            "Error retrieving monthly attendance: " + e.getMessage()));
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

    /**
     * Gets account balance for authenticated user
     * Returns available SkillCoins
     *
     * Endpoint: GET /dashboard/account-balance
     * Requires: Valid JWT token in Authorization header
     *
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with account balance data
     */
    @GetMapping("/account-balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAccountBalance(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            String role = determineUserRole(authenticatedPerson);

            Integer balance = dashboardService.getAccountBalance(
                    authenticatedPerson.getId(),
                    role
            );

            // Crear respuesta simple con el balance
            Map<String, Integer> response = new HashMap<>();
            response.put("skillCoins", balance);

            return new GlobalResponseHandler().handleResponse(
                    "Account balance retrieved successfully",
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
            System.err.println("Error getting account balance: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving account balance",
                            "Error retrieving account balance: " + e.getMessage()));
        }
    }

    //#region Session History Endpoints
    /**
     * Gets historical sessions for authenticated learner
     * Returns completed and cancelled sessions with pagination
     *
     * Endpoint: GET /dashboard/session-history
     * Requires: Valid JWT token in Authorization header
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with paginated historical sessions
     */
    @GetMapping("/session-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getHistoricalSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            String role = determineUserRole(authenticatedPerson);

            if (!"LEARNER".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied",
                                "Only learners can access session history"));
            }

            // Obtener el ID del Learner
            Long learnerId = authenticatedPerson.getLearner().getId();

            Map<String, Object> response = dashboardService.getLearnerHistoricalSessions(
                    learnerId,
                    page,
                    size
            );

            return new GlobalResponseHandler().handleResponse(
                    "Historical sessions retrieved successfully",
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
            System.err.println("Error getting historical sessions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving historical sessions",
                            "Error retrieving historical sessions: " + e.getMessage()));
        }
    }

    /**
     * Gets details of a specific historical session for authenticated learner
     * Returns session information and participant count
     *
     * Endpoint: GET /dashboard/session-history/{sessionId}
     * Requires: Valid JWT token in Authorization header
     *
     * @param sessionId ID of the session
     * @param request HttpServletRequest for metadata
     * @return ResponseEntity with session details
     */
    @GetMapping("/session-history/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSessionDetails(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            String role = determineUserRole(authenticatedPerson);

            if (!"LEARNER".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied",
                                "Only learners can access session details"));
            }

            // Obtener el ID del Learner
            Long learnerId = authenticatedPerson.getLearner().getId();

            Map<String, Object> response = dashboardService.getSessionDetails(
                    sessionId,
                    learnerId
            );

            return new GlobalResponseHandler().handleResponse(
                    "Session details retrieved successfully",
                    response,
                    HttpStatus.OK,
                    request
            );

        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (RuntimeException e) {
            System.err.println("Error getting session details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Session not found", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error getting session details: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving session details",
                            "Error retrieving session details: " + e.getMessage()));
        }
    }
        //#endregion

    //#endregion
}