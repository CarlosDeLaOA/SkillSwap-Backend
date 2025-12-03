package com.project.skillswap.logic.entity.SessionSuggestion;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/*
 Entidad que representa una sugerencia personalizada de sesión para un usuario.
 Contiene el score, razón, estado de vista y timestamps. ***
*/
@Table(name = "session_suggestion", indexes = {
        @Index(name = "idx_suggestion_person", columnList = "person_id"),
        @Index(name = "idx_suggestion_session", columnList = "learning_session_id"),
        @Index(name = "idx_suggestion_viewed", columnList = "viewed")
})
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SessionSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_session_id", referencedColumnName = "id", nullable = false)
    private LearningSession learningSession;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "viewed")
    private Boolean viewed = false;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    @Column(name = "viewed_at")
    private Date viewedAt;

    public SessionSuggestion() {
        // default constructor
    }

    public SessionSuggestion(Person person, LearningSession learningSession, Double matchScore, String reason) {
        this.person = person;
        this.learningSession = learningSession;
        this.matchScore = matchScore;
        this.reason = reason;
        this.viewed = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public LearningSession getLearningSession() {
        return learningSession;
    }

    public void setLearningSession(LearningSession learningSession) {
        this.learningSession = learningSession;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getViewed() {
        return viewed;
    }

    public void setViewed(Boolean viewed) {
        this.viewed = viewed;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(Date viewedAt) {
        this.viewedAt = viewedAt;
    }
}