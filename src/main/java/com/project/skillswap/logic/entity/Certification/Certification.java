
package com.project.skillswap.logic.entity.Certification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Skill.Skill;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Table(name = "certification", indexes = {
        @Index(name = "idx_certification_code", columnList = "verification_code", unique = true)
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_certification_learner_skill", columnNames = {"learner_id", "skill_id"})
})
@Entity
public class Certification {
    private static final Logger logger = LoggerFactory.getLogger(Certification.class);

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

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "accumulated_credentials")
    private Integer accumulatedCredentials = 10;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "verification_code", unique = true, length = 100)
    private String verificationCode;

    @CreationTimestamp
    @Column(updatable = false, name = "issue_date")
    private Date issueDate;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Certification() {}
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAccumulatedCredentials() {
        return accumulatedCredentials;
    }

    public void setAccumulatedCredentials(Integer accumulatedCredentials) {
        this.accumulatedCredentials = accumulatedCredentials;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }
    //</editor-fold>
}