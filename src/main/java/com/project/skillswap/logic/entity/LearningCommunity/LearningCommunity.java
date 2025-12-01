
package com.project.skillswap.logic.entity.LearningCommunity;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMember;
import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocument;
import com.project.skillswap.logic.entity.Booking.Booking;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.List;

@Table(name = "learning_community", indexes = {
        @Index(name = "idx_learning_community_code", columnList = "invitation_code", unique = true),
        @Index(name = "idx_learning_community_creator", columnList = "creator_id")
})
@Entity
public class LearningCommunity {
    private static final Logger logger = LoggerFactory.getLogger(LearningCommunity.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "person", "bookings"})
    private Learner creator;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_members")
    private Integer maxMembers = 10;

    @Column(name = "invitation_code", unique = true, length = 50)
    private String invitationCode;

    @Column(name = "active")
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false, name = "creation_date")
    private Date creationDate;

    @OneToMany(mappedBy = "learningCommunity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"learningCommunity", "learner", "hibernateLazyInitializer", "handler"})
    private List<CommunityMember> members;

    @OneToMany(mappedBy = "learningCommunity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"learningCommunity", "hibernateLazyInitializer", "handler"})
    private List<CommunityDocument> documents;

    @OneToMany(mappedBy = "community", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"community", "learningSession", "learner", "hibernateLazyInitializer", "handler"})
    private List<Booking> bookings;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public LearningCommunity() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Learner getCreator() {
        return creator;
    }

    public void setCreator(Learner creator) {
        this.creator = creator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public List<CommunityMember> getMembers() {
        return members;
    }

    public void setMembers(List<CommunityMember> members) {
        this.members = members;
    }

    public List<CommunityDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<CommunityDocument> documents) {
        this.documents = documents;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }
    //</editor-fold>
}