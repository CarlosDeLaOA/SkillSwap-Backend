
package com.project.skillswap.rest.Skill;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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


@RestController
@RequestMapping("/skills")
@CrossOrigin(origins = "*")
public class SkillRestController {
    private static final Logger logger = LoggerFactory.getLogger(SkillRestController.class);

    //<editor-fold desc="Dependencies">
    @Autowired
    private SkillService skillService;


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
            logger.info("Error: Invalid user role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Invalid user role", e.getMessage()));
        } catch (Exception e) {
            logger.info("Error getting skills: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving skills",
                            "Error retrieving skills: " + e.getMessage()));
        }
    }


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
            logger.info("Error: Authentication principal is not a Person: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Invalid authentication type",
                            "Authenticated user is not of expected type"));
        } catch (IllegalStateException e) {
            logger.info("Error: Invalid user role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Invalid user role", e.getMessage()));
        } catch (Exception e) {
            logger.info("Error getting all skills: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving all skills",
                            "Error retrieving all skills: " + e.getMessage()));
        }
    }


    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Skills");
        response.put("message", "Skill Controller is running");
        return ResponseEntity.ok(response);
    }

    private void validateUserRole(Person person) {
        if (person.getInstructor() == null && person.getLearner() == null) {
            throw new IllegalStateException("User must have either instructor or learner role");
        }
    }


    private Map<String, String> createErrorResponse(String error, String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return errorResponse;
    }

}