package com.project.skillswap.rest.SessionSuggestion;

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

/**
 * REST Controller para gestionar sugerencias personalizadas de sesiones
 * Endpoints:
 * - GET /api/suggestions → Obtiene sugerencias para el usuario autenticado
 * - POST /api/suggestions/{suggestionId}/view → Marca una sugerencia como vista
 * - GET /api/suggestions/health → Health check
 */
@RestController
@RequestMapping("/api/suggestions")
@CrossOrigin(origins = "*")
public class SessionSuggestionRestController {

    //#region Dependencies
    @Autowired
    private SessionSuggestionService suggestionService;

    //  Inyectar PersonRepository para obtener el usuario autenticado
    @Autowired
    private PersonRepository personRepository;
    //#endregion

    //#region GET Endpoints

    /**
     * GET /api/suggestions
     * Obtiene sugerencias personalizadas para el usuario autenticado
     *
     * @return ResponseEntity con sugerencias personalizadas
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSuggestions() {
        try {
            System.out.println("[CONTROLLER] GET /api/suggestions - Fecha: 2025-11-22 02:17:44");

            // Obtener autenticación
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Obtener el email del usuario autenticado (authentication.getName() retorna el username/email)
            String userEmail = authentication.getName();
            System.out.println("[CONTROLLER] Usuario autenticado: " + userEmail);

            // Buscar la Person en la BD usando el email
            Optional<Person> personOptional = personRepository.findByEmail(userEmail);

            // Validar que el usuario exista en la BD
            if (!personOptional.isPresent()) {
                System.err.println("[CONTROLLER] ❌ Usuario no encontrado en BD: " + userEmail);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Usuario no encontrado");
                errorResponse.put("data", null);
                errorResponse.put("count", 0);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Person authenticatedPerson = personOptional.get();
            System.out.println("[CONTROLLER] ✅ Persona obtenida - ID: " + authenticatedPerson.getId() +
                    ", Email: " + authenticatedPerson.getEmail());

            // Generar sugerencias personalizadas
            SessionSuggestionResponse suggestions = suggestionService.generateSuggestions(authenticatedPerson);

            // Construir respuesta exitosa
            Map<String, Object> response = new HashMap<>();
            response.put("success", suggestions.isSuccess());
            response.put("message", suggestions.getMessage());
            response.put("data", suggestions.getSuggestions());
            response.put("count", suggestions.getSuggestions().size());

            System.out.println("[CONTROLLER] ✅ Respuesta exitosa: " + suggestions.getSuggestions().size() + " sugerencias generadas");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[CONTROLLER] ❌ Error en GET /api/suggestions: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error generando sugerencias: " + e.getMessage());
            errorResponse.put("data", null);
            errorResponse.put("count", 0);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * GET /api/suggestions/health
     * Health check del servicio de sugerencias
     *
     * @return ResponseEntity con estado del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        System.out.println("[CONTROLLER] GET /api/suggestions/health - Health check");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "SessionSuggestions");
        response.put("timestamp", "2025-11-22T02:17:44Z");

        return ResponseEntity.ok(response);
    }

    //#endregion

    //#region POST Endpoints

    /**
     * POST /api/suggestions/{suggestionId}/view
     * Marca una sugerencia como vista por el usuario
     *
     * @param suggestionId ID de la sugerencia a marcar como vista
     * @return ResponseEntity con resultado de la operación
     */
    @PostMapping("/{suggestionId}/view")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> recordSuggestionView(
            @PathVariable Long suggestionId) {
        try {
            System.out.println("[CONTROLLER] POST /api/suggestions/" + suggestionId + "/view");

            // Obtener autenticación
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            //  Obtener email del usuario autenticado
            String userEmail = authentication.getName();
            System.out.println("[CONTROLLER] Usuario autenticado: " + userEmail);

            // Marcar la sugerencia como vista
            suggestionService.markSuggestionAsViewed(suggestionId, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sugerencia marcada como vista");
            response.put("suggestionId", suggestionId);
            response.put("viewedBy", userEmail);
            response.put("timestamp", "2025-11-22T02:17:44Z");

            System.out.println("[CONTROLLER] ✅ Sugerencia " + suggestionId + " marcada como vista por " + userEmail);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[CONTROLLER] ❌ Error en POST /api/suggestions/{suggestionId}/view: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error marcando sugerencia como vista: " + e.getMessage());
            errorResponse.put("suggestionId", suggestionId);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    //#endregion
}