package com.project.skillswap.logic.entity.CollaborativeDocument;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "collaborative_documents")
public class CollaborativeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private LearningSession session;

    @Column(nullable = false, unique = true, length = 100)
    private String documentId;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false)
    private Long sizeInBytes = 0L;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime expiresAt;

    // Constructores
    public CollaborativeDocument() {
    }

    public CollaborativeDocument(Long id, LearningSession session, String documentId, String content,
                                 Long version, Long sizeInBytes, Boolean isActive,
                                 LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime expiresAt) {
        this.id = id;
        this.session = session;
        this.documentId = documentId;
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

    public LearningSession getSession() {
        return session;
    }

    public String getDocumentId() {
        return documentId;
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

    public void setSession(LearningSession session) {
        this.session = session;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
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

    // Métodos helper
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void updateSize() {
        this.sizeInBytes = content != null ? content.getBytes().length : 0L;
    }

    @Override
    public String toString() {
        return "CollaborativeDocument{" +
                "id=" + id +
                ", documentId='" + documentId + '\'' +
                ", version=" + version +
                ", sizeInBytes=" + sizeInBytes +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", expiresAt=" + expiresAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollaborativeDocument that = (CollaborativeDocument) o;
        return Objects.equals(id, that.id) && Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, documentId);
    }
}