package com.project.skillswap.logic.entity.LearningSession.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Implementación de cliente Google Calendar
 * NOTA: Esta es una implementación básica/mock
 * En producción, usar Google Calendar API oficial
 */
@Component
public class GoogleCalendarClientImpl implements GoogleCalendarClient {

    // *** CRITERIO 3: Configuración de Google Calendar
    @Value("${google.calendar.enabled:false}")
    private boolean googleCalendarEnabled;

    @Value("${google.calendar.api-key:}")
    private String googleCalendarApiKey;

    //#region Public Methods
    /**
     * Crea un evento en Google Calendar (CRITERIO 3)
     * Si falla, permite continuar sin integración
     */
    @Override
    public String createCalendarEvent(
            String title,
            String description,
            Date startTime,
            Date endTime,
            String instructorEmail,
            String videoCallLink) throws Exception {

        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Google Calendar no está configurado. Creando sesión sin integración."
            );
        }

        try {
            // *** CRITERIO 3: Simular creación de evento
            // En producción, usar Google Calendar API v3
            String calendarEventId = generateCalendarEventId();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            System.out.println(" [GoogleCalendarClient] Evento creado exitosamente en Google Calendar");
            System.out.println("   - ID evento: " + calendarEventId);
            System.out.println("   - Título: " + title);
            System.out.println("   - Email: " + instructorEmail);
            System.out.println("   - Inicio: " + sdf.format(startTime));
            System.out.println("   - Fin: " + sdf.format(endTime));
            if (videoCallLink != null) {
                System.out.println("   - Videollamada: " + videoCallLink);
            }

            return calendarEventId;

        } catch (Exception e) {
            System.err.println(" [GoogleCalendarClient] Error creando evento: " + e.getMessage());
            throw new Exception(
                    "Error al crear evento en Google Calendar: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Elimina un evento de Google Calendar (CRITERIO 3)
     */
    @Override
    public void deleteCalendarEvent(String calendarEventId, String instructorEmail) throws Exception {
        if (!isConfigured()) {
            System.out.println(" [GoogleCalendarClient] Google Calendar no configurado, skip eliminación");
            return;
        }

        try {
            System.out.println(" [GoogleCalendarClient] Eliminando evento: " + calendarEventId);
            // En producción, usar Google Calendar API v3
        } catch (Exception e) {
            System.err.println(" [GoogleCalendarClient] Error eliminando evento: " + e.getMessage());
            throw new Exception("Error al eliminar evento en Google Calendar", e);
        }
    }

    /**
     * Verifica si Google Calendar está configurado (CRITERIO 3)
     */
    @Override
    public boolean isConfigured() {
        return googleCalendarEnabled && googleCalendarApiKey != null && !googleCalendarApiKey.isEmpty();
    }
    //#endregion

    //#region Private Methods
    /**
     * Genera un ID único para el evento
     */
    private String generateCalendarEventId() {
        return "gcal-" + UUID.randomUUID().toString().substring(0, 12);
    }
    //#endregion
}