package com.project.skillswap.rest.community;

import com.project.skillswap.logic.entity.CommunityInvitation.CommunityInvitationService;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para gestionar comunidades de aprendizaje.
 */
@RestController
@RequestMapping("/communities")
@CrossOrigin(origins = "*")
public class CommunityRestController {

    //#region Dependencies
    private static final Logger logger = LoggerFactory.getLogger(CommunityRestController.class);
    private final CommunityInvitationService invitationService;
    private final PersonRepository personRepository;

    public CommunityRestController(CommunityInvitationService invitationService,
                                   PersonRepository personRepository) {
        this.invitationService = invitationService;
        this.personRepository = personRepository;
    }
    //#endregion

    //#region Endpoints
    /**
     * Crea una nueva comunidad y envía invitaciones.
     *
     * @param request datos de la comunidad
     * @param userDetails usuario autenticado
     * @return respuesta con el resultado
     */
    @PostMapping("/create")
    public ResponseEntity<?> createCommunity(@RequestBody CreateCommunityRequest request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        logger.info(">>> createCommunity called. user={}", userDetails != null ? userDetails.getUsername() : "anonymous");

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("El nombre de la comunidad es requerido"));
        }

        if (request.getName().length() > 150) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("El nombre no puede exceder 150 caracteres"));
        }

        if (request.getMemberEmails() == null || request.getMemberEmails().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Debes invitar al menos un miembro"));
        }

        if (request.getMemberEmails().size() > 9) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Máximo 9 invitaciones permitidas"));
        }

        for (String email : request.getMemberEmails()) {
            if (!isValidEmail(email)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Email inválido: " + email));
            }
        }

        if (request.getCreatorId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("El ID del creador es requerido"));
        }

        CommunityInvitationService.CreateCommunityResult result =
                invitationService.createCommunityWithInvitations(
                        request.getName(),
                        request.getDescription(),
                        request.getCreatorId(),
                        request.getMemberEmails()
                );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());

        if (result.isSuccess()) {
            response.put("communityId", result.getCommunityId());

            if (result.getInvitationsSummary() != null) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("successfulInvitations", result.getInvitationsSummary().getSuccessfulInvitations());
                summary.put("failedInvitations", result.getInvitationsSummary().getFailedInvitations());
                response.put("invitationsSummary", summary);
            }

            logger.info("Community created successfully: {}", result.getCommunityId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            logger.warn("Failed to create community: {}", result.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Acepta una invitación a una comunidad.
     *
     * @param token el token de invitación
     * @param userDetails usuario autenticado
     * @return respuesta con el resultado
     */
    @GetMapping("/accept-invitation")
    public ResponseEntity<?> acceptInvitation(@RequestParam String token,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        logger.info(">>> acceptInvitation called. token={} userDetails={}",
                token, userDetails != null ? userDetails.getUsername() : "null");

        if (token == null || token.trim().isEmpty()) {
            logger.warn("Token is null or empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("El token es requerido"));
        }

        // Verificar que el usuario esté autenticado
        if (userDetails == null) {
            logger.error("UserDetails is null - user not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Debes iniciar sesión para aceptar la invitación"));
        }

        try {
            // Extraer el ID de la persona del usuario autenticado
            Long personId = extractPersonId(userDetails);
            logger.info("Extracted personId: {}", personId);

            if (personId == null) {
                logger.error("Could not extract personId from userDetails: {}", userDetails.getUsername());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("No se pudo identificar tu usuario"));
            }

            // Procesar la invitación
            CommunityInvitationService.AcceptInvitationResult result =
                    invitationService.acceptInvitation(token, personId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("status", result.getStatus().name());

            if (result.isSuccess()) {
                logger.info("Invitation accepted successfully for user: {}", userDetails.getUsername());
                return ResponseEntity.ok(response);
            } else {
                HttpStatus httpStatus = determineHttpStatus(result.getStatus());
                logger.warn("Failed to accept invitation. Status: {} Message: {}",
                        result.getStatus(), result.getMessage());
                return ResponseEntity.status(httpStatus).body(response);
            }
        } catch (Exception e) {
            logger.error("Error accepting invitation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno al procesar la invitación: " + e.getMessage()));
        }
    }
    //#endregion

    //#region Private Methods
    /**
     * Crea un mapa de respuesta de error.
     *
     * @param message mensaje de error
     * @return mapa con el error
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    /**
     * Valida el formato de un email.
     *
     * @param email email a validar
     * @return true si es válido
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Determina el código HTTP apropiado según el estado de invitación.
     *
     * @param status estado de invitación
     * @return código HTTP correspondiente
     */
    private HttpStatus determineHttpStatus(CommunityInvitationService.InvitationStatus status) {
        return switch (status) {
            case INVALID_TOKEN, USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case EXPIRED_TOKEN -> HttpStatus.GONE;
            case ALREADY_ACCEPTED, ALREADY_MEMBER, ALREADY_IN_COMMUNITY, COMMUNITY_FULL -> HttpStatus.CONFLICT;
            case EMAIL_MISMATCH, NOT_LEARNER -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * Extrae el ID de la persona del UserDetails.
     * El UserDetails contiene el email del usuario, así que buscamos la persona por email.
     *
     * @param userDetails usuario autenticado
     * @return ID de la persona o null si no se encuentra
     */
    private Long extractPersonId(UserDetails userDetails) {
        if (userDetails == null) {
            logger.error("UserDetails is null in extractPersonId");
            return null;
        }

        String email = userDetails.getUsername();
        logger.debug("Looking for person with email: {}", email);

        Optional<Person> personOptional = personRepository.findByEmail(email);

        if (personOptional.isPresent()) {
            Person person = personOptional.get();
            logger.debug("Found person with id: {} for email: {}", person.getId(), email);
            return person.getId();
        } else {
            logger.warn("No person found with email: {}", email);
            return null;
        }
    }
    //#endregion

    //#region Request Classes
    /**
     * Clase para el request de crear comunidad.
     */
    public static class CreateCommunityRequest {
        private String name;
        private String description;
        private Long creatorId;
        private List<String> memberEmails;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Long getCreatorId() {
            return creatorId;
        }

        public void setCreatorId(Long creatorId) {
            this.creatorId = creatorId;
        }

        public List<String> getMemberEmails() {
            return memberEmails;
        }

        public void setMemberEmails(List<String> memberEmails) {
            this.memberEmails = memberEmails;
        }
    }
    //#endregion
}