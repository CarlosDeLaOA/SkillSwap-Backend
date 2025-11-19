package com.project.skillswap.logic.entity.videocall;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
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

    @Autowired
    private LearningSessionRepository sessionRepository;
    //#endregion

    //#region Endpoints
    /**
     * Genera token y datos para unirse a una videollamada
     */
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinVideoCall(@RequestBody Map<String, Object> request) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (request.get("sessionId") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "sessionId es requerido"));
            }

            Long sessionId = Long.valueOf(request.get("sessionId").toString());

            String joinLink = request.get("joinLink") != null
                    ? request.get("joinLink").toString()
                    : null;

            if (joinLink != null && !joinLink.isEmpty() &&
                    !videoCallService.validateVideoCallLink(sessionId, joinLink)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Enlace no válido o sesión no disponible"));
            }

            //  Determinar rol correctamente
            boolean isModerator = isSessionInstructor(person, sessionId);

            System.out.println("========================================");
            System.out.println("🔐 DETERMINANDO ROL DE USUARIO");
            System.out.println("   Usuario: " + person.getFullName());
            System.out.println("   Email: " + person.getEmail());
            System.out.println("   Session ID: " + sessionId);
            System.out.println("   Tiene perfil instructor: " + isInstructor(person));
            System.out.println("   Es instructor de ESTA sesión: " + isModerator);
            System.out.println("========================================");

            Map<String, Object> videoCallData = videoCallService.generateVideoCallToken(sessionId, person, isModerator);

            videoCallData.put("cameraEnabled", request.getOrDefault("cameraEnabled", true));
            videoCallData.put("microphoneEnabled", request.getOrDefault("microphoneEnabled", true));

            videoCallService.registerParticipantJoin(sessionId, person.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Unión exitosa a videollamada");
            response.put("data", videoCallData);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "sessionId debe ser un número válido"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al unirse a videollamada: " + e.getMessage()));
        }
    }

    @GetMapping("/info/{sessionId}")
    public ResponseEntity<Map<String, Object>> getVideoCallInfo(@PathVariable Long sessionId) {
        try {
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

    @PostMapping("/validate-screen-share")
    public ResponseEntity<Map<String, Object>> validateScreenShare(@RequestBody Map<String, Object> request) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            Long sessionId = Long.valueOf(request.get("sessionId").toString());
            boolean canShare = isSessionInstructor(person, sessionId);

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

    @PostMapping("/end/{sessionId}")
    public ResponseEntity<Map<String, Object>> endVideoCall(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
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
    private Person getAuthenticatedPerson() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        return personRepository.findByEmail(email).orElse(null);
    }


    private boolean isSessionInstructor(Person person, Long sessionId) {
        try {
            // 1. Verificar que tenga perfil de instructor
            if (!isInstructor(person)) {
                System.out.println("    No tiene perfil de instructor");
                return false;
            }

            // 2. Obtener la sesión
            LearningSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null) {
                System.out.println("    Sesión no encontrada");
                return false;
            }

            // 3. Verificar que el instructor de la sesión sea esta persona
            Long sessionInstructorId = session.getInstructor().getId();
            Long personInstructorId = person.getInstructor().getId();

            System.out.println("   ID Instructor de la sesión: " + sessionInstructorId);
            System.out.println("   ID Instructor de la persona: " + personInstructorId);

            boolean isMatch = sessionInstructorId.equals(personInstructorId);
            System.out.println("   ¿Coinciden? " + isMatch);

            return isMatch;

        } catch (Exception e) {
            System.err.println("    Error al verificar instructor: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean isInstructor(Person person) {
        return person.getInstructor() != null && person.getInstructor().getId() != null;
    }
    //#endregion
}