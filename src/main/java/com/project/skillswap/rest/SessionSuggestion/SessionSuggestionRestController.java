package com.project.skillswap.rest.SessionSuggestion;

import com.project.skillswap.logic.entity.SessionSuggestion.SessionSuggestionService;
import com.project.skillswap.logic.entity.SessionSuggestion.SessionSuggestionResponse;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.auth.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller para sugerencias personalizadas de sesiones
 *
 * Criterios de aceptación implementados:
 * ✅ Generar sugerencias matching categorías de intereses
 * ✅ Score basado en coincidencias
 * ✅ Limitar a 5 sugerencias top; ordenar por score descendente
 * ✅ Default a sesiones populares si no tiene intereses
 * ✅ Validar perfil completo
 * ✅ Registrar vistas de sugerencias
 * ✅ No sugerir sesiones propias
 */
@RestController
@RequestMapping("/api/suggestions")
@CrossOrigin(origins = "*")
public class SessionSuggestionRestController {

    //#region Dependencies
    @Autowired
    private SessionSuggestionService suggestionService;

    @Autowired
    private JwtService jwtService;
    //#endregion

    //#region Endpoints

    /**
     * GET /api/suggestions
     * Obtiene sugerencias personalizadas para el usuario autenticado
     *
     * @param authHeader Header de autorización con JWT
     * @return ResponseEntity con sugerencias
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSuggestions(
            @RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("[CONTROLLER] GET /api/suggestions");

            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            System.out.println("[CONTROLLER] Usuario autenticado: " + userEmail);

            // Generar sugerencias
            SessionSuggestionResponse suggestions = suggestionService.generateSuggestions(authenticatedPerson);

            Map<String, Object> response = new HashMap<>();
            response.put("success", suggestions.isSuccess());
            response.put("message", suggestions.getMessage());
            response.put("data", suggestions.getSuggestions());
            response.put("count", suggestions.getSuggestions().size());

            System.out.println("[CONTROLLER] Respuesta: " + suggestions.getSuggestions().size() + " sugerencias");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[CONTROLLER] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("data", null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * POST /api/suggestions/{suggestionId}/view
     * Registra que el usuario vio una sugerencia
     *
     * @param suggestionId ID de la sugerencia
     * @param authHeader Header de autorización
     * @return ResponseEntity con confirmación
     */
    @PostMapping("/{suggestionId}/view")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> recordSuggestionView(
            @PathVariable Long suggestionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("[CONTROLLER] POST /api/suggestions/" + suggestionId + "/view");

            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token);

            suggestionService.markSuggestionAsViewed(suggestionId, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sugerencia marcada como vista");

            System.out.println("[CONTROLLER] Sugerencia registrada como vista");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[CONTROLLER] Error: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * GET /api/suggestions/health
     * Health check del servicio
     *
     * @return ResponseEntity con estado
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "SessionSuggestions");
        response.put("message", "Session Suggestions Controller is running");
        return ResponseEntity.ok(response);
    }

    //#endregion
}