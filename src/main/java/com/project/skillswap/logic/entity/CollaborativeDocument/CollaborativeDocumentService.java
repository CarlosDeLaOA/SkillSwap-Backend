
package com.project.skillswap.logic.entity.CollaborativeDocument;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class CollaborativeDocumentService {
    private static final Logger logger = LoggerFactory.getLogger(CollaborativeDocumentService.class);

    //#region Dependencies
    @Autowired
    private CollaborativeDocumentRepository documentRepository;

    @Autowired
    private LearningSessionRepository sessionRepository;

    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10MB
    //#endregion

    //#region Document Operations

    /**
     * Creates or retrieves a collaborative document for a session
     */
    @Transactional
    public DocumentResponse getOrCreateDocument(Long sessionId) {
        logger.info("Getting or creating document for session: " + sessionId);

        Optional<CollaborativeDocument> existingDoc = documentRepository.findBySessionId(sessionId);

        if (existingDoc.isPresent()) {
            CollaborativeDocument doc = existingDoc.get();

            if (doc.isExpired()) {
                logger.info("Document expired, creating new one");
                documentRepository.delete(doc);
            } else {
                logger.info("Existing document found");
                return mapToResponse(doc);
            }
        }

        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        CollaborativeDocument newDoc = new CollaborativeDocument();
        newDoc.setSession(session);
        newDoc.setDocumentId("doc_" + UUID.randomUUID().toString());
        newDoc.setContent("");
        newDoc.setVersion(0L);
        newDoc.setSizeInBytes(0L);
        newDoc.setIsActive(true);
        newDoc.setExpiresAt(LocalDateTime.now().plusHours(24));

        CollaborativeDocument saved = documentRepository.save(newDoc);
        logger.info("New document created: " + saved.getDocumentId());

        return mapToResponse(saved);
    }

    /**
     * Updates document content
     * NOTE: Version validation disabled to allow collaborative editing
     */
    @Transactional
    public DocumentResponse updateDocument(String documentId, String content, Long expectedVersion) {
        logger.info("Updating document: " + documentId);

        CollaborativeDocument document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (document.isExpired()) {
            throw new RuntimeException("Document has expired");
        }

        // VERSION VALIDATION DISABLED - Accept all changes
        // if (expectedVersion != null && !document.getVersion().equals(expectedVersion)) {
        //     throw new RuntimeException("Version conflict. Document was modified by another user.");
        // }

        long newSize = content.getBytes().length;
        if (newSize > MAX_DOCUMENT_SIZE) {
            throw new RuntimeException("Document exceeds maximum allowed size (10MB)");
        }

        document.setContent(content);
        document.setVersion(document.getVersion() + 1);
        document.updateSize();

        CollaborativeDocument updated = documentRepository.save(document);
        logger.info("Document updated - Version: " + updated.getVersion());

        return mapToResponse(updated);
    }

    /**
     * Gets a document by its ID
     */
    public DocumentResponse getDocumentByDocumentId(String documentId) {
        CollaborativeDocument document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (document.isExpired()) {
            throw new RuntimeException("Document has expired");
        }

        return mapToResponse(document);
    }

    /**
     * Gets a document by session ID
     */
    public DocumentResponse getDocumentBySessionId(Long sessionId) {
        CollaborativeDocument document = documentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Document not found for this session"));

        if (document.isExpired()) {
            throw new RuntimeException("Document has expired");
        }

        return mapToResponse(document);
    }

    /**
     * Deactivates a document (when session ends)
     */
    @Transactional
    public void deactivateDocument(Long sessionId) {
        Optional<CollaborativeDocument> docOpt = documentRepository.findBySessionId(sessionId);

        if (docOpt.isPresent()) {
            CollaborativeDocument doc = docOpt.get();
            doc.setIsActive(false);
            documentRepository.save(doc);
            logger.info("Document deactivated for session: " + sessionId);
        }
    }

    //#endregion

    //#region Scheduled Tasks

    /**
     * Deletes expired documents (scheduled task)
     * Runs every hour (3600000 ms)
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredDocuments() {
        logger.info("Cleaning up expired documents...");
        int deleted = documentRepository.deleteExpiredDocuments(LocalDateTime.now());
        logger.info("Documents deleted: " + deleted);
    }

    //#endregion

    //#region Private Methods

    /**
     * Maps entity to response DTO
     */
    private DocumentResponse mapToResponse(CollaborativeDocument document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setDocumentId(document.getDocumentId());
        response.setSessionId(document.getSession().getId());
        response.setContent(document.getContent());
        response.setVersion(document.getVersion());
        response.setSizeInBytes(document.getSizeInBytes());
        response.setIsActive(document.getIsActive());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setExpiresAt(document.getExpiresAt());
        return response;
    }

    //#endregion
}