package com.project.skillswap.logic.entity.LearningSession;

import com.project.skillswap.logic.entity.Booking.BookingRepository;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio encargado de gestionar las sesiones creadas por instructores:
 * - Listado con filtros y paginación
 * - Actualización de sesiones con validaciones estrictas
 */
@Service
public class InstructorSessionService {

    //#region Dependencies

    @Autowired
    private LearningSessionRepository sessionRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private BookingRepository bookingRepository;

    //#endregion

    //#region List Sessions
    /**
     * Lista todas las sesiones pertenecientes a un instructor, con filtros
     * opcionales de estado y soporte de paginación.
     *
     * @param userEmail Email del instructor autenticado
     * @param status    Estado opcional de filtro (SCHEDULED, ACTIVE, FINISHED, CANCELLED)
     * @param page      Número de página (0-indexed)
     * @param size      Tamaño de página
     * @return Página de sesiones con vista resumida
     */
    @Transactional(readOnly = true)
    public Page<SessionListResponse> getInstructorSessions(
            String userEmail,
            String status,
            int page,
            int size
    ) {
        // Obtener instructor
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Instructor instructor = person.getInstructor();
        if (instructor == null) {
            throw new RuntimeException("El usuario no tiene un perfil de instructor");
        }

        // Mapear estado opcional a enum
        SessionStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = SessionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Estado inválido: " + status);
            }
        }

        Pageable pageable = PageRequest.of(page, size);

        return sessionRepository.findInstructorSessions(
                instructor.getId(),
                statusEnum,
                pageable
        );
    }

    //#endregion

    //#region Update Session
    /**
     * Actualiza una sesión creada por un instructor, únicamente si cumple
     * validaciones estrictas de estado y restricciones de negocio.
     *
     * @param sessionId ID de la sesión a editar
     * @param request   Datos de actualización
     * @param userEmail Email del instructor autenticado
     * @return Datos finales de la sesión actualizada
     */
    @Transactional
    public SessionUpdateResponse updateSession(
            Long sessionId,
            SessionUpdateRequest request,
            String userEmail
    ) {
        // Verificar instructor
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Instructor instructor = person.getInstructor();
        if (instructor == null) {
            throw new RuntimeException("El usuario no tiene un perfil de instructor");
        }

        // Obtener sesión y verificar pertenencia
        LearningSession session = sessionRepository.findByIdAndInstructor(sessionId, instructor.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Sesión no encontrada o no tienes permiso para editarla"));

        validateSessionEditable(session);
        validateChanges(session, request);

        Map<String, Object> changes = applyChanges(session, request);

        LearningSession updatedSession = sessionRepository.save(session);

        return new SessionUpdateResponse(
                updatedSession.getId(),
                updatedSession.getTitle(),
                updatedSession.getDescription(),
                updatedSession.getDurationMinutes(),
                updatedSession.getVideoCallLink(),
                updatedSession.getScheduledDatetime(),
                updatedSession.getStatus().toString(),
                changes
        );
    }

    //#endregion

    //#region Validation Rules
    /**
     * Valida que la sesión esté en un estado editable.
     * Únicamente sesiones SCHEDULED pueden ser modificadas.
     */
    private void validateSessionEditable(LearningSession session) {

        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new RuntimeException("No se puede editar una sesión que ya finalizó");
        }

        if (session.getStatus() == SessionStatus.ACTIVE) {
            throw new RuntimeException("No se puede editar una sesión que está en curso");
        }

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new RuntimeException("No se puede editar una sesión cancelada");
        }

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new RuntimeException("Solo se pueden editar sesiones programadas");
        }
    }

    /**
     * Valida cambios propuestos para asegurar que cumplan reglas de negocio.
     */
    private void validateChanges(LearningSession session, SessionUpdateRequest request) {

        // Cambio de duración con bookings confirmados
        if (request.getDurationMinutes() != null &&
                !request.getDurationMinutes().equals(session.getDurationMinutes())) {

            long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(session.getId());

            if (confirmedBookings > 0) {
                throw new RuntimeException(
                        "No se puede modificar la duración porque ya hay " +
                                confirmedBookings +
                                " participante(s) registrado(s)"
                );
            }
        }

        // Duración mínima
        if (request.getDurationMinutes() != null && request.getDurationMinutes() < 15) {
            throw new RuntimeException("La duración mínima es de 15 minutos");
        }

        // Duración máxima
        if (request.getDurationMinutes() != null && request.getDurationMinutes() > 480) {
            throw new RuntimeException("La duración máxima es de 480 minutos (8 horas)");
        }

        // Validación de descripción
        if (request.getDescription() != null) {

            if (request.getDescription().trim().isEmpty()) {
                throw new RuntimeException("La descripción no puede estar vacía");
            }

            if (request.getDescription().length() < 10) {
                throw new RuntimeException("La descripción debe tener al menos 10 caracteres");
            }

            if (request.getDescription().length() > 500) {
                throw new RuntimeException("La descripción no puede exceder 500 caracteres");
            }
        }
    }

    //#endregion

    //#region Apply Changes
    /**
     * Aplica los cambios permitidos a la entidad y devuelve un mapa
     * con el historial de modificaciones realizadas.
     */
    private Map<String, Object> applyChanges(
            LearningSession session,
            SessionUpdateRequest request
    ) {
        Map<String, Object> changes = new HashMap<>();

        // Descripción
        if (request.getDescription() != null &&
                !request.getDescription().equals(session.getDescription())) {

            changes.put("description", Map.of(
                    "old", session.getDescription(),
                    "new", request.getDescription()
            ));

            session.setDescription(request.getDescription());
        }

        // Duración
        if (request.getDurationMinutes() != null &&
                !request.getDurationMinutes().equals(session.getDurationMinutes())) {

            changes.put("durationMinutes", Map.of(
                    "old", session.getDurationMinutes(),
                    "new", request.getDurationMinutes()
            ));

            session.setDurationMinutes(request.getDurationMinutes());
        }

        return changes;
    }

    //#endregion
}
