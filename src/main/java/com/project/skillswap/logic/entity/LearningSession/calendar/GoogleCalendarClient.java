package com.project.skillswap.logic.entity.LearningSession.calendar;

import java.util.Date;

/**
 * Interfaz para operaciones con Google Calendar
 * Define contrato para crear, actualizar y eliminar eventos
 */
public interface GoogleCalendarClient {

    /**
     * Crea un evento en Google Calendar
     *
     * @param title Título del evento
     * @param description Descripción del evento
     * @param startTime Fecha/hora de inicio
     * @param endTime Fecha/hora de fin
     * @param instructorEmail Email del instructor
     * @param videoCallLink Enlace de videollamada (opcional)
     * @return ID del evento creado en Google Calendar
     * @throws Exception Si falla la creación
     */
    String createCalendarEvent(
            String title,
            String description,
            Date startTime,
            Date endTime,
            String instructorEmail,
            String videoCallLink
    ) throws Exception;

    /**
     * Elimina un evento de Google Calendar
     *
     * @param calendarEventId ID del evento en Google Calendar
     * @param instructorEmail Email del instructor
     * @throws Exception Si falla la eliminación
     */
    void deleteCalendarEvent(String calendarEventId, String instructorEmail) throws Exception;

    /**
     * Verifica si el cliente está correctamente configurado
     *
     * @return true si está listo, false si no
     */
    boolean isConfigured();
}