package com.project.skillswap.logic.entity.LearningSession;

import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Classnote.ClassNote;
import com.project.skillswap.logic.entity.Attendancerecord.AttendanceRecord;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Table(name = "learning_session", indexes = {
        @Index(name = "idx_learning_session_instructor", columnList = "instructor_id"),
        @Index(name = "idx_learning_session_skill", columnList = "skill_id"),
        @Index(name = "idx_learning_session_datetime", columnList = "scheduled_datetime"),
        @Index(name = "idx_learning_session_status", columnList = "status"),
        @Index(name = "idx_learning_session_status_datetime", columnList = "status, scheduled_datetime")
})
@Entity
public class LearningSession {

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", referencedColumnName = "id", nullable = false)
    private Instructor instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", referencedColumnName = "id", nullable = false)
    private Skill skill;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_datetime", nullable = false)
    private Date scheduledDatetime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private SessionType type = SessionType.SCHEDULED;

    @Column(name = "max_capacity")
    private Integer maxCapacity = 10;

    @Column(name = "is_premium")
    private Boolean isPremium = false;

    @Column(name = "skillcoins_cost", precision = 10, scale = 2)
    private BigDecimal skillcoinsCost = BigDecimal.ZERO;

    @Column(name = "language", length = 10)
    private String language = "en";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "video_call_link", length = 500)
    private String videoCallLink;

    @Column(name = "audio_recording_url", length = 500)
    private String audioRecordingUrl;

    @Column(name = "google_calendar_id", length = 255)
    private String googleCalendarId;

    @CreationTimestamp
    @Column(updatable = false, name = "creation_date")
    private Date creationDate;

    @OneToMany(mappedBy = "learningSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Booking> bookings;

    @OneToOne(mappedBy = "learningSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ClassNote classNote;

    @OneToOne(mappedBy = "learningSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AttendanceRecord attendanceRecord;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public LearningSession() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getScheduledDatetime() {
        return scheduledDatetime;
    }

    public void setScheduledDatetime(Date scheduledDatetime) {
        this.scheduledDatetime = scheduledDatetime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public SessionType getType() {
        return type;
    }

    public void setType(SessionType type) {
        this.type = type;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Boolean getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(Boolean isPremium) {
        this.isPremium = isPremium;
    }

    public BigDecimal getSkillcoinsCost() {
        return skillcoinsCost;
    }

    public void setSkillcoinsCost(BigDecimal skillcoinsCost) {
        this.skillcoinsCost = skillcoinsCost;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getVideoCallLink() {
        return videoCallLink;
    }

    public void setVideoCallLink(String videoCallLink) {
        this.videoCallLink = videoCallLink;
    }

    public String getAudioRecordingUrl() {
        return audioRecordingUrl;
    }

    public void setAudioRecordingUrl(String audioRecordingUrl) {
        this.audioRecordingUrl = audioRecordingUrl;
    }

    public String getGoogleCalendarId() {
        return googleCalendarId;
    }

    public void setGoogleCalendarId(String googleCalendarId) {
        this.googleCalendarId = googleCalendarId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }

    public ClassNote getClassNote() {
        return classNote;
    }

    public void setClassNote(ClassNote classNote) {
        this.classNote = classNote;
    }

    public AttendanceRecord getAttendanceRecord() {
        return attendanceRecord;
    }

    public void setAttendanceRecord(AttendanceRecord attendanceRecord) {
        this.attendanceRecord = attendanceRecord;
    }
    //</editor-fold>
}