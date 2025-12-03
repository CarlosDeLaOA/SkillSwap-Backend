package com.project.skillswap.logic.entity.CommunityDocument;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Table(name = "community_document", indexes = {
        @Index(name = "idx_community_document_community", columnList = "learning_community_id"),
        @Index(name = "idx_community_document_session", columnList = "learning_session_id")
})
@Entity
public class CommunityDocument {
    private static final Logger logger = LoggerFactory.getLogger(CommunityDocument.class);

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_community_id", referencedColumnName = "id", nullable = false)
    private LearningCommunity learningCommunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_session_id", referencedColumnName = "id")
    private LearningSession learningSession;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "resource_url", nullable = false, length = 500)
    private String resourceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", referencedColumnName = "id")
    private Learner uploadedBy;

    @CreationTimestamp
    @Column(updatable = false, name = "upload_date")
    private Date uploadDate;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by", referencedColumnName = "id")
    private Person deletedBy;

    @Column(name = "deleted_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deletedAt;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public CommunityDocument() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public Learner getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Learner uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Person getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(Person deletedBy) {
        this.deletedBy = deletedBy;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    public void setDeletionReason(String deletionReason) {
        this.deletionReason = deletionReason;
    }
    //</editor-fold>
}