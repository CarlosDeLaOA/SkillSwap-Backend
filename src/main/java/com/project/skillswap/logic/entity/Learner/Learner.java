package com.project.skillswap.logic.entity.Learner;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Credential.Credential;
import com.project.skillswap.logic.entity.Certification.Certification;
import com.project.skillswap.logic.entity.Feedback.Feedback;
import com.project.skillswap.logic.entity.Quiz.Quiz;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMember;
import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocument;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;

@Table(name = "learner", indexes = {
        @Index(name = "idx_learner_person", columnList = "person_id", unique = true)
})
@Entity
public class Learner {

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false, unique = true)
    private Person person;

    @Column(name = "skillcoins_balance", precision = 10, scale = 2)
    private BigDecimal skillcoinsBalance = BigDecimal.ZERO;

    @Column(name = "completed_sessions")
    private Integer completedSessions = 0;

    @Column(name = "credentials_obtained")
    private Integer credentialsObtained = 0;

    @OneToMany(mappedBy = "learner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Booking> bookings;

    @OneToMany(mappedBy = "learner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Credential> credentials;

    @OneToMany(mappedBy = "learner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Certification> certifications;

    @OneToMany(mappedBy = "learner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Feedback> feedbacks;

    @OneToMany(mappedBy = "learner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Quiz> quizzes;

    @OneToMany(mappedBy = "learner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CommunityMember> communityMemberships;

    @OneToMany(mappedBy = "uploadedBy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CommunityDocument> uploadedDocuments;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Learner() {}
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

    public BigDecimal getSkillcoinsBalance() {
        return skillcoinsBalance;
    }

    public void setSkillcoinsBalance(BigDecimal skillcoinsBalance) {
        this.skillcoinsBalance = skillcoinsBalance;
    }

    public Integer getCompletedSessions() {
        return completedSessions;
    }

    public void setCompletedSessions(Integer completedSessions) {
        this.completedSessions = completedSessions;
    }

    public Integer getCredentialsObtained() {
        return credentialsObtained;
    }

    public void setCredentialsObtained(Integer credentialsObtained) {
        this.credentialsObtained = credentialsObtained;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }

    public List<Credential> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<Credential> credentials) {
        this.credentials = credentials;
    }

    public List<Certification> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<Certification> certifications) {
        this.certifications = certifications;
    }

    public List<Feedback> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }

    public List<CommunityMember> getCommunityMemberships() {
        return communityMemberships;
    }

    public void setCommunityMemberships(List<CommunityMember> communityMemberships) {
        this.communityMemberships = communityMemberships;
    }

    public List<CommunityDocument> getUploadedDocuments() {
        return uploadedDocuments;
    }

    public void setUploadedDocuments(List<CommunityDocument> uploadedDocuments) {
        this.uploadedDocuments = uploadedDocuments;
    }
    //</editor-fold>
}