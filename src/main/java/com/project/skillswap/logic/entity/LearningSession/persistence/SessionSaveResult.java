package com.project.skillswap.logic.entity.LearningSession.persistence;

import com.project.skillswap.logic.entity.LearningSession.SessionStatus;

/**
 * Clase para encapsular resultado del guardado de una sesión
 * Proporciona información clara sobre qué se guardó y dónde
 * CRITERIO 5: Información del guardado en BD interna
 */
public class SessionSaveResult {

    //#region Fields
    private Long sessionId;
    private SessionStatus sessionStatus;
    private boolean savedToDatabase;
    private boolean integrationWithGoogleCalendar;
    private String googleCalendarEventId;
    private String successMessage;
    private String storageLocation;
    //#endregion

    //#region Constructors
    /**
     * Constructor para sesión guardada en BD sin integración
     */
    public SessionSaveResult(Long sessionId, SessionStatus status, boolean savedToDb) {
        this.sessionId = sessionId;
        this.sessionStatus = status;
        this.savedToDatabase = savedToDb;
        this.integrationWithGoogleCalendar = false;
        this.storageLocation = "Base de datos interna";
        this.successMessage = "Sesión guardada exitosamente en BD interna";
    }

    /**
     * Constructor para sesión guardada con integración
     */
    public SessionSaveResult(Long sessionId, SessionStatus status, boolean savedToDb,
                             String googleCalendarId) {
        this.sessionId = sessionId;
        this.sessionStatus = status;
        this.savedToDatabase = savedToDb;
        this.integrationWithGoogleCalendar = true;
        this.googleCalendarEventId = googleCalendarId;
        this.storageLocation = "Base de datos interna + Google Calendar";
        this.successMessage = "Sesión guardada en BD interna y sincronizada con Google Calendar";
    }
    //#endregion

    //#region Getters
    public Long getSessionId() {
        return sessionId;
    }

    public SessionStatus getSessionStatus() {
        return sessionStatus;
    }

    public boolean isSavedToDatabase() {
        return savedToDatabase;
    }

    public boolean hasGoogleCalendarIntegration() {
        return integrationWithGoogleCalendar;
    }

    public String getGoogleCalendarEventId() {
        return googleCalendarEventId;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getStorageLocation() {
        return storageLocation;
    }
    //#endregion

    //#region Helper Methods
    /**
     * Retorna un resumen ejecutivo del resultado
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Sesión #").append(sessionId).append(" - ");
        summary.append(sessionStatus).append(" - ");

        if (integrationWithGoogleCalendar) {
            summary.append("Con Google Calendar (").append(googleCalendarEventId).append(")");
        } else {
            summary.append("BD Interna");
        }

        return summary.toString();
    }

    /**
     * Retorna información detallada del guardado
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== RESULTADO DEL GUARDADO ===\n");
        info.append("ID Sesión: ").append(sessionId).append("\n");
        info.append("Estado: ").append(sessionStatus).append("\n");
        info.append("Guardado en BD: ").append(savedToDatabase ? "✅ SÍ" : "❌ NO").append("\n");
        info.append("Ubicación: ").append(storageLocation).append("\n");

        if (integrationWithGoogleCalendar) {
            info.append("Google Calendar: ✅ INTEGRADO\n");
            info.append("Event ID: ").append(googleCalendarEventId).append("\n");
        } else {
            info.append("Google Calendar: ⚠️ NO INTEGRADO\n");
        }

        info.append("\nMensaje: ").append(successMessage);

        return info.toString();
    }
    //#endregion
}