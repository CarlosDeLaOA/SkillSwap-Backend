package com.project.skillswap.logic.entity.LearningSession;

import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import com.project.skillswap.logic.entity.UserSkill.UserSkill;
import com.project.skillswap.logic.entity.UserSkill.UserSkillRepository;
import com.project.skillswap.logic.entity.LearningSession.scheduling.SessionGoogleCalendarService;
import com.project.skillswap.logic.entity.LearningSession.scheduling.SessionScheduleValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

/**
 * Servicio para gestión de LearningSession:
 * - creación en DRAFT
 * - publicación (validaciones, opción de integración con Google Calendar)
 * - cancelación y otras operaciones relacionadas
 */
@Service
public class LearningSessionService {

    //#region Dependencies
    @Autowired
    private LearningSessionRepository learningSessionRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private SessionNotificationService sessionNotificationService;

    @Autowired
    private SessionEmailService sessionEmailService;

    @Autowired
    private SessionScheduleValidator scheduleValidator;

    @Autowired
    private SessionGoogleCalendarService sessionGoogleCalendarService;
    //#endregion

    //#region Configuration
    @Value("${app.frontend.url}")
    private String frontendBaseUrl;
    //#endregion

    //#region Constants
    private static final int MIN_TITLE_LENGTH = 5;
    private static final int MIN_DESCRIPTION_LENGTH = 20;
    private static final int MIN_DURATION_MINUTES = 15;
    private static final int MAX_DURATION_MINUTES = 240;
    private static final int MIN_CAPACITY = 1;
    private static final int MAX_CAPACITY = 50;
    private static final String DEFAULT_LANGUAGE = "es";
    private static final Set<String> VALID_LANGUAGES = Set.of("es", "en");
    private static final long IMMEDIATE_SESSION_THRESHOLD_MINUTES = 30;
    private static final double MAX_EDIT_CHANGE_PERCENTAGE = 0.50;
    //#endregion

    //#region Public Methods - Query
    @Transactional(readOnly = true)
    public List<LearningSession> getAvailableSessions() {
        Date currentDate = new Date();
        Date fiveMinutesAgo = getFiveMinutesAgo(currentDate);

        return learningSessionRepository.findAvailableSessions(currentDate, fiveMinutesAgo);
    }

    @Transactional(readOnly = true)
    public List<LearningSession> getFilteredSessions(Long categoryId, String language) {
        Date currentDate = new Date();
        Date fiveMinutesAgo = getFiveMinutesAgo(currentDate);

        if (categoryId != null && language != null && !language.isEmpty()) {
            return learningSessionRepository.findSessionsByCategoryAndLanguage(
                    currentDate, fiveMinutesAgo, categoryId, language
            );
        }

        if (categoryId != null) {
            return learningSessionRepository.findSessionsByCategory(
                    currentDate, fiveMinutesAgo, categoryId
            );
        }

        if (language != null && !language.isEmpty()) {
            return learningSessionRepository.findSessionsByLanguage(
                    currentDate, fiveMinutesAgo, language
            );
        }

        return getAvailableSessions();
    }

    @Transactional(readOnly = true)
    public LearningSession getSessionById(Long sessionId, Person authenticatedPerson) {
        Optional<LearningSession> sessionOptional = learningSessionRepository.findById(sessionId);

        if (sessionOptional.isEmpty()) {
            throw new IllegalArgumentException("La sesión no existe");
        }

        LearningSession session = sessionOptional.get();

        if (!session.getInstructor().getId().equals(authenticatedPerson.getInstructor().getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a esta sesión");
        }

        return session;
    }
    //#endregion

    //#region Public Methods - Create
    @Transactional
    public LearningSession createSession(LearningSession session, Person authenticatedPerson) {
        validateInstructorRole(authenticatedPerson);

        Instructor instructor = authenticatedPerson.getInstructor();

        validateTitle(session.getTitle());
        validateDescription(session.getDescription());
        validateDuration(session.getDurationMinutes());
        validateCapacity(session.getMaxCapacity());
        validateScheduledDatetime(session.getScheduledDatetime());

        String language = validateAndNormalizeLanguage(session.getLanguage());
        session.setLanguage(language);

        Skill skill = validateAndGetSkill(session.getSkill());
        validateInstructorHasExpertSkill(authenticatedPerson.getId(), skill);

        session.setInstructor(instructor);
        session.setSkill(skill);
        session.setType(SessionType.SCHEDULED);
        session.setStatus(SessionStatus.DRAFT);

        LearningSession savedSession = learningSessionRepository.save(session);

        String videoCallLink = frontendBaseUrl + "/app/video-call/" + savedSession.getId();
        savedSession.setVideoCallLink(videoCallLink);

        savedSession = learning_session_save_video_link_fix(saved_session_copy(savedSession));

        return savedSession;
    }

    // Helper to avoid duplicate save code in older snippet conversions
    private LearningSession saved_session_copy(LearningSession s) {
        return s;
    }

    private LearningSession learning_session_save_video_link_fix(LearningSession s) {
        // Save change to add videoCallLink
        return learningSessionRepository.save(s);
    }
    //#endregion

    //#region Public Methods - Publish
    /**
     * Publica una sesión, opcionalmente sincronizando con Google Calendar.
     *
     * @param sessionId ID de la sesión a publicar
     * @param authenticatedPerson Persona autenticada
     * @param minorEdits Ediciones menores opcionales
     * @param enableIntegration Si true, intenta crear evento en Google Calendar (estricto: si falla, no publicar)
     * @return Sesión publicada
     */
    @Transactional
    public LearningSession publishSession(Long sessionId, Person authenticatedPerson, Map<String, String> minorEdits, boolean enableIntegration) {
        validateInstructorRole(authenticatedPerson);

        LearningSession session = getSessionById(sessionId, authenticatedPerson);

        validateSessionIsComplete(session);

        if (session.getStatus() != SessionStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden programar sesiones en estado pendiente (DRAFT)");
        }

        if (minorEdits != null) {
            applyMinorEdits(session, minorEdits);
        }

        // validar anticipación mínima y conflictos de horario
        scheduleValidator.validateMinimumAdvanceTime(session.getScheduledDatetime());
        scheduleValidator.validateNoScheduleConflicts(
                authenticatedPerson.getInstructor().getId(),
                session.getScheduledDatetime(),
                session.getDurationMinutes(),
                session.getId()
        );

        // Integración con Google Calendar (estricto)
        String googleCalendarEventId = null;
        if (enableIntegration) {
            googleCalendarEventId = sessionGoogleCalendarService.tryCreateCalendarEvent(session, authenticatedPerson, true);
            if (googleCalendarEventId == null) {
                throw new IllegalStateException("Fallo creando evento en Google Calendar. La sesión no fue publicada.");
            }
            session.setGoogleCalendarId(googleCalendarEventId);
        }

        session.setStatus(SessionStatus.SCHEDULED);

        LearningSession publishedSession;
        try {
            publishedSession = learningSessionRepository.save(session);
        } catch (Exception e) {
            if (googleCalendarEventId != null) {
                try {
                    sessionGoogleCalendarService.tryDeleteCalendarEvent(googleCalendarEventId, authenticatedPerson.getEmail());
                } catch (Exception ex) {
                    System.err.println("Error tratando de compensar (eliminar evento Google): " + ex.getMessage());
                }
            }
            throw e;
        }

        if (publishedSession.getVideoCallLink() == null ||
                publishedSession.getVideoCallLink().trim().isEmpty()) {

            String videoCallLink = frontendBaseUrl + "/app/video-call/" + publishedSession.getId();
            publishedSession.setVideoCallLink(videoCallLink);
            publishedSession = learningSessionRepository.save(publishedSession);
        }

        try {
            sessionEmailService.sendSessionCreationEmail(publishedSession, authenticatedPerson);
        } catch (Exception e) {
            System.err.println("Error enviando email de confirmación: " + e.getMessage());
        }

        return publishedSession;
    }
    //#endregion

    //#region Public Methods - Cancel
    @Transactional
    public LearningSession cancelSession(Long sessionId, Person authenticatedPerson, String reason) {
        validateInstructorRole(authenticatedPerson);

        LearningSession session = getSessionById(sessionId, authenticatedPerson);

        validateSessionCanBeCancelled(session);
        validateIsSessionOwner(session, authenticatedPerson);

        if (session.getStatus() == SessionStatus.ACTIVE) {
            System.out.println("Warning: Cancelling ACTIVE session - requires additional confirmation");
        }

        List<String> participantEmails = session.getBookings().stream()
                .map(booking -> booking.getLearner().getPerson().getEmail())
                .filter(email -> email != null && !email.isEmpty())
                .toList();

        int participantsCount = participantEmails.size();

        session.setStatus(SessionStatus.CANCELLED);
        session.setCancellationReason(reason != null ? reason.trim() : "Sin razón especificada");
        session.setCancellationDate(new Date());
        session.setCancelledByInstructorId(authenticatedPerson.getInstructor().getId());

        LearningSession cancelledSession = learningSessionRepository.save(session);

        if (!participantEmails.isEmpty()) {
            try {
                sessionNotificationService.sendCancellationNotifications(cancelledSession, participantEmails);
            } catch (Exception e) {
                System.err.println("Failed to send some notification emails: " + e.getMessage());
            }
        }

        return cancelledSession;
    }
    //#endregion

    //#region Private Methods - Validation
    private void validateInstructorRole(Person person) {
        if (person.getInstructor() == null) {
            throw new IllegalStateException("rol no autorizado");
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }

        if (title.trim().length() < MIN_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("El título debe tener al menos %d caracteres", MIN_TITLE_LENGTH)
            );
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }

        if (description.trim().length() < MIN_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("La descripción debe tener al menos %d caracteres", MIN_DESCRIPTION_LENGTH)
            );
        }
    }

    private void validateDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new IllegalArgumentException("La duración debe ser un valor positivo");
        }

        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException(
                    String.format("La duración debe estar entre %d y %d minutos",
                            MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
            );
        }
    }

    private void validateCapacity(Integer maxCapacity) {
        if (maxCapacity == null || maxCapacity <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser un valor positivo");
        }

        if (maxCapacity < MIN_CAPACITY || maxCapacity > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                    String.format("La capacidad máxima debe estar entre %d y %d participantes",
                            MIN_CAPACITY, MAX_CAPACITY)
            );
        }
    }

    private void validateScheduledDatetime(Date scheduledDatetime) {
        if (scheduledDatetime == null) {
            throw new IllegalArgumentException("La fecha y hora de la sesión son obligatorias");
        }

        Date now = new Date();
        if (scheduledDatetime.before(now)) {
            throw new IllegalArgumentException("La fecha y hora de la sesión no pueden estar en el pasado");
        }
    }

    private String validateAndNormalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return DEFAULT_LANGUAGE;
        }

        String normalizedLanguage = language.trim().toLowerCase();

        if (!VALID_LANGUAGES.contains(normalizedLanguage)) {
            return DEFAULT_LANGUAGE;
        }

        return normalizedLanguage;
    }

    private Skill validateAndGetSkill(Skill skill) {
        if (skill == null || skill.getId() == null) {
            throw new IllegalArgumentException("La habilidad es obligatoria");
        }

        Optional<Skill> skillOptional = skillRepository.findById(skill.getId());

        if (skillOptional.isEmpty()) {
            throw new IllegalArgumentException("La habilidad seleccionada no existe");
        }

        Skill dbSkill = skillOptional.get();

        if (!dbSkill.getActive()) {
            throw new IllegalArgumentException("La habilidad seleccionada no está activa");
        }

        return dbSkill;
    }

    private void validateInstructorHasExpertSkill(Long personId, Skill skill) {
        List<UserSkill> userSkills = userSkillRepository.findActiveUserSkillsByPersonId(personId);

        boolean hasSkill = userSkills.stream()
                .anyMatch(us -> us.getSkill().getId().equals(skill.getId()));

        if (!hasSkill) {
            throw new IllegalArgumentException(
                    String.format("No tienes la habilidad '%s' en tu perfil. Solo puedes crear sesiones de habilidades que dominas.",
                            skill.getName())
            );
        }
    }

    private void validateSessionIsComplete(LearningSession session) {
        List<String> missingFields = new ArrayList<>();

        if (session.getTitle() == null || session.getTitle().trim().isEmpty()) {
            missingFields.add("título");
        }
        if (session.getDescription() == null || session.getDescription().trim().isEmpty()) {
            missingFields.add("descripción");
        }
        if (session.getSkill() == null) {
            missingFields.add("habilidad");
        }
        if (session.getScheduledDatetime() == null) {
            missingFields.add("fecha y hora");
        }
        if (session.getDurationMinutes() == null) {
            missingFields.add("duración");
        }
        if (session.getMaxCapacity() == null) {
            missingFields.add("capacidad");
        }

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "La sesión está incompleta. Campos pendientes: " + String.join(", ", missingFields)
            );
        }
    }
    //#endregion

    //#region Private Methods - Minor Edits
    private void applyMinorEdits(LearningSession session, Map<String, String> minorEdits) {
        String newTitle = minorEdits.get("title");
        String newDescription = minorEdits.get("description");

        if (newTitle != null && !newTitle.trim().isEmpty()) {
            validateMinorEdit(session.getTitle(), newTitle, "título");
            validateTitle(newTitle);
            session.setTitle(newTitle.trim());
        }

        if (newDescription != null && !newDescription.trim().isEmpty()) {
            validateMinorEdit(session.getDescription(), newDescription, "descripción");
            validateDescription(newDescription);
            session.setDescription(newDescription.trim());
        }
    }

    private void validateMinorEdit(String original, String edited, String fieldName) {
        int originalLength = original.length();
        int editedLength = edited.length();

        double changePercentage = Math.abs(editedLength - originalLength) / (double) originalLength;

        if (changePercentage > MAX_EDIT_CHANGE_PERCENTAGE) {
            throw new IllegalArgumentException(
                    String.format("El cambio en %s excede el 50%% permitido. Considera esto como una edición mayor.", fieldName)
            );
        }
    }
    //#endregion

    //#region Private Methods - Cancel Validation
    private void validateSessionCanBeCancelled(LearningSession session) {
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new IllegalArgumentException("La sesión ya está cancelada");
        }

        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new IllegalArgumentException("No se puede cancelar una sesión que ya finalizó");
        }
    }

    private void validateIsSessionOwner(LearningSession session, Person authenticatedPerson) {
        if (!session.getInstructor().getId().equals(authenticatedPerson.getInstructor().getId())) {
            throw new IllegalStateException("Solo el creador de la sesión puede cancelarla");
        }
    }
    //#endregion

    //#region Private Methods - Business Logic
    private SessionStatus determineSessionStatus(Date scheduledDatetime) {
        Date now = new Date();
        long diffInMillis = scheduledDatetime.getTime() - now.getTime();
        long diffInMinutes = diffInMillis / (60 * 1000);

        if (diffInMinutes <= IMMEDIATE_SESSION_THRESHOLD_MINUTES) {
            return SessionStatus.ACTIVE;
        }

        return SessionStatus.SCHEDULED;
    }

    private SessionStatus determinePublishStatus(Date scheduledDatetime) {
        return determineSessionStatus(scheduledDatetime);
    }

    private Date getFiveMinutesAgo(Date currentDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.MINUTE, -5);
        return calendar.getTime();
    }
    //#endregion
}