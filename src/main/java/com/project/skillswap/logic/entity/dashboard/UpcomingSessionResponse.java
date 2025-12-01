package com.project.skillswap.logic.entity.dashboard;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.Date;

/**
 * Response containing information about an upcoming session
 */
public class UpcomingSessionResponse {
    private static final Logger logger = LoggerFactory.getLogger(UpcomingSessionResponse.class);

    //#region Fields
    private Long id;
    private String title;
    private String description;
    private Date scheduledDatetime;
    private Integer durationMinutes;
    private String status;
    private String videoCallLink;
    private String skillName;

    //  NUEVOS CAMPOS para cancelación de bookings (solo para LEARNERS)
    private Long bookingId;
    private String bookingType;  // INDIVIDUAL o GROUP
    //#endregion

    //#region Constructors
    public UpcomingSessionResponse() {}

    // Constructor para INSTRUCTORES (sin booking info)
    public UpcomingSessionResponse(Long id, String title, String description, Date scheduledDatetime,
                                   Integer durationMinutes, String status, String videoCallLink, String skillName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.scheduledDatetime = scheduledDatetime;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.videoCallLink = videoCallLink;
        this.skillName = skillName;
    }

    // Constructor para LEARNERS (con booking info)
    public UpcomingSessionResponse(Long id, String title, String description, Date scheduledDatetime,
                                   Integer durationMinutes, String status, String videoCallLink, String skillName,
                                   Long bookingId, String bookingType) {
        this(id, title, description, scheduledDatetime, durationMinutes, status, videoCallLink, skillName);
        this.bookingId = bookingId;
        this.bookingType = bookingType;
    }
    //#endregion

    //#region Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVideoCallLink() {
        return videoCallLink;
    }

    public void setVideoCallLink(String videoCallLink) {
        this.videoCallLink = videoCallLink;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingType() {
        return bookingType;
    }

    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }
    //#endregion
}