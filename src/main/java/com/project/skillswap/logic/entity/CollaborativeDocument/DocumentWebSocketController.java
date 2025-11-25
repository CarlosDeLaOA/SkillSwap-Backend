package com.project.skillswap.logic.entity.CollaborativeDocument;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket Controller for real-time collaborative document editing
 */
@Controller
public class DocumentWebSocketController {

    //#region Dependencies
    @Autowired
    private CollaborativeDocumentService documentService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    //#endregion

    //#region Message Handlers

    /**
     * Handles document update messages
     * Broadcasts changes to all connected clients
     */
    @MessageMapping("/document/{documentId}/update")
    @SendTo("/topic/document/{documentId}")
    public DocumentMessage handleDocumentUpdate(
            @DestinationVariable String documentId,
            DocumentMessage message) {

        try {
            System.out.println("[WebSocket] Update received");
            System.out.println("            Document ID: " + documentId);
            System.out.println("            User: " + message.getUserName());
            System.out.println("            Version: " + message.getVersion());

            // Save without version validation to allow collaborative editing
            if ("UPDATE".equals(message.getAction())) {
                try {
                    DocumentResponse updated = documentService.updateDocument(
                            documentId,
                            message.getContent(),
                            message.getVersion()
                    );

                    message.setVersion(updated.getVersion());
                    System.out.println("            Document saved - Version: " + updated.getVersion());

                } catch (Exception e) {
                    // Ignore version errors and broadcast message anyway
                    System.out.println("            Save error (ignored): " + e.getMessage());
                }
            }

            // Always broadcast (even if save fails)
            return message;

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();

            // Send UPDATE anyway instead of ERROR
            return message;
        }
    }

    /**
     * Handles user join messages
     * Notifies all clients when a user joins the document
     */
    @MessageMapping("/document/{documentId}/join")
    @SendTo("/topic/document/{documentId}")
    public DocumentMessage handleUserJoin(
            @DestinationVariable String documentId,
            DocumentMessage message) {

        System.out.println("[WebSocket] User joined");
        System.out.println("            Document ID: " + documentId);
        System.out.println("            User: " + message.getUserName());

        message.setAction("USER_JOIN");
        message.setDocumentId(documentId);

        return message;
    }

    /**
     * Handles user leave messages
     * Notifies all clients when a user leaves the document
     */
    @MessageMapping("/document/{documentId}/leave")
    @SendTo("/topic/document/{documentId}")
    public DocumentMessage handleUserLeave(
            @DestinationVariable String documentId,
            DocumentMessage message) {

        System.out.println("[WebSocket] User left");
        System.out.println("            Document ID: " + documentId);
        System.out.println("            User: " + message.getUserName());

        message.setAction("USER_LEAVE");
        message.setDocumentId(documentId);

        return message;
    }

    /**
     * Handles cursor movement messages
     * Broadcasts cursor positions to enable real-time cursor tracking
     * Note: No logging to avoid console spam (cursors move constantly)
     */
    @MessageMapping("/document/{documentId}/cursor")
    @SendTo("/topic/document/{documentId}")
    public DocumentMessage handleCursorMove(
            @DestinationVariable String documentId,
            DocumentMessage message) {

        message.setAction("CURSOR_MOVE");
        message.setDocumentId(documentId);

        return message;
    }

    //#endregion

    //#region Broadcast Methods

    /**
     * Broadcasts a message to all clients subscribed to a document
     */
    public void broadcastToDocument(String documentId, DocumentMessage message) {
        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
        System.out.println("Broadcast sent to /topic/document/" + documentId);
    }

    //#endregion
}