package com.project.skillswap.logic.entity.CommunityMember;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.Learner.Learner;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Table(name = "community_member", indexes = {
        @Index(name = "idx_community_member_community", columnList = "learning_community_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_community_member_learner", columnNames = {"learning_community_id", "learner_id"})
})
@Entity
public class CommunityMember {
    private static final Logger logger = LoggerFactory.getLogger(CommunityMember.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_community_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"members", "documents", "bookings", "creator", "hibernateLazyInitializer", "handler"})
    private LearningCommunity learningCommunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"person", "bookings", "hibernateLazyInitializer", "handler"})
    private Learner learner;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private MemberRole role = MemberRole.MEMBER;

    @CreationTimestamp
    @Column(updatable = false, name = "join_date")
    private Date joinDate;

    @Column(name = "active")
    private Boolean active = true;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public CommunityMember() {}
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

    public Learner getLearner() {
        return learner;
    }

    public void setLearner(Learner learner) {
        this.learner = learner;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public Date getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
    //</editor-fold>
}