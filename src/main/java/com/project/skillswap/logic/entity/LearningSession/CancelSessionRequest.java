
package com.project.skillswap.logic.entity.LearningSession;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * DTO para recibir la solicitud de cancelación de sesión
 */
public class CancelSessionRequest {
    private static final Logger logger = LoggerFactory.getLogger(CancelSessionRequest.class);

    private String reason;

    public CancelSessionRequest() {}

    public CancelSessionRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}