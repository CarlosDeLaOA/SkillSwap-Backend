package com.project.skillswap.logic.entity.dashboard;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para gestionar el historial de sesiones de SkillSeekers en el dashboard.
 * Permite a los estudiantes ver las sesiones en las que han participado.
 *
 */
@Service
public class SessionHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(SessionHistoryService.class);

    @Autowired
    private LearningSessionRepository sessionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * Obtiene el historial de sesiones para un SkillSeeker (estudiante).
     * Retorna todas las sesiones completadas o canceladas en las que participó.
     *
     * @param learnerId ID del estudiante
     * @param page número de página (inicia en 0)
     * @param size tamaño de página
     * @return mapa con sesiones paginadas y metadata
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLearnerHistoricalSessions(
            Long learnerId,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LearningSession> sessionsPage = sessionRepository
                .findHistoricalSessionsByLearnerId(learnerId, pageable);

        // Construir respuesta con metadata de paginación
        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessionsPage.getContent());
        response.put("currentPage", sessionsPage.getNumber());
        response.put("totalItems", sessionsPage.getTotalElements());
        response.put("totalPages", sessionsPage.getTotalPages());
        response.put("hasNext", sessionsPage.hasNext());
        response.put("hasPrevious", sessionsPage.hasPrevious());

        return response;
    }

    /**
     * Obtiene los detalles completos de una sesión histórica específica.
     * Incluye el número de participantes que asistieron.
     *
     * @param sessionId ID de la sesión
     * @param learnerId ID del estudiante (para validar que participó)
     * @return mapa con sesión y detalles adicionales
     * @throws RuntimeException si la sesión no existe o el estudiante no participó
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionDetails(Long sessionId, Long learnerId) {

        // Verificar que la sesión existe
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        // Verificar que el estudiante participó en la sesión
        boolean participated = bookingRepository.existsByLearningSessionIdAndLearnerId(sessionId, learnerId);
        if (!participated) {
            throw new RuntimeException("El estudiante no participó en esta sesión");
        }

        // Contar participantes confirmados
        Integer participantCount = bookingRepository.countParticipantsBySessionId(sessionId);

        // Construir respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("session", session);
        response.put("participantCount", participantCount);

        return response;
    }
}