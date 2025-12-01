
package com.project.skillswap.logic.entity.Notification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO que agrupa sesiones por usuario para el envío de alertas
 */
public class UserSessionAlertDTO {
    private Long personId;
    private String fullName;
    private String email;
    private List<SessionAlertDTO> instructorSessions;
    private List<SessionAlertDTO> learnerSessions;

    public UserSessionAlertDTO() {
        this.instructorSessions = new ArrayList<>();
        this.learnerSessions = new ArrayList<>();
    }

    public UserSessionAlertDTO(Long personId, String fullName, String email) {
        this.personId = personId;
        this.fullName = fullName;
        this.email = email;
        this.instructorSessions = new ArrayList<>();
        this.learnerSessions = new ArrayList<>();
    }

    public boolean hasSessions() {
        return !instructorSessions.isEmpty() || !learnerSessions.isEmpty();
    }

    public int getTotalSessions() {
        return instructorSessions.size() + learnerSessions.size();
    }

    // Getters and Setters
    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<SessionAlertDTO> getInstructorSessions() { return instructorSessions; }
    public void setInstructorSessions(List<SessionAlertDTO> instructorSessions) {
        this.instructorSessions = instructorSessions;
    }

    public List<SessionAlertDTO> getLearnerSessions() { return learnerSessions; }
    public void setLearnerSessions(List<SessionAlertDTO> learnerSessions) {
        this.learnerSessions = learnerSessions;
    }
}