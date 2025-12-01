package com.project.skillswap.rest.SessionSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.SessionSuggestion.SessionSuggestionService;
import com.project.skillswap.logic.entity.SessionSuggestion.SessionSuggestionResponse;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/*
 REST controller para gestionar sugerencias de sesiones. ***
*/
@RestController
@RequestMapping("/api/suggestions")
@CrossOrigin(origins = "*")
public class SessionSuggestionRestController {
    private static final Logger logger = LoggerFactory.getLogger(SessionSuggestionRestController.class);

    @Autowired
    private SessionSuggestionService suggestionService;

    @Autowired
    private PersonRepository personRepository; // repositorio para buscar Person por email ***

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSuggestions() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();
            Optional<Person> personOptional = personRepository.findByEmail(userEmail);

            if (!personOptional.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Usuario no encontrado");
                errorResponse.put("data", null);
                errorResponse.put("count", 0);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Person authenticatedPerson = personOptional.get();
            SessionSuggestionResponse suggestions = suggestionService.generateSuggestions(authenticatedPerson);

            Map<String, Object> response = new HashMap<>();
            response.put("success", suggestions.isSuccess());
            response.put("message", suggestions.getMessage());
            response.put("data", suggestions.getSuggestions());
            response.put("count", suggestions.getSuggestions().size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error generando sugerencias: " + e.getMessage());
            errorResponse.put("data", null);
            errorResponse.put("count", 0);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{suggestionId}/view")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> recordSuggestionView(@PathVariable Long suggestionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();
            suggestionService.markSuggestionAsViewed(suggestionId, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sugerencia marcada como vista");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error marcando sugerencia: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "SessionSuggestions");
        return ResponseEntity.ok(response);
    }
}