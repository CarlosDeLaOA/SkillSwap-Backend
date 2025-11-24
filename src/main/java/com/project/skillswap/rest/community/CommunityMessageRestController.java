package com.project.skillswap.rest.community;

import com.project.skillswap.logic.entity.CommunityMessage.CommunityMessage;
import com.project.skillswap.logic.entity.CommunityMessage.CommunityMessageRepository;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controlador para gestionar mensajes de comunidades.
 */
@Controller
@CrossOrigin(origins = "*")
public class CommunityMessageRestController {

    //#region Dependencies
    private static final Logger logger = LoggerFactory.getLogger(CommunityMessageRestController.class);
    private final CommunityMessageRepository messageRepository;
    private final LearningCommunityRepository communityRepository;
    private final PersonRepository personRepository;

    public CommunityMessageRestController(CommunityMessageRepository messageRepository,
                                          LearningCommunityRepository communityRepository,
                                          PersonRepository personRepository) {
        this.messageRepository = messageRepository;
        this.communityRepository = communityRepository;
        this.personRepository = personRepository;
    }
    //#endregion

    //#region WebSocket Endpoints
    /**
     * Recibe un mensaje por WebSocket y lo difunde a todos los suscriptores.
     *
     * @param communityId ID de la comunidad
     * @param messageRequest datos del mensaje
     * @return mensaje guardado
     */
    @MessageMapping("/chat/{communityId}")
    @SendTo("/topic/community/{communityId}")
    public Map<String, Object> sendMessage(@DestinationVariable Long communityId,
                                           SendMessageRequest messageRequest) {
        logger.info("Received WebSocket message for community: {}", communityId);

        try {
            Optional<LearningCommunity> communityOpt = communityRepository.findById(communityId);
            Optional<Person> senderOpt = personRepository.findById(messageRequest.getSenderId());

            if (communityOpt.isEmpty() || senderOpt.isEmpty()) {
                logger.warn("Community or sender not found");
                return createErrorResponse("Comunidad o remitente no encontrado");
            }

            CommunityMessage message = new CommunityMessage();
            message.setLearningCommunity(communityOpt.get());
            message.setSender(senderOpt.get());
            message.setContent(messageRequest.getContent());

            CommunityMessage savedMessage = messageRepository.save(message);
            logger.info("Message saved with ID: {}", savedMessage.getId());

            return createMessageResponse(savedMessage);
        } catch (Exception e) {
            logger.error("Error sending message", e);
            return createErrorResponse("Error al enviar el mensaje");
        }
    }
    //#endregion

    //#region REST Endpoints
    /**
     * Obtiene todos los mensajes de una comunidad.
     *
     * @param communityId ID de la comunidad
     * @param userDetails usuario autenticado
     * @return lista de mensajes
     */
    @GetMapping("/communities/{communityId}/messages")
    @ResponseBody
    public ResponseEntity<?> getMessages(@PathVariable Long communityId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Getting messages for community: {}", communityId);

        try {
            Optional<LearningCommunity> communityOpt = communityRepository.findById(communityId);
            if (communityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorMap("Comunidad no encontrada"));
            }

            List<CommunityMessage> messages = messageRepository
                    .findByCommunityIdOrderBySentDateAsc(communityId);

            List<Map<String, Object>> messageResponses = new ArrayList<>();
            for (CommunityMessage message : messages) {
                messageResponses.add(createMessageResponse(message));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", messageResponses);
            response.put("count", messageResponses.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorMap("Error al obtener los mensajes"));
        }
    }

    /**
     * Obtiene los últimos N mensajes de una comunidad.
     *
     * @param communityId ID de la comunidad
     * @param limit número de mensajes a obtener (por defecto 50)
     * @param userDetails usuario autenticado
     * @return lista de mensajes
     */
    @GetMapping("/communities/{communityId}/messages/recent")
    @ResponseBody
    public ResponseEntity<?> getRecentMessages(@PathVariable Long communityId,
                                               @RequestParam(defaultValue = "50") int limit,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Getting recent messages for community: {}, limit: {}", communityId, limit);

        try {
            List<CommunityMessage> messages = messageRepository
                    .findLastMessagesByCommunityId(communityId, limit);

            Collections.reverse(messages);

            List<Map<String, Object>> messageResponses = new ArrayList<>();
            for (CommunityMessage message : messages) {
                messageResponses.add(createMessageResponse(message));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", messageResponses);
            response.put("count", messageResponses.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting recent messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorMap("Error al obtener los mensajes recientes"));
        }
    }
    //#endregion

    //#region Private Methods
    /**
     * Crea un mapa de respuesta con los datos del mensaje.
     *
     * @param message mensaje a convertir
     * @return mapa con datos del mensaje
     */
    private Map<String, Object> createMessageResponse(CommunityMessage message) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", message.getId());
        response.put("content", message.getContent());
        response.put("sentDate", message.getSentDate());
        response.put("edited", message.getEdited());

        Map<String, Object> senderData = new HashMap<>();
        senderData.put("id", message.getSender().getId());
        senderData.put("fullName", message.getSender().getFullName());
        senderData.put("profilePhotoUrl", message.getSender().getProfilePhotoUrl());
        senderData.put("email", message.getSender().getEmail());

        response.put("sender", senderData);
        response.put("success", true);

        return response;
    }

    /**
     * Crea un mapa de respuesta de error.
     *
     * @param errorMessage mensaje de error
     * @return mapa con el error
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", errorMessage);
        return response;
    }

    /**
     * Crea un mapa de error para respuestas REST.
     *
     * @param message mensaje de error
     * @return mapa con el error
     */
    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
    //#endregion

    //#region Request Classes
    /**
     * Clase para el request de enviar mensaje.
     */
    public static class SendMessageRequest {
        private Long senderId;
        private String content;

        public Long getSenderId() {
            return senderId;
        }

        public void setSenderId(Long senderId) {
            this.senderId = senderId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
    //#endregion
}