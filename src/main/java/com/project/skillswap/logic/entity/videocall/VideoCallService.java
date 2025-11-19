package com.project.skillswap.logic.entity.videocall;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio para manejar operaciones de videollamadas con Jitsi Meet
 * MODO SIN JWT - Para pruebas con Jitsi público
 */
@Service
public class VideoCallService {

    //#region Dependencies
    @Autowired
    private LearningSessionRepository sessionRepository;

    @Value("${jitsi.domain:meet.jit.si}")
    private String jitsiDomain;

    @Value("${frontend.video-call-url:http://localhost:4200/app/video-call}")
    private String frontendVideoCallUrl;

    @Value("${app.development.mode:true}")
    private Boolean developmentMode;
    //#endregion

    //#region Public Methods
    /**
     *  Genera sala con nombre completamente aleatorio
     *
     * @param sessionId ID de la sesión de aprendizaje
     * @param person Usuario que se une a la videollamada
     * @param isModerator Si el usuario es moderador (instructor)
     * @return Map con datos de la videollamada
     */
    public Map<String, Object> generateVideoCallToken(Long sessionId, Person person, boolean isModerator) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));


        if (developmentMode) {
            System.out.println("🛠️ [DEV MODE] Permitiendo acceso a sesión");
            if (session.getStatus() == SessionStatus.CANCELLED) {
                throw new RuntimeException("La sesión ha sido cancelada");
            }

            // Cambiar a ACTIVE si está SCHEDULED o DRAFT
            if (session.getStatus() == SessionStatus.SCHEDULED ||
                    session.getStatus() == SessionStatus.DRAFT) {
                session.setStatus(SessionStatus.ACTIVE);
                sessionRepository.save(session);
                System.out.println("🟢 Sesión cambiada a ACTIVE");
            }
        } else {
            System.out.println("🔒 [PROD MODE] Validando estado: " + session.getStatus());
            if (session.getStatus() != SessionStatus.SCHEDULED &&
                    session.getStatus() != SessionStatus.ACTIVE) {
                throw new RuntimeException("La sesión no está disponible. Estado: " + session.getStatus());
            }
        }

        //  Usar nombre de sala COMPLETAMENTE ALEATORIO

        String randomToken = UUID.randomUUID().toString().replaceAll("-", "");
        String roomName = "ss" + sessionId + randomToken.substring(0, 12);

        // Link del frontend
        String videoCallLink = frontendVideoCallUrl + "/" + sessionId;

        // Actualizar link en BD si no existe
        if (session.getVideoCallLink() == null || session.getVideoCallLink().isEmpty()) {
            session.setVideoCallLink(videoCallLink);
            sessionRepository.save(session);
            System.out.println(" Link guardado: " + videoCallLink);
        }

        // Link directo de Jitsi
        String jitsiJoinLink = "https://" + jitsiDomain + "/" + roomName;

        System.out.println(" Datos de videollamada:");
        System.out.println("   Domain: " + jitsiDomain);
        System.out.println("   Room: " + roomName);
        System.out.println("   User: " + person.getFullName());
        System.out.println("   Moderator: " + isModerator);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("roomName", roomName);
        response.put("videoCallLink", videoCallLink);
        response.put("jitsiJoinLink", jitsiJoinLink);
        response.put("domain", jitsiDomain);
        response.put("displayName", person.getFullName());
        response.put("email", person.getEmail());
        response.put("isModerator", isModerator);
        response.put("status", session.getStatus().toString());
        response.put("useJWT", false);
        response.put("cameraEnabled", true);
        response.put("microphoneEnabled", true);

        return response;
    }

    /**
     * Valida un enlace de videollamada contra una sesión
     */
    public boolean validateVideoCallLink(Long sessionId, String joinLink) {
        Optional<LearningSession> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            return false;
        }

        LearningSession session = sessionOpt.get();

        if (session.getVideoCallLink() == null) {
            return false;
        }

        return session.getVideoCallLink().equals(joinLink) &&
                (session.getStatus() == SessionStatus.SCHEDULED ||
                        session.getStatus() == SessionStatus.ACTIVE ||
                        (developmentMode && session.getStatus() != SessionStatus.CANCELLED));
    }

    /**
     * Registra la unión de un participante a la videollamada
     */
    public Map<String, Object> registerParticipantJoin(Long sessionId, Long personId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Cambiar a ACTIVE si está en SCHEDULED o DRAFT
        if (session.getStatus() == SessionStatus.SCHEDULED ||
                (developmentMode && session.getStatus() == SessionStatus.DRAFT)) {
            session.setStatus(SessionStatus.ACTIVE);
            sessionRepository.save(session);
            System.out.println("🟢 Sesión activada al unirse participante");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("personId", personId);
        response.put("joinedAt", new Date());
        response.put("sessionStatus", session.getStatus().toString());

        return response;
    }

    /**
     * Obtiene información de una videollamada activa
     */
    public Map<String, Object> getVideoCallInfo(Long sessionId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("title", session.getTitle());
        response.put("videoCallLink", session.getVideoCallLink());
        response.put("status", session.getStatus().toString());
        response.put("scheduledDatetime", session.getScheduledDatetime());
        response.put("durationMinutes", session.getDurationMinutes());
        response.put("instructorName", session.getInstructor().getPerson().getFullName());
        response.put("maxCapacity", session.getMaxCapacity());
        response.put("currentBookings", session.getCurrentBookings());

        return response;
    }
    //#endregion
}