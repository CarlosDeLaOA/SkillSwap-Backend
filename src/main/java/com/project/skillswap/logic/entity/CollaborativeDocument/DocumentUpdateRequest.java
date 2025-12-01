
package com.project.skillswap.logic.entity.CollaborativeDocument;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
public class DocumentUpdateRequest {
    private String documentId;
    private String content;
    private Long version;

    // Constructor vacío
    public DocumentUpdateRequest() {
    }

    // Constructor con parámetros
    public DocumentUpdateRequest(String documentId, String content, Long version) {
        this.documentId = documentId;
        this.content = content;
        this.version = version;
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

    @Override
    public String toString() {
        return "DocumentUpdateRequest{" +
                "documentId='" + documentId + '\'' +
                ", version=" + version +
                '}';
    }
}