
package com.project.skillswap.logic.entity.Notification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.Date;

/**
 * DTO para alertas de sesiones próximas de la semana
 */
public class SessionAlertDTO {
    private Long sessionId;
    private String sessionTitle;
    private String skillName;
    private Date scheduledDatetime;
    private Integer durationMinutes;
    private String role; // "INSTRUCTOR" o "LEARNER"
    private String instructorName;
    private String videoCallLink;

    public SessionAlertDTO() {}

    public SessionAlertDTO(Long sessionId, String sessionTitle, String skillName,
                           Date scheduledDatetime, Integer durationMinutes,
                           String role, String instructorName, String videoCallLink) {
        this.sessionId = sessionId;
        this.sessionTitle = sessionTitle;
        this.skillName = skillName;
        this.scheduledDatetime = scheduledDatetime;
        this.durationMinutes = durationMinutes;
        this.role = role;
        this.instructorName = instructorName;
        this.videoCallLink = videoCallLink;
    }

    // Getters and Setters
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public Date getScheduledDatetime() { return scheduledDatetime; }
    public void setScheduledDatetime(Date scheduledDatetime) { this.scheduledDatetime = scheduledDatetime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getVideoCallLink() { return videoCallLink; }
    public void setVideoCallLink(String videoCallLink) { this.videoCallLink = videoCallLink; }
}