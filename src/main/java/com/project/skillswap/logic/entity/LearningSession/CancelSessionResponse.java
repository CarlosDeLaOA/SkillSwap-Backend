
package com.project.skillswap.logic.entity.LearningSession;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
/**
 * DTO para responder después de cancelar una sesión
 */
public class CancelSessionResponse {
    private static final Logger logger = LoggerFactory.getLogger(CancelSessionResponse.class);

    private Long sessionId;
    private String sessionTitle;
    private Integer participantsNotified;
    private String status;
    private String cancellationDate;

    public CancelSessionResponse() {}

    public CancelSessionResponse(Long sessionId, String sessionTitle,
                                 Integer participantsNotified, String status,
                                 String cancellationDate) {
        this.sessionId = sessionId;
        this.sessionTitle = sessionTitle;
        this.participantsNotified = participantsNotified;
        this.status = status;
        this.cancellationDate = cancellationDate;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }

    public Integer getParticipantsNotified() {
        return participantsNotified;
    }

    public void setParticipantsNotified(Integer participantsNotified) {
        this.participantsNotified = participantsNotified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCancellationDate() {
        return cancellationDate;
    }

    public void setCancellationDate(String cancellationDate) {
        this.cancellationDate = cancellationDate;
    }
}