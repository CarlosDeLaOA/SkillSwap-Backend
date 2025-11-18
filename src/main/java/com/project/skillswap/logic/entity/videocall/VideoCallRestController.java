package com.project.skillswap.logic.entity.videocall;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para operaciones de videollamadas
 */
@RestController
@RequestMapping("/videocall")
@CrossOrigin(origins = "http://localhost:4200")
public class VideoCallRestController {

    //#region Dependencies
    @Autowired
    private VideoCallService videoCallService;

    @Autowired
    private PersonRepository personRepository;
    //#endregion

    //#region Endpoints
    /**
     * Genera token y datos para unirse a una videollamada
     * @param request Map con sessionId, joinLink (opcional), cameraEnabled, microphoneEnabled
     * @return ResponseEntity con datos de la videollamada
     */
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinVideoCall(@RequestBody Map<String, Object> request) {
        try {
            // Obtener usuario autenticado
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            // Validar que sessionId exista
            if (request.get("sessionId") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "sessionId es requerido"));
            }

            Long sessionId = Long.valueOf(request.get("sessionId").toString());

            // joinLink es opcional - si no existe, el servicio lo generará
            String joinLink = request.get("joinLink") != null
                    ? request.get("joinLink").toString()
                    : null;

            // Si hay joinLink Y NO está vacío, validarlo
            if (joinLink != null && !joinLink.isEmpty() &&
                    !videoCallService.validateVideoCallLink(sessionId, joinLink)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Enlace no válido o sesión no disponible"));
            }

            // Determinar si es moderador (instructor)
            boolean isModerator = isInstructor(person);

            // Generar token y datos
            Map<String, Object> videoCallData = videoCallService.generateVideoCallToken(sessionId, person, isModerator);

            // Agregar controles iniciales
            videoCallData.put("cameraEnabled", request.getOrDefault("cameraEnabled", false));
            videoCallData.put("microphoneEnabled", request.getOrDefault("microphoneEnabled", false));

            // Registrar unión
            videoCallService.registerParticipantJoin(sessionId, person.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Unión exitosa a videollamada");
            response.put("data", videoCallData);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "sessionId debe ser un número válido"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al unirse a videollamada: " + e.getMessage()));
        }
    }

    /**
     * Obtiene información de una videollamada
     * @param sessionId ID de la sesión
     * @return ResponseEntity con información de la videollamada
     */
    @GetMapping("/info/{sessionId}")
    public ResponseEntity<Map<String, Object>> getVideoCallInfo(@PathVariable Long sessionId) {
        try {
            // Obtener usuario autenticado
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            Map<String, Object> videoCallInfo = videoCallService.getVideoCallInfo(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Información obtenida exitosamente");
            response.put("data", videoCallInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener información: " + e.getMessage()));
        }
    }

    /**
     * Valida permiso para compartir pantalla (solo instructores)
     * @param request Map con sessionId
     * @return ResponseEntity con validación
     */
    @PostMapping("/validate-screen-share")
    public ResponseEntity<Map<String, Object>> validateScreenShare(@RequestBody Map<String, Object> request) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            // Validar que sea instructor (SkillSwapper)
            boolean canShare = isInstructor(person);

            if (!canShare) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "message", "No posee los permisos necesarios para compartir pantalla",
                                "canShareScreen", false
                        ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Permiso concedido para compartir pantalla");
            response.put("canShareScreen", true);
            response.put("personId", person.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al validar permisos: " + e.getMessage()));
        }
    }

    /**
     * Finaliza una videollamada
     * @param sessionId ID de la sesión
     * @return ResponseEntity con confirmación
     */
    @PostMapping("/end/{sessionId}")
    public ResponseEntity<Map<String, Object>> endVideoCall(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            // Solo instructores pueden finalizar la sesión
            if (!isInstructor(person)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede finalizar la sesión"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Videollamada finalizada exitosamente");
            response.put("sessionId", sessionId);
            response.put("endedAt", new java.util.Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al finalizar videollamada: " + e.getMessage()));
        }
    }
    //#endregion

    //#region Private Methods
    /**
     * Obtiene la persona autenticada del contexto de seguridad
     * @return Person autenticada o null
     */
    private Person getAuthenticatedPerson() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        return personRepository.findByEmail(email).orElse(null);
    }

    /**
     * Determina si una persona es instructor
     * @param person Persona a verificar
     * @return true si es instructor, false en caso contrario
     */
    private boolean isInstructor(Person person) {
        // Verificar si tiene un perfil de Instructor
        return person.getInstructor() != null && person.getInstructor().getId() != null;
    }
    //#endregion
}