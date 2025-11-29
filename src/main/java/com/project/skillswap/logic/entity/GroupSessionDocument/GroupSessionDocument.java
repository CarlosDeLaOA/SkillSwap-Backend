package com.project.skillswap.logic.entity.GroupSessionDocument;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un documento almacenado por grupo/sesión.
 * Organizado por fecha y sesión con límite de almacenamiento por grupo (100MB).
 * Solo permite archivos PDF.
 */
@Entity
@Table(name = "group_session_document", indexes = {
        @Index(name = "idx_gsd_community", columnList = "learning_community_id"),
        @Index(name = "idx_gsd_session", columnList = "learning_session_id"),
        @Index(name = "idx_gsd_session_date", columnList = "session_date DESC"),
        @Index(name = "idx_gsd_upload_date", columnList = "upload_date DESC")
})
public class GroupSessionDocument {

    //#region Fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_community_id", nullable = false)
    @JsonIgnoreProperties({"members", "documents", "bookings", "creator", "hibernateLazyInitializer", "handler"})
    private LearningCommunity learningCommunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_session_id", nullable = true)
    @JsonIgnoreProperties({"bookings", "instructor", "hibernateLazyInitializer", "handler"})
    private LearningSession learningSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "googleOauthId", "emailVerified", "instructor", "learner", "hibernateLazyInitializer", "handler"})
    private Person uploadedBy;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize; // en bytes

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @CreationTimestamp
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // CAMPOS PARA HISTORIAL DE BORRADO
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    @JsonIgnoreProperties({"passwordHash", "googleOauthId", "emailVerified", "instructor", "learner", "hibernateLazyInitializer", "handler"})
    private Person deletedBy;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;
    //#endregion

    //#region Constructors
    public GroupSessionDocument() {
    }

    public GroupSessionDocument(LearningCommunity learningCommunity, LearningSession learningSession,
                                Person uploadedBy, String fileName, String originalFileName,
                                String filePath, Long fileSize, String contentType,
                                LocalDateTime sessionDate) {
        this.learningCommunity = learningCommunity;
        this.learningSession = learningSession;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.sessionDate = sessionDate;
        this.active = true;
    }
    //#endregion

    //#region Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LearningCommunity getLearningCommunity() {
        return learningCommunity;
    }

    public void setLearningCommunity(LearningCommunity learningCommunity) {
        this.learningCommunity = learningCommunity;
    }

    public LearningSession getLearningSession() {
        return learningSession;
    }

    public void setLearningSession(LearningSession learningSession) {
        this.learningSession = learningSession;
    }

    public Person getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Person uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDateTime sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Person getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(Person deletedBy) {
        this.deletedBy = deletedBy;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    public void setDeletionReason(String deletionReason) {
        this.deletionReason = deletionReason;
    }
    //#endregion

    //#region Helper Methods
    /**
     * Retorna el tamaño del archivo en formato legible (KB, MB).
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * Verifica si el documento es un PDF válido.
     */
    public boolean isPdf() {
        return "application/pdf".equalsIgnoreCase(contentType);
    }
    // Verifica si es material de apoyo (sin sesión asociada)
    public boolean isSupportMaterial() {
        return learningSession == null;
    }
    //#endregion

    //#region ToString
    @Override
    public String toString() {
        return "GroupSessionDocument{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", originalFileName='" + originalFileName + '\'' +
                ", fileSize=" + getFormattedFileSize() +
                ", sessionDate=" + sessionDate +
                ", uploadDate=" + uploadDate +
                ", active=" + active +
                '}';
    }
    //#endregion
}