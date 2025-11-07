package com.project.skillswap.rest.person;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
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
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller para gestionar el perfil de Person autenticado
 * Proporciona endpoint para consultar información del usuario
 *
 */
@RestController
@RequestMapping("/persons")
@CrossOrigin(origins = "*")
public class PersonProfileRestController {

    @Autowired
    private PersonRepository personRepository;

    /**
     * Obtiene el perfil completo del usuario autenticado
     * Incluye datos de Person, Instructor (si aplica) y Learner (si aplica)
     *
     * Endpoint: GET /persons/me
     * Requiere: Token JWT válido en el header Authorization
     *
     * @param request HttpServletRequest para metadata
     * @return ResponseEntity con los datos del perfil o error
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAuthenticatedPerson(HttpServletRequest request) {
        try {
            // Obtener el usuario autenticado desde el contexto de seguridad
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Person authenticatedPerson = (Person) authentication.getPrincipal();

            // Recargar desde BD para obtener todas las relaciones (Instructor, Learner)
            Optional<Person> fullPerson = personRepository.findById(authenticatedPerson.getId());

            if (fullPerson.isPresent()) {
                return new GlobalResponseHandler().handleResponse(
                        "User profile retrieved successfully",
                        fullPerson.get(),
                        HttpStatus.OK,
                        request
                );
            } else {
                return new GlobalResponseHandler().handleResponse(
                        "User profile not found",
                        HttpStatus.NOT_FOUND,
                        request
                );
            }
        } catch (ClassCastException e) {
            System.err.println("Error: Authentication principal is not a Person: " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid authentication type");
            errorResponse.put("message", "El usuario autenticado no es del tipo esperado");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            System.err.println("Error getting user profile: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error retrieving profile");
            errorResponse.put("message", "Error al obtener el perfil: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Endpoint de salud para verificar que el controlador está funcionando
     *
     * Endpoint: GET /persons/health
     * No requiere autenticación
     *
     * @return ResponseEntity indicando que el servicio está activo
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "PersonProfile");
        response.put("message", "Person Profile Controller is running");
        return ResponseEntity.ok(response);
    }
}