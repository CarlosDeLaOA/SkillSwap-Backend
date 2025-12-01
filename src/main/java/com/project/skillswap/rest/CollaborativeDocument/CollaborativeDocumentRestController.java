
package com.project.skillswap.rest.CollaborativeDocument;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.CollaborativeDocument.CollaborativeDocumentService;
import com.project.skillswap.logic.entity.CollaborativeDocument.DocumentResponse;
import com.project.skillswap.logic.entity.CollaborativeDocument.DocumentUpdateRequest;
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
 * REST Controller for collaborative document operations
 */
@RestController
@RequestMapping("/api/collaborative-documents")
@CrossOrigin(origins = "http://localhost:4200")
public class CollaborativeDocumentRestController {
    private static final Logger logger = LoggerFactory.getLogger(CollaborativeDocumentRestController.class);

    //#region Dependencies
    @Autowired
    private CollaborativeDocumentService documentService;

    @Autowired
    private PersonRepository personRepository;
    //#endregion

    //#region Endpoints

    /**
     * Gets or creates a document for a session
     * GET /api/collaborative-documents/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getOrCreateDocument(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            logger.info("[GET] Getting document for session: " + sessionId);
            logger.info("     User: " + person.getFullName());

            DocumentResponse document = documentService.getOrCreateDocument(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document retrieved successfully");
            response.put("data", document);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("Error retrieving document: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving document: " + e.getMessage()));
        }
    }

    /**
     * Gets a document by its unique documentId
     * GET /api/collaborative-documents/{documentId}
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> getDocumentById(@PathVariable String documentId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            logger.info("[GET] Getting document: " + documentId);

            DocumentResponse document = documentService.getDocumentByDocumentId(documentId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document found");
            response.put("data", document);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.info("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.info("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving document"));
        }
    }

    /**
     * Updates document content
     * PUT /api/collaborative-documents/update
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateDocument(@RequestBody DocumentUpdateRequest request) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            // Validate request
            if (request.getDocumentId() == null || request.getDocumentId().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("documentId is required"));
            }

            if (request.getContent() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("content is required"));
            }

            logger.info("[PUT] Updating document: " + request.getDocumentId());
            logger.info("      User: " + person.getFullName());
            logger.info("      Expected version: " + request.getVersion());
            logger.info("      Content size: " + request.getContent().length() + " characters");

            DocumentResponse updated = documentService.updateDocument(
                    request.getDocumentId(),
                    request.getContent(),
                    request.getVersion()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document updated successfully");
            response.put("data", updated);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.info("Validation error: " + e.getMessage());

            // Determine status code based on error
            HttpStatus status = HttpStatus.BAD_REQUEST;
            if (e.getMessage().contains("not found") || e.getMessage().contains("no encontrado")) {
                status = HttpStatus.NOT_FOUND;
            } else if (e.getMessage().contains("expired") || e.getMessage().contains("expirado")) {
                status = HttpStatus.GONE;
            } else if (e.getMessage().contains("version") || e.getMessage().contains("versión") ||
                    e.getMessage().contains("Conflict") || e.getMessage().contains("Conflicto")) {
                status = HttpStatus.CONFLICT;
            }

            return ResponseEntity.status(status)
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            logger.info("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating document"));
        }
    }

    /**
     * Deactivates a document when session ends
     * POST /api/collaborative-documents/deactivate/{sessionId}
     */
    @PostMapping("/deactivate/{sessionId}")
    public ResponseEntity<Map<String, Object>> deactivateDocument(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            logger.info("[POST] Deactivating document for session: " + sessionId);
            logger.info("       User: " + person.getFullName());

            documentService.deactivateDocument(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document deactivated successfully");
            response.put("sessionId", sessionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("Error deactivating document: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error deactivating document"));
        }
    }

    /**
     * Health check endpoint
     * GET /api/collaborative-documents/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "collaborative-documents");
        response.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    //#endregion

    //#region Private Methods

    /**
     * Gets authenticated person from security context
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
     * Creates a standard error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("timestamp", java.time.LocalDateTime.now());
        return response;
    }

    //#endregion
}