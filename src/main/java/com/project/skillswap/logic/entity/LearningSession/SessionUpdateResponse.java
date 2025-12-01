
package com.project.skillswap.logic.entity.LearningSession;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.Date;
import java.util.Map;

public class SessionUpdateResponse {
    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private String videoCallLink;
    private Date scheduledDatetime;
    private String status;
    private Map<String, Object> changes;

    public SessionUpdateResponse() {}

    public SessionUpdateResponse(Long id, String title, String description,
                                 Integer durationMinutes, String videoCallLink,
                                 Date scheduledDatetime, String status,
                                 Map<String, Object> changes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.videoCallLink = videoCallLink;
        this.scheduledDatetime = scheduledDatetime;
        this.status = status;
        this.changes = changes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getVideoCallLink() { return videoCallLink; }
    public void setVideoCallLink(String videoCallLink) { this.videoCallLink = videoCallLink; }

    public Date getScheduledDatetime() { return scheduledDatetime; }
    public void setScheduledDatetime(Date scheduledDatetime) { this.scheduledDatetime = scheduledDatetime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getChanges() { return changes; }
    public void setChanges(Map<String, Object> changes) { this.changes = changes; }
}
