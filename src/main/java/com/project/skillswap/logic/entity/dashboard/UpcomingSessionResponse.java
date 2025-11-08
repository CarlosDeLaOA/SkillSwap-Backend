package com.project.skillswap.logic.entity.dashboard;

import java.util.Date;

/**
 * Response containing information about an upcoming session
 */
public class UpcomingSessionResponse {

    //#region Fields
    private Long id;
    private String title;
    private String description;
    private Date scheduledDatetime;
    private Integer durationMinutes;
    private String status;
    private String videoCallLink;
    private String skillName;
    //#endregion

    //#region Constructors
    public UpcomingSessionResponse() {}
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
    //#endregion
}