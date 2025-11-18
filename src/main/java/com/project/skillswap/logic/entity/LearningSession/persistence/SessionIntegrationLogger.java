package com.project.skillswap.logic.entity.LearningSession.persistence;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Componente responsable de logging detallado sobre integraciones
 * Proporciona información clara sobre qué sistemas están integrados
 * CRITERIO 5: Logging de guardado en BD interna
 */
@Component
public class SessionIntegrationLogger {

    //#region Public Methods
    /**
     * Loguea el guardado exitoso de una sesión en BD interna
     * Incluye detalles de la sesión y si hay integraciones externas
     *
     * @param session Sesión guardada
     */
    public void logSessionSavedToBD(LearningSession session) {
        System.out.println(" [SessionIntegrationLogger] ✅ SESIÓN GUARDADA EN BD INTERNA");
        System.out.println("   ├─ ID Sesión: " + session.getId());
        System.out.println("   ├─ Título: " + session.getTitle());
        System.out.println("   ├─ Estado: " + session.getStatus());
        System.out.println("   ├─ Instructor: " + session.getInstructor().getPerson().getFullName());
        System.out.println("   ├─ Habilidad: " + session.getSkill().getName());

        if (session.getScheduledDatetime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("es", "ES"));
            System.out.println("   ├─ Programada: " + sdf.format(session.getScheduledDatetime()));
        }

        System.out.println("   └─ Integraciones: " + getIntegrationsSummary(session));
    }

    /**
     * Loguea cuando una sesión se crea en estado DRAFT
     *
     * @param session Sesión creada
     */
    public void logSessionCreatedAsDraft(LearningSession session) {
        System.out.println(" [SessionIntegrationLogger] ✅ SESIÓN CREADA EN BD INTERNA (DRAFT)");
        System.out.println("   ├─ ID Sesión: " + session.getId());
        System.out.println("   ├─ Título: " + session.getTitle());
        System.out.println("   ├─ Estado: DRAFT (pendiente de programación)");
        System.out.println("   └─ Almacenamiento: Base de datos interna");
    }

    /**
     * Loguea cuando se intenta guardar sin integración
     * (fallback de Google Calendar)
     *
     * @param session Sesión guardada sin integración
     */
    public void logSessionSavedWithoutIntegration(LearningSession session) {
        System.out.println("⚠️ [SessionIntegrationLogger] SESIÓN GUARDADA SIN INTEGRACIÓN EXTERNA");
        System.out.println("   ├─ ID Sesión: " + session.getId());
        System.out.println("   ├─ Razón: Google Calendar no está disponible o no está habilitado");
        System.out.println("   ├─ Almacenamiento: Base de datos interna ✅");
        System.out.println("   └─ Nota: La sesión está completamente funcional en SkillSwap");
    }

    /**
     * Loguea cuando se guarda con integración completa
     *
     * @param session Sesión guardada
     * @param googleCalendarId ID del evento en Google Calendar
     */
    public void logSessionSavedWithIntegration(LearningSession session, String googleCalendarId) {
        System.out.println(" [SessionIntegrationLogger] ✅ SESIÓN GUARDADA CON INTEGRACIONES");
        System.out.println("   ├─ ID Sesión: " + session.getId());
        System.out.println("   ├─ Almacenamiento: Base de datos interna ✅");
        System.out.println("   ├─ Google Calendar: INTEGRADO ✅");
        System.out.println("   │  └─ ID Evento: " + googleCalendarId);
        System.out.println("   └─ Estado: Sincronizado en múltiples sistemas");
    }

    /**
     * Retorna resumen de integraciones disponibles
     *
     * @param session Sesión a verificar
     * @return String con resumen de integraciones
     */
    public String getIntegrationsSummary(LearningSession session) {
        StringBuilder summary = new StringBuilder();

        // *** CRITERIO 5: Siempre está en BD interna
        summary.append("BD Interna (✅)");

        // Google Calendar
        if (session.getGoogleCalendarId() != null && !session.getGoogleCalendarId().isEmpty()) {
            summary.append(", Google Calendar (✅)");
        }

        // Video Call
        if (session.getVideoCallLink() != null && !session.getVideoCallLink().isEmpty()) {
            summary.append(", Videollamada (✅)");
        }

        return summary.toString();
    }

    /**
     * Retorna información completa de dónde está almacenada la sesión
     *
     * @param session Sesión a verificar
     * @return String con detalles de almacenamiento
     */
    public String getStorageDetails(LearningSession session) {
        StringBuilder details = new StringBuilder();

        /// *** CRITERIO 5: BD interna siempre
        details.append("✅ Base de datos interna SkillSwap");

        if (session.getGoogleCalendarId() != null) {
            details.append(" + ✅ Google Calendar");
        }

        if (session.getVideoCallLink() != null) {
            details.append(" + ✅ Enlace de videollamada");
        }

        return details.toString();
    }
    //#endregion
}