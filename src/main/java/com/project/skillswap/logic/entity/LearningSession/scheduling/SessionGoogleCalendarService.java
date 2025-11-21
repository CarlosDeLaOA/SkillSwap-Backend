package com.project.skillswap.logic.entity.LearningSession.scheduling;

import com.project.skillswap.logic.entity.LearningSession.calendar.GoogleCalendarClient;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Calendar;
import java.util.Date;

/**
 * Servicio para integración con Google Calendar
 * Orquesta la creación de eventos en Google Calendar
 */
@Service
public class SessionGoogleCalendarService {

    //#region Dependencies
    @Autowired
    private GoogleCalendarClient googleCalendarClient;
    //#endregion

    //#region Public Methods -
    /**
     * Intenta crear un evento en Google Calendar
     * Si falla, retorna null (permite crear sesión sin integración)
     *
     * @param session Sesión a sincronizar
     * @param instructor Instructor propietario
     * @param enableIntegration Si es true, intenta crear en Google Calendar
     * @return ID del evento en Google Calendar o null si falla/deshabilitado
     */
    public String tryCreateCalendarEvent(LearningSession session, Person instructor, boolean enableIntegration) {

        // Si no está habilitada la integración, skip
        if (!enableIntegration || !googleCalendarClient.isConfigured()) {
            System.out.println(" [SessionGoogleCalendarService] Google Calendar deshabilitado o no configurado");
            return null;
        }

        try {
            // Calcular hora de fin
            Date startTime = session.getScheduledDatetime();
            Date endTime = calculateEndTime(startTime, session.getDurationMinutes());

            // Crear evento en Google Calendar
            String googleCalendarEventId = googleCalendarClient.createCalendarEvent(
                    session.getTitle(),
                    session.getDescription(),
                    startTime,
                    endTime,
                    instructor.getEmail(),
                    session.getVideoCallLink()
            );

            System.out.println(" [SessionGoogleCalendarService] Evento creado exitosamente: " + googleCalendarEventId);
            return googleCalendarEventId;

        } catch (Exception e) {
            //  Si falla Google Calendar, permitir creación sin integración
            System.err.println(" [SessionGoogleCalendarService] Error creando evento en Google Calendar: " + e.getMessage());
            System.out.println(" [SessionGoogleCalendarService] ⚠️ Continuando con creación de sesión sin integración...");
            return null;
        }
    }

    /**
     * Intenta eliminar un evento de Google Calendar
     * No lanza excepciones si falla (operación silenciosa)
     *
     * @param googleCalendarId ID del evento en Google Calendar
     * @param instructorEmail Email del instructor
     */
    public void tryDeleteCalendarEvent(String googleCalendarId, String instructorEmail) {
        if (googleCalendarId == null || googleCalendarId.isEmpty()) {
            return;
        }

        try {
            googleCalendarClient.deleteCalendarEvent(googleCalendarId, instructorEmail);
            System.out.println(" [SessionGoogleCalendarService] Evento eliminado de Google Calendar: " + googleCalendarId);
        } catch (Exception e) {
            System.err.println(" [SessionGoogleCalendarService] Error eliminando evento: " + e.getMessage());
            // No relanzar la excepción, solo loguear
        }
    }
    //#endregion

    //#region Private Methods
    /**
     * Calcula la hora de fin de la sesión
     */
    private Date calculateEndTime(Date startTime, Integer durationMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.add(Calendar.MINUTE, durationMinutes);
        return calendar.getTime();
    }
    //#endregion
}