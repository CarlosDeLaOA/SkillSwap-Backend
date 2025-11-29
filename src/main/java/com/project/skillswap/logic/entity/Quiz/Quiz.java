package com.project.skillswap.logic.entity.Quiz;

import com.project.skillswap.logic.entity.Transcription.Transcription;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Question.Question;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.List;

/**
 * Entidad que representa un cuestionario de evaluación
 * generado a partir de una sesión de aprendizaje
 */
@Table(name = "quiz", indexes = {
        @Index(name = "idx_quiz_session", columnList = "learning_session_id"),
        @Index(name = "idx_quiz_learner", columnList = "learning_session_id, learner_id"),
        @Index(name = "idx_quiz_status", columnList = "status")
})
@Entity
public class Quiz {

    //#region Fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_session_id", referencedColumnName = "id", nullable = false)
    private LearningSession learningSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", referencedColumnName = "id", nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", referencedColumnName = "id", nullable = false)
    private Learner learner;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private QuizStatus status = QuizStatus.IN_PROGRESS;

    @Column(name = "score_obtained")
    private Integer scoreObtained;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "json")
    private String optionsJson;

    @Column(name = "passed")
    private Boolean passed;

    @CreationTimestamp
    @Column(updatable = false, name = "creation_date")
    private Date creationDate;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private Date lastUpdated;

    @Column(name = "completion_date")
    private Date completionDate;

    @JsonManagedReference
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Question> questions;
    //#endregion

    //#region Constructors
    public Quiz() {}
    //#endregion

    //#region Getters and Setters
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

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public Learner getLearner() {
        return learner;
    }

    public void setLearner(Learner learner) {
        this.learner = learner;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public QuizStatus getStatus() {
        return status;
    }

    public void setStatus(QuizStatus status) {
        this.status = status;
    }

    public Integer getScoreObtained() {
        return scoreObtained;
    }

    public void setScoreObtained(Integer scoreObtained) {
        this.scoreObtained = scoreObtained;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
    //#endregion

    //#region Business Methods
    /**
     * Calcula el porcentaje de respuestas correctas
     *
     * @return porcentaje de aciertos (0-100)
     */
    @Transient
    public Double getPercentageScore() {
        if (questions == null || questions.isEmpty()) {
            return 0.0;
        }
        long correctAnswers = questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsCorrect()))
                .count();
        return (correctAnswers * 100.0) / questions.size();
    }

    /**
     * Verifica si el cuestionario fue aprobado
     *
     * @return true si el puntaje es mayor o igual al 70%
     */
    @Transient
    public Boolean isPassingScore() {
        return getPercentageScore() >= 70.0;
    }
    //#endregion
}