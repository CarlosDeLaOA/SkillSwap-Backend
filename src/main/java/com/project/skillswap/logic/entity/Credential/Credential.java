package com.project.skillswap.logic.entity.Credential;

import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Quiz.Quiz;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.util.Date;

@Table(name = "credential", indexes = {
        @Index(name = "idx_credential_learner", columnList = "learner_id"),
        @Index(name = "idx_credential_skill", columnList = "skill_id"),
        @Index(name = "idx_credential_learner_skill", columnList = "learner_id, skill_id")
})
@Entity
public class Credential {

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", referencedColumnName = "id", nullable = false)
    private Learner learner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", referencedColumnName = "id", nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_session_id", referencedColumnName = "id", nullable = false)
    private LearningSession learningSession;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", referencedColumnName = "id", nullable = false)
    private Quiz quiz;

    @Column(name = "percentage_achieved", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageAchieved;

    @Column(name = "badge_url", length = 500)
    private String badgeUrl;

    @CreationTimestamp
    @Column(updatable = false, name = "obtained_date")
    private Date obtainedDate;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Credential() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Learner getLearner() {
        return learner;
    }

    public void setLearner(Learner learner) {
        this.learner = learner;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public LearningSession getLearningSession() {
        return learningSession;
    }

    public void setLearningSession(LearningSession learningSession) {
        this.learningSession = learningSession;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public BigDecimal getPercentageAchieved() {
        return percentageAchieved;
    }

    public void setPercentageAchieved(BigDecimal percentageAchieved) {
        this.percentageAchieved = percentageAchieved;
    }

    public String getBadgeUrl() {
        return badgeUrl;
    }

    public void setBadgeUrl(String badgeUrl) {
        this.badgeUrl = badgeUrl;
    }

    public Date getObtainedDate() {
        return obtainedDate;
    }

    public void setObtainedDate(Date obtainedDate) {
        this.obtainedDate = obtainedDate;
    }
    //</editor-fold>
}