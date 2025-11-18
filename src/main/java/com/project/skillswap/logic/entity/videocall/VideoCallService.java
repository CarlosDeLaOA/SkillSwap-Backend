package com.project.skillswap.logic.entity.videocall;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import com.project.skillswap.logic.entity.Person.Person;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio para manejar operaciones de videollamadas con Jitsi Meet
 */
@Service
public class VideoCallService {

    //#region Dependencies
    @Autowired
    private LearningSessionRepository sessionRepository;

    @Value("${jitsi.app-id}")
    private String jitsiAppId;

    @Value("${jitsi.app-secret}")
    private String jitsiAppSecret;

    @Value("${jitsi.domain}")
    private String jitsiDomain;

    @Value("${jitsi.token-expiration}")
    private Long tokenExpiration;
    //#endregion

    //#region Public Methods
    /**
     * Genera un token JWT para unirse a una videollamada
     * @param sessionId ID de la sesión de aprendizaje
     * @param person Usuario que se une a la videollamada
     * @param isModerator Si el usuario es moderador (instructor)
     * @return Map con datos de la videollamada
     */
    public Map<String, Object> generateVideoCallToken(Long sessionId, Person person, boolean isModerator) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Validar que la sesión esté activa o programada
        if (session.getStatus() != SessionStatus.SCHEDULED &&
                session.getStatus() != SessionStatus.ACTIVE) {
            throw new RuntimeException("La sesión no está disponible para videollamada");
        }

        // Generar nombre de sala único
        String roomName = "skillswap_" + sessionId + "_" + session.getTitle().replaceAll("[^a-zA-Z0-9]", "_");

        // Generar JWT token para Jitsi
        String jitsiToken = generateJitsiJWT(roomName, person, isModerator);

        // Generar enlace de videollamada
        String videoCallLink = "https://" + jitsiDomain + "/" + roomName;

        // Actualizar enlace en la sesión si no existe
        if (session.getVideoCallLink() == null || session.getVideoCallLink().isEmpty()) {
            session.setVideoCallLink(videoCallLink);
            sessionRepository.save(session);
        }

        // Preparar respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("roomName", roomName);
        response.put("videoCallLink", videoCallLink);
        response.put("jitsiToken", jitsiToken);
        response.put("domain", jitsiDomain);
        response.put("displayName", person.getFullName());
        response.put("email", person.getEmail());
        response.put("isModerator", isModerator);
        response.put("status", session.getStatus().toString());

        return response;
    }

    /**
     * Valida un enlace de videollamada contra una sesión
     * @param sessionId ID de la sesión
     * @param joinLink Enlace de unión
     * @return true si el enlace es válido
     */
    public boolean validateVideoCallLink(Long sessionId, String joinLink) {
        Optional<LearningSession> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            return false;
        }

        LearningSession session = sessionOpt.get();

        // Validar que el enlace coincida con la sesión
        if (session.getVideoCallLink() == null) {
            return false;
        }

        // Validar que la sesión esté en estado válido
        return session.getVideoCallLink().equals(joinLink) &&
                (session.getStatus() == SessionStatus.SCHEDULED ||
                        session.getStatus() == SessionStatus.ACTIVE);
    }

    /**
     * Registra la unión de un participante a la videollamada
     * @param sessionId ID de la sesión
     * @param personId ID del usuario
     * @return Map con confirmación
     */
    public Map<String, Object> registerParticipantJoin(Long sessionId, Long personId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Actualizar estado de la sesión a ACTIVE si estaba SCHEDULED
        if (session.getStatus() == SessionStatus.SCHEDULED) {
            session.setStatus(SessionStatus.ACTIVE);
            sessionRepository.save(session);
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
     * @param sessionId ID de la sesión
     * @return Map con información de la videollamada
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

    //#region Private Methods
    /**
     * Genera un JWT token para Jitsi Meet
     * @param roomName Nombre de la sala
     * @param person Usuario
     * @param isModerator Si es moderador
     * @return JWT token
     */
    private String generateJitsiJWT(String roomName, Person person, boolean isModerator) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + (tokenExpiration * 1000));

        Map<String, Object> context = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        user.put("name", person.getFullName());
        user.put("email", person.getEmail());
        user.put("id", person.getId().toString());
        user.put("moderator", isModerator);
        context.put("user", user);

        Map<String, Object> features = new HashMap<>();
        features.put("livestreaming", false);
        features.put("recording", true); // Solo audio según requisitos
        features.put("transcription", false);
        context.put("features", features);

        return Jwts.builder()
                .setSubject(jitsiDomain)
                .setAudience(jitsiAppId)
                .setIssuer(jitsiAppId)
                .claim("room", roomName)
                .claim("context", context)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, jitsiAppSecret.getBytes())
                .compact();
    }
    //#endregion
}