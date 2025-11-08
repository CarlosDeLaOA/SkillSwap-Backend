package com.project.skillswap.logic.entity.Instructor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;

@Table(name = "instructor", indexes = {
        @Index(name = "idx_instructor_person", columnList = "person_id", unique = true)
})
@Entity
public class Instructor {

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false, unique = true)
    @JsonIgnore   // 👈 evita Person -> Instructor -> Person -> ...
    private Person person;

    @Column(name = "paypal_account", length = 255)
    private String paypalAccount;

    @Column(name = "skillcoins_balance", precision = 10, scale = 2)
    private BigDecimal skillcoinsBalance = BigDecimal.ZERO;

    @Column(name = "verified_account")
    private Boolean verifiedAccount = false;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "sessions_taught")
    private Integer sessionsTaught = 0;

    @Column(name = "total_earnings", precision = 10, scale = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore   // 👈 evita Instructor -> Sessions -> Instructor -> ...
    private List<LearningSession> learningSessions;

    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Instructor() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
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

    public String getPaypalAccount() {
        return paypalAccount;
    }

    public void setPaypalAccount(String paypalAccount) {
        this.paypalAccount = paypalAccount;
    }

    public BigDecimal getSkillcoinsBalance() {
        return skillcoinsBalance;
    }

    public void setSkillcoinsBalance(BigDecimal skillcoinsBalance) {
        this.skillcoinsBalance = skillcoinsBalance;
    }

    public Boolean getVerifiedAccount() {
        return verifiedAccount;
    }

    public void setVerifiedAccount(Boolean verifiedAccount) {
        this.verifiedAccount = verifiedAccount;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getSessionsTaught() {
        return sessionsTaught;
    }

    public void setSessionsTaught(Integer sessionsTaught) {
        this.sessionsTaught = sessionsTaught;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(BigDecimal totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public List<LearningSession> getLearningSessions() {
        return learningSessions;
    }

    public void setLearningSessions(List<LearningSession> learningSessions) {
        this.learningSessions = learningSessions;
    }
    //</editor-fold>
}