package com.project.skillswap.logic.entity.Feedback;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Learner.Learner;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Table(name = "feedback", indexes = {
        @Index(name = "idx_feedback_session", columnList = "learning_session_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_feedback_session_learner", columnNames = {"learning_session_id", "learner_id"})
})
@Entity
public class Feedback {
    private static final Logger logger = LoggerFactory.getLogger(Feedback.class);

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_session_id", referencedColumnName = "id", nullable = false)
    private LearningSession learningSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", referencedColumnName = "id", nullable = false)
    private Learner learner;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "audio_transcription", columnDefinition = "TEXT")
    private String audioTranscription;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "processing_date")
    private LocalDateTime processingDate;

    @CreationTimestamp
    @Column(updatable = false, name = "creation_date")
    private Date creationDate;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Feedback() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LearningSession getLearningSession() {
        return learningSession;
    }

    public void setLearningSession(LearningSession learningSession) {
        this.learningSession = learningSession;
    }

    public Learner getLearner() {
        return learner;
    }

    public void setLearner(Learner learner) {
        this.learner = learner;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getAudioTranscription() {
        return audioTranscription;
    }

    public void setAudioTranscription(String audioTranscription) {
        this.audioTranscription = audioTranscription;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public LocalDateTime getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(LocalDateTime processingDate) {
        this.processingDate = processingDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    //</editor-fold>
}