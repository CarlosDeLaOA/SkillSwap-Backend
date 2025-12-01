
package com.project.skillswap.logic.entity.LearningSession;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.Date;

public class SessionListResponse {
    private static final Logger logger = LoggerFactory.getLogger(SessionListResponse.class);
    private Long id;
    private String title;
    private String description;
    private Date scheduledDatetime;
    private Integer durationMinutes;
    private String status;
    private String videoCallLink;
    private String skillName;
    private Integer maxCapacity;
    private Integer currentBookings;
    private Integer availableSpots;
    private Boolean isPremium;
    private Date creationDate;

    public SessionListResponse() {}

    public SessionListResponse(Long id, String title, String description,
                               Date scheduledDatetime, Integer durationMinutes,
                               String status, String videoCallLink, String skillName,
                               Integer maxCapacity, Long currentBookings, Boolean isPremium,
                               Date creationDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.scheduledDatetime = scheduledDatetime;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.videoCallLink = videoCallLink;
        this.skillName = skillName;
        this.maxCapacity = maxCapacity;
        this.currentBookings = currentBookings != null ? currentBookings.intValue() : 0;
        this.availableSpots = maxCapacity - this.currentBookings;
        this.isPremium = isPremium;
        this.creationDate = creationDate;
    }

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

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getCurrentBookings() {
        return currentBookings;
    }

    public void setCurrentBookings(Integer currentBookings) {
        this.currentBookings = currentBookings;
    }

    public Integer getAvailableSpots() {
        return availableSpots;
    }

    public void setAvailableSpots(Integer availableSpots) {
        this.availableSpots = availableSpots;
    }

    public Boolean getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(Boolean isPremium) {
        this.isPremium = isPremium;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}