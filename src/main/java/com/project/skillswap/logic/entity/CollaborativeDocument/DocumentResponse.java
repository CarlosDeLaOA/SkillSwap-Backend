
package com.project.skillswap.logic.entity.CollaborativeDocument;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.time.LocalDateTime;

public class DocumentResponse {
    private static final Logger logger = LoggerFactory.getLogger(DocumentResponse.class);
    private Long id;
    private String documentId;
    private Long sessionId;
    private String content;
    private Long version;
    private Long sizeInBytes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    // Constructor vacío
    public DocumentResponse() {
    }

    // Constructor con parámetros
    public DocumentResponse(Long id, String documentId, Long sessionId, String content,
                            Long version, Long sizeInBytes, Boolean isActive,
                            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime expiresAt) {
        this.id = id;
        this.documentId = documentId;
        this.sessionId = sessionId;
        this.content = content;
        this.version = version;
        this.sizeInBytes = sizeInBytes;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getContent() {
        return content;
    }

    public Long getVersion() {
        return version;
    }

    public Long getSizeInBytes() {
        return sizeInBytes;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setSizeInBytes(Long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "DocumentResponse{" +
                "id=" + id +
                ", documentId='" + documentId + '\'' +
                ", sessionId=" + sessionId +
                ", version=" + version +
                ", sizeInBytes=" + sizeInBytes +
                ", isActive=" + isActive +
                '}';
    }
}