package com.project.skillswap.logic.entity.CollaborativeDocument;

/**
 * Mensaje WebSocket para actualizaciones de documento en tiempo real
 * Se usa para comunicación bidireccional entre cliente y servidor
 */
public class DocumentMessage {

    private String documentId;
    private String content;
    private Long version;
    private String userId;
    private String userName;
    private String action; // "UPDATE", "CURSOR_MOVE", "USER_JOIN", "USER_LEAVE"
    private Long timestamp;

    // Constructor vacío
    public DocumentMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor con parámetros
    public DocumentMessage(String documentId, String content, Long version,
                           String userId, String userName, String action) {
        this.documentId = documentId;
        this.content = content;
        this.version = version;
        this.userId = userId;
        this.userName = userName;
        this.action = action;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getDocumentId() {
        return documentId;
    }

    public String getContent() {
        return content;
    }

    public Long getVersion() {
        return version;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getAction() {
        return action;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DocumentMessage{" +
                "documentId='" + documentId + '\'' +
                ", version=" + version +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}