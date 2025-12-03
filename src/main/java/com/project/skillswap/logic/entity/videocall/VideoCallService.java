
package com.project.skillswap.logic.entity.videocall;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class VideoCallService {
    private static final Logger logger = LoggerFactory.getLogger(VideoCallService.class);

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
     *  Genera datos de videollamada con nombre de sala CONSISTENTE
     * Todos los participantes de la misma sesión usan la MISMA sala
     *
     * @param sessionId ID de la sesión de aprendizaje
     * @param person Usuario que se une a la videollamada
     * @param isModerator Si el usuario es moderador (instructor)
     * @return Map con datos de la videollamada
     */
    public Map<String, Object> generateVideoCallToken(Long sessionId, Person person, boolean isModerator) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Validar estado de la sesión
        if (developmentMode) {
            logger.info("️ [DEV MODE] Permitiendo acceso a sesión");
            if (session.getStatus() == SessionStatus.CANCELLED) {
                throw new RuntimeException("La sesión ha sido cancelada");
            }


            if (session.getStatus() == SessionStatus.SCHEDULED ||
                    session.getStatus() == SessionStatus.DRAFT) {
                session.setStatus(SessionStatus.ACTIVE);
                sessionRepository.save(session);
                logger.info(" Sesión cambiada a ACTIVE");
            }
        } else {
            logger.info(" [PROD MODE] Validando estado: " + session.getStatus());
            if (session.getStatus() != SessionStatus.SCHEDULED &&
                    session.getStatus() != SessionStatus.ACTIVE) {
                throw new RuntimeException("La sesión no está disponible. Estado: " + session.getStatus());
            }
        }


        String roomName = "skillswap_session_" + sessionId;

        logger.info("========================================");
        logger.info(" GENERANDO DATOS DE VIDEOLLAMADA");
        logger.info("   Session ID: " + sessionId);
        logger.info("   Room Name: " + roomName);
        logger.info("   Usuario: " + person.getFullName());
        logger.info("   Es Moderador: " + isModerator);
        logger.info("========================================");


        String videoCallLink = frontendVideoCallUrl + "/" + sessionId;


        if (session.getVideoCallLink() == null || session.getVideoCallLink().isEmpty()) {
            session.setVideoCallLink(videoCallLink);
            sessionRepository.save(session);
            logger.info(" Link guardado: " + videoCallLink);
        }


        String jitsiJoinLink = "https://" + jitsiDomain + "/" + roomName;

        logger.info(" Datos de videollamada:");
        logger.info("   Domain: " + jitsiDomain);
        logger.info("   Room: " + roomName);
        logger.info("   User: " + person.getFullName());
        logger.info("   Moderator: " + isModerator);

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
            logger.info(" Sesión activada al unirse participante");
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