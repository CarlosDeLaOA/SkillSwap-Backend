package com.project.skillswap.logic.entity.LearningSession;

/**
 * DTO para recibir la solicitud de cancelación de sesión
 */
public class CancelSessionRequest {

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