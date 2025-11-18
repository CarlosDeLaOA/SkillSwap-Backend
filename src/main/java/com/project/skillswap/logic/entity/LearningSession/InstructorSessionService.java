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
 * Servicio para gestión de sesiones del instructor
 */
@Service
public class InstructorSessionService {

    @Autowired
    private LearningSessionRepository sessionRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * Lista todas las sesiones de un instructor con filtros y paginación
     *
     * @param userEmail Email del instructor
     * @param status Estado para filtrar (opcional)
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Página de sesiones
     */
    @Transactional(readOnly = true)
    public Page<SessionListResponse> getInstructorSessions(String userEmail,
                                                           String status,
                                                           int page,
                                                           int size) {
        System.out.println("📋 [SESSION_LIST] Listando sesiones del instructor: " + userEmail);

        // 1. Obtener instructor
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Instructor instructor = person.getInstructor();
        if (instructor == null) {
            throw new RuntimeException("El usuario no tiene un perfil de instructor");
        }

        // 2. Convertir string status a enum (si se proporcionó)
        SessionStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = SessionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Estado inválido: " + status);
            }
        }

        // 3. Crear paginación
        Pageable pageable = PageRequest.of(page, size);

        // 4. Obtener sesiones
        Page<SessionListResponse> sessions = sessionRepository.findInstructorSessions(
                instructor.getId(),
                statusEnum,
                pageable
        );

        System.out.println("✅ [SESSION_LIST] Encontradas " + sessions.getTotalElements() + " sesiones");

        return sessions;
    }

    /**
     * Actualiza una sesión del instructor con validaciones
     *
     * @param sessionId ID de la sesión
     * @param request Datos a actualizar
     * @param userEmail Email del instructor
     * @return Sesión actualizada
     */
    @Transactional
    public SessionUpdateResponse updateSession(Long sessionId,
                                               SessionUpdateRequest request,
                                               String userEmail) {
        System.out.println("✏️ [SESSION_UPDATE] Actualizando sesión " + sessionId);

        // 1. Obtener instructor
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Instructor instructor = person.getInstructor();
        if (instructor == null) {
            throw new RuntimeException("El usuario no tiene un perfil de instructor");
        }

        // 2. Obtener sesión y verificar pertenencia
        LearningSession session = sessionRepository.findByIdAndInstructor(sessionId, instructor.getId())
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada o no tienes permiso para editarla"));

        // 3. Validar que la sesión pueda ser editada
        validateSessionEditable(session);

        // 4. Validar cambios específicos
        validateChanges(session, request);

        // 5. Aplicar cambios y registrar
        Map<String, Object> changes = applyChanges(session, request);

        // 6. Guardar
        LearningSession updatedSession = sessionRepository.save(session);

        System.out.println("✅ [SESSION_UPDATE] Sesión actualizada exitosamente");

        // 7. Construir respuesta
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

    /**
     * Valida que la sesión pueda ser editada
     */
    private void validateSessionEditable(LearningSession session) {
        // ❌ NO se puede editar si ya está COMPLETADA
        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new RuntimeException("No se puede editar una sesión que ya finalizó");
        }

        // ❌ NO se puede editar si está ACTIVA
        if (session.getStatus() == SessionStatus.ACTIVE) {
            throw new RuntimeException("No se puede editar una sesión que está en curso");
        }

        // ❌ NO se puede editar si está CANCELADA
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new RuntimeException("No se puede editar una sesión cancelada");
        }

        // ✅ Solo se puede editar si está SCHEDULED
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new RuntimeException("Solo se pueden editar sesiones programadas");
        }
    }

    /**
     * Valida cambios específicos
     */
    private void validateChanges(LearningSession session, SessionUpdateRequest request) {
        // Validar duración si hay bookings confirmados
        if (request.getDurationMinutes() != null &&
                !request.getDurationMinutes().equals(session.getDurationMinutes())) {

            long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(session.getId());

            if (confirmedBookings > 0) {
                throw new RuntimeException(
                        "No se puede modificar la duración porque ya hay " + confirmedBookings +
                                " participante(s) registrado(s)"
                );
            }
        }

        // Validar duración mínima
        if (request.getDurationMinutes() != null && request.getDurationMinutes() < 15) {
            throw new RuntimeException("La duración mínima es de 15 minutos");
        }

        // Validar duración máxima
        if (request.getDurationMinutes() != null && request.getDurationMinutes() > 480) {
            throw new RuntimeException("La duración máxima es de 480 minutos (8 horas)");
        }

        // Validar descripción no vacía
        if (request.getDescription() != null && request.getDescription().trim().isEmpty()) {
            throw new RuntimeException("La descripción no puede estar vacía");
        }

        // Validar longitud de descripción
        if (request.getDescription() != null && request.getDescription().length() < 10) {
            throw new RuntimeException("La descripción debe tener al menos 10 caracteres");
        }

        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new RuntimeException("La descripción no puede exceder 500 caracteres");
        }
    }

    /**
     * Aplica los cambios y registra qué se modificó
     */
    private Map<String, Object> applyChanges(LearningSession session, SessionUpdateRequest request) {
        Map<String, Object> changes = new HashMap<>();

        // Actualizar descripción
        if (request.getDescription() != null &&
                !request.getDescription().equals(session.getDescription())) {

            changes.put("description", Map.of(
                    "old", session.getDescription(),
                    "new", request.getDescription()
            ));
            session.setDescription(request.getDescription());
        }

        // Actualizar duración
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
}