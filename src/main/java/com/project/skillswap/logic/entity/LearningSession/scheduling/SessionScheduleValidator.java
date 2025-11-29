package com.project.skillswap.logic.entity.LearningSession.scheduling;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Componente responsable de validar disponibilidad de horarios
 * y detectar conflictos de programación para instructores
 */
@Component
public class SessionScheduleValidator {

    //#region Dependencies
    @Autowired
    private LearningSessionRepository learningSessionRepository;
    //#endregion

    //#region Constants
    private static final long MINIMUM_ADVANCE_MINUTES = 60; // 1 hora
    private static final int SUGGESTION_SLOTS_COUNT = 5;
    private static final int SEARCH_DAYS_AHEAD = 7;
    private static final int BUSINESS_HOURS_START = 9;
    private static final int BUSINESS_HOURS_END = 18;
    //#endregion

    //#region Public Methods
    /**
     * Valida que no haya conflictos de horario para el instructor
     * Si hay conflictos, genera sugerencias y lanza excepción
     *
     * @param instructorId ID del instructor
     * @param scheduledDatetime Fecha y hora de la nueva sesión
     * @param durationMinutes Duración de la nueva sesión
     * @param excludeSessionId ID de sesión a excluir (null si es nueva)
     * @throws IllegalArgumentException Si hay conflicto de horario
     */
    public void validateNoScheduleConflicts(Long instructorId, Date scheduledDatetime,
                                            Integer durationMinutes, Long excludeSessionId) {
        // Calcular hora de fin de la nueva sesión
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(scheduledDatetime);
        calendar.add(Calendar.MINUTE, durationMinutes);
        Date endTime = calendar.getTime();

        // Buscar conflictos
        List<LearningSession> conflictingSessions = learningSessionRepository
                .findConflictingSessions(instructorId, scheduledDatetime, endTime);

        // Filtrar si es actualización (excluir la sesión actual)
        if (excludeSessionId != null) {
            conflictingSessions = conflictingSessions.stream()
                    .filter(s -> !s.getId().equals(excludeSessionId))
                    .toList();
        }

        // Si hay conflictos, lanzar excepción con sugerencias
        if (!conflictingSessions.isEmpty()) {
            String conflictDetails = buildConflictMessage(conflictingSessions);
            List<String> alternatives = suggestAlternativeTimeSlots(instructorId, scheduledDatetime, durationMinutes);

            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append(conflictDetails);

            if (!alternatives.isEmpty()) {
                errorMessage.append(" Horarios sugeridos: ");
                errorMessage.append(String.join(" | ", alternatives));
            }

            throw new IllegalArgumentException(errorMessage.toString());
        }
    }

    /**
     * Valida la anticipación mínima de 1 hora
     * Proporciona información sobre cuánto tiempo falta
     *
     * @param scheduledDatetime Fecha y hora de la sesión
     * @throws IllegalArgumentException Si la sesión es en menos de 1 hora
     */
    public void validateMinimumAdvanceTime(Date scheduledDatetime) {
        Date now = new Date();
        long diffInMillis = scheduledDatetime.getTime() - now.getTime();
        long diffInMinutes = diffInMillis / (60 * 1000);

        if (diffInMinutes < MINIMUM_ADVANCE_MINUTES) {
            long minutesNeeded = MINIMUM_ADVANCE_MINUTES - diffInMinutes;

            String errorMessage;
            if (diffInMinutes < 0) {
                // Pasada
                errorMessage = String.format(
                        "La sesión no puede estar en el pasado. Intenta programarla para una hora futura."
                );
            } else {
                // Muy próxima
                errorMessage = String.format(
                        "La sesión debe programarse con al menos 1 hora de anticipación. " +
                                "Necesitas esperar %d minuto(s) más.",
                        minutesNeeded
                );
            }

            throw new IllegalArgumentException(errorMessage);
        }
    }
    //#endregion

    //#region Private Methods - Conflict Detection
    /**
     * Construye mensaje descriptivo del conflicto
     */
    private String buildConflictMessage(List<LearningSession> conflictingSessions) {
        if (conflictingSessions.isEmpty()) return "";

        StringBuilder message = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        message.append("Conflicto de horario detectado. Ya tienes sesión(es) programada(s): ");
        for (int i = 0; i < conflictingSessions.size(); i++) {
            LearningSession session = conflictingSessions.get(i);
            message.append("[").append(session.getTitle()).append(" - ")
                    .append(sdf.format(session.getScheduledDatetime())).append("]");
            if (i < conflictingSessions.size() - 1) {
                message.append(", ");
            }
        }
        message.append(".");

        return message.toString();
    }
    //#endregion

    //#region Private Methods - Suggestions
    /**
     * Sugiere 5 horarios alternativos disponibles
     * Busca slots disponibles en los próximos 7 días entre 9am y 6pm
     * Devuelve strings formateados para mostrar al usuario
     */
    private List<String> suggestAlternativeTimeSlots(Long instructorId, Date requestedTime,
                                                     Integer durationMinutes) {
        List<String> suggestions = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // Rango de búsqueda: hoy hasta 7 días adelante
        Date startDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DAY_OF_MONTH, SEARCH_DAYS_AHEAD);
        Date endDate = calendar.getTime();

        // Obtener todas las sesiones ocupadas en ese rango
        List<LearningSession> occupiedSessions = learningSessionRepository
                .findInstructorScheduledSessions(instructorId, startDate, endDate);

        // Generar candidatos de slots: cada hora desde mañana a las 9am hasta 6pm
        calendar.setTime(startDate);
        calendar.set(Calendar.HOUR_OF_DAY, BUSINESS_HOURS_START);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1); // Comenzar desde mañana

        while (suggestions.size() < SUGGESTION_SLOTS_COUNT) {
            Date slotStart = calendar.getTime();

            // Parar si salimos del rango o pasamos las 6pm
            if (slotStart.after(endDate) || calendar.get(Calendar.HOUR_OF_DAY) >= BUSINESS_HOURS_END) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, BUSINESS_HOURS_START);
                calendar.set(Calendar.MINUTE, 0);

                // Seguridad: no buscar más allá de 7 días
                if (slotStart.after(endDate)) {
                    break;
                }
                continue;
            }

            // Calcular fin del slot
            Date slotEnd = new Date(slotStart.getTime() + (durationMinutes * 60 * 1000L));

            // Verificar disponibilidad
            if (isTimeSlotAvailable(slotStart, slotEnd, occupiedSessions)) {
                suggestions.add(sdf.format(slotStart));
            }

            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }

        return suggestions;
    }

    /**
     * Valida si un slot de tiempo está disponible (sin conflictos)
     */
    private boolean isTimeSlotAvailable(Date slotStart, Date slotEnd, List<LearningSession> occupiedSessions) {
        for (LearningSession session : occupiedSessions) {
            Date sessionStart = session.getScheduledDatetime();
            Date sessionEnd = new Date(sessionStart.getTime() + (session.getDurationMinutes() * 60 * 1000L));

            // Verificar si hay solapamiento
            if (slotStart.before(sessionEnd) && slotEnd.after(sessionStart)) {
                return false;
            }
        }
        return true;
    }
    //#endregion
}