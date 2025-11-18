package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;

@Table(name = "booking", indexes = {
        @Index(name = "idx_booking_session", columnList = "learning_session_id"),
        @Index(name = "idx_booking_community", columnList = "community_id"),
        @Index(name = "idx_booking_learner", columnList = "learner_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_booking_session_learner", columnNames = {"learning_session_id", "learner_id"})
})
@Entity
public class Booking {

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_session_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"bookings", "instructor", "skill"})
    private LearningSession learningSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"bookings", "person"})
    private Learner learner;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private BookingType type = BookingType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(name = "access_link", length = 500)
    private String accessLink;

    @Column(name = "attended")
    private Boolean attended = false;

    @Column(name = "entry_time")
    private Date entryTime;

    @Column(name = "exit_time")
    private Date exitTime;

    @CreationTimestamp
    @Column(updatable = false, name = "booking_date")
    private Date bookingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"bookings", "members", "documents", "creator"})
    private LearningCommunity community;    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Booking() {}
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

    public BookingType getType() {
        return type;
    }

    public void setType(BookingType type) {
        this.type = type;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getAccessLink() {
        return accessLink;
    }

    public void setAccessLink(String accessLink) {
        this.accessLink = accessLink;
    }

    public Boolean getAttended() {
        return attended;
    }

    public void setAttended(Boolean attended) {
        this.attended = attended;
    }

    public Date getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(Date entryTime) {
        this.entryTime = entryTime;
    }

    public Date getExitTime() {
        return exitTime;
    }

    public void setExitTime(Date exitTime) {
        this.exitTime = exitTime;
    }

    public Date getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(Date bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LearningCommunity getCommunity() {
        return community;
    }

    public void setCommunity(LearningCommunity community) {
        this.community = community;
    }
    //</editor-fold>
}