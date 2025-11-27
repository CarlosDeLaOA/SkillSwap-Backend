package com.project.skillswap.logic.entity.LearningSession;

import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import com.project.skillswap.logic.entity.UserSkill.UserSkill;
import com.project.skillswap.logic.entity.UserSkill.UserSkillRepository;
import com.project.skillswap.logic.entity.LearningSession.persistence.SessionIntegrationLogger;
import com.project.skillswap.logic.entity.LearningSession.persistence.SessionPersistenceValidator;
import com.project.skillswap.logic.entity.LearningSession.scheduling.SessionGoogleCalendarService;
import com.project.skillswap.logic.entity.LearningSession.scheduling.SessionScheduleValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    private SessionGoogleCalendarService googleCalendarService;

    @Autowired
    private SessionScheduleValidator scheduleValidator;

    @Autowired
    private SessionPersistenceValidator persistenceValidator;

    @Autowired
    private SessionIntegrationLogger integrationLogger;
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

    /**
     * Obtiene una sesión para estudiantes (inscripción)
     */
    @Transactional(readOnly = true)
    public LearningSession getSessionById(Long sessionId, Person authenticatedPerson) {
        Optional<LearningSession> sessionOptional = learningSessionRepository.findById(sessionId);

        if (sessionOptional.isEmpty()) {
            throw new IllegalArgumentException("La sesión no existe");
        }

        LearningSession session = sessionOptional.get();

        // No pueden inscribirse en su propia sesión
        if (authenticatedPerson.getInstructor() != null &&
                session.getInstructor().getId().equals(authenticatedPerson.getInstructor().getId())) {
            throw new IllegalArgumentException("No puedes inscribirte en tu propia sesión");
        }

        // Solo mostrar sesiones publicadas
        if (session.getStatus() != SessionStatus.SCHEDULED &&
                session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalArgumentException("Esta sesión no está disponible para inscripción");
        }

        return session;
    }

    /**
     * Obtiene sesión para el instructor
     */
    @Transactional(readOnly = true)
    public LearningSession getSessionByIdForInstructor(Long sessionId, Person authenticatedPerson) {
        validateInstructorRole(authenticatedPerson);

        Optional<LearningSession> sessionOptional = learningSessionRepository.findById(sessionId);

        if (sessionOptional.isEmpty()) {
            throw new IllegalArgumentException("La sesión no existe");
        }

        LearningSession session = sessionOptional.get();

        if (!session.getInstructor().getId().equals(authenticatedPerson.getInstructor().getId())) {
            throw new IllegalArgumentException("No tienes permiso para gestionar esta sesión");
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

        scheduleValidator.validateMinimumAdvanceTime(session.getScheduledDatetime());

        scheduleValidator.validateNoScheduleConflicts(
                instructor.getId(),
                session.getScheduledDatetime(),
                session.getDurationMinutes(),
                null
        );

        String language = validateAndNormalizeLanguage(session.getLanguage());
        session.setLanguage(language);

        Skill skill = validateAndGetSkill(session.getSkill());
        validateInstructorHasExpertSkill(authenticatedPerson.getId(), skill);

        session.setInstructor(instructor);
        session.setSkill(skill);
        session.setType(SessionType.SCHEDULED);
        session.setStatus(SessionStatus.DRAFT);

        LearningSession createdSession = learningSessionRepository.save(session);

        persistenceValidator.validateSessionSavedCorrectly(createdSession);
        integrationLogger.logSessionCreatedAsDraft(createdSession);

        return createdSession;
    }
    //#endregion

    //#region Public Methods - Publish
    @Transactional
    public LearningSession publishSession(Long sessionId, Person authenticatedPerson, Map<String, String> minorEdits) {
        validateInstructorRole(authenticatedPerson);

        LearningSession session = getSessionByIdForInstructor(sessionId, authenticatedPerson);

        validateSessionIsComplete(session);
        validateSessionStatusIsPublishable(session);

        if (minorEdits != null) {
            applyMinorEdits(session, minorEdits);
        }

        boolean enableGoogleCalendar = false;
        if (minorEdits != null && minorEdits.containsKey("enableGoogleCalendar")) {
            enableGoogleCalendar = "true".equalsIgnoreCase(minorEdits.get("enableGoogleCalendar"));
        }

        scheduleValidator.validateMinimumAdvanceTime(session.getScheduledDatetime());
        scheduleValidator.validateNoScheduleConflicts(
                session.getInstructor().getId(),
                session.getScheduledDatetime(),
                session.getDurationMinutes(),
                sessionId
        );

        SessionStatus newStatus = determinePublishStatus(session.getScheduledDatetime());
        session.setStatus(newStatus);

        if (enableGoogleCalendar) {
            String googleCalendarEventId = googleCalendarService.tryCreateCalendarEvent(
                    session,
                    authenticatedPerson,
                    true
            );

            if (googleCalendarEventId != null) {
                session.setGoogleCalendarId(googleCalendarEventId);
            }
        }

        LearningSession publishedSession = learningSessionRepository.save(session);
        persistenceValidator.validateSessionSavedCorrectly(publishedSession);

        if (publishedSession.getGoogleCalendarId() != null) {
            integrationLogger.logSessionSavedWithIntegration(
                    publishedSession,
                    publishedSession.getGoogleCalendarId()
            );
        } else {
            integrationLogger.logSessionSavedWithoutIntegration(publishedSession);
        }

        try {
            sessionEmailService.sendSessionCreationEmail(publishedSession, authenticatedPerson);
        } catch (Exception ignored) {}

        return publishedSession;
    }
    //#endregion

    //#region Public Methods - Cancel
    @Transactional
    public LearningSession cancelSession(Long sessionId, Person authenticatedPerson, String reason) {
        validateInstructorRole(authenticatedPerson);

        LearningSession session = getSessionByIdForInstructor(sessionId, authenticatedPerson);

        validateSessionCanBeCancelled(session);
        validateIsSessionOwner(session, authenticatedPerson);

        List<String> participantEmails = session.getBookings().stream()
                .map(b -> b.getLearner().getPerson().getEmail())
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
                sessionNotificationService.sendCancellationNotifications(
                        cancelledSession,
                        participantEmails
                );
            } catch (Exception ignored) {}
        }

        if (cancelledSession.getGoogleCalendarId() != null) {
            googleCalendarService.tryDeleteCalendarEvent(
                    cancelledSession.getGoogleCalendarId(),
                    authenticatedPerson.getEmail()
            );
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
                    String.format(
                            "La duración debe estar entre %d y %d minutos",
                            MIN_DURATION_MINUTES, MAX_DURATION_MINUTES
                    )
            );
        }
    }

    private void validateCapacity(Integer maxCapacity) {
        if (maxCapacity == null || maxCapacity <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser un valor positivo");
        }

        if (maxCapacity < MIN_CAPACITY || maxCapacity > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                    String.format(
                            "La capacidad máxima debe estar entre %d y %d participantes",
                            MIN_CAPACITY, MAX_CAPACITY
                    )
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

        String normalized = language.trim().toLowerCase();

        if (!VALID_LANGUAGES.contains(normalized)) {
            return DEFAULT_LANGUAGE;
        }

        return normalized;
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
                    String.format(
                            "No tienes la habilidad '%s' en tu perfil. Solo puedes crear sesiones de habilidades que dominas.",
                            skill.getName()
                    )
            );
        }
    }

    private void validateSessionIsComplete(LearningSession session) {
        List<String> missing = new ArrayList<>();

        if (isEmpty(session.getTitle())) missing.add("título");
        if (isEmpty(session.getDescription())) missing.add("descripción");
        if (session.getSkill() == null) missing.add("habilidad");
        if (session.getScheduledDatetime() == null) missing.add("fecha y hora");
        if (session.getDurationMinutes() == null) missing.add("duración");
        if (session.getMaxCapacity() == null) missing.add("capacidad");

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "La sesión está incompleta. Campos pendientes: " + String.join(", ", missing)
            );
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void validateSessionStatusIsPublishable(LearningSession session) {
        if (session.getStatus() == null) {
            throw new IllegalArgumentException("La sesión no tiene estado definido");
        }

        if (session.getStatus() != SessionStatus.DRAFT) {
            String msg = switch (session.getStatus()) {
                case SCHEDULED -> "Esta sesión ya está programada. No puedes reprogramarla.";
                case ACTIVE -> "Esta sesión ya está activa. No puedes cambiarla.";
                case FINISHED -> "Esta sesión ya ha finalizado. No puedes modificarla.";
                case CANCELLED -> "Esta sesión ha sido cancelada. No puedes modificarla.";
                default -> "Esta sesión no puede ser programada en su estado actual.";
            };
            throw new IllegalArgumentException(msg);
        }
    }
    //#endregion

    //#region Minor Edits
    private void applyMinorEdits(LearningSession session, Map<String, String> minorEdits) {
        String newTitle = minorEdits.get("title");
        String newDescription = minorEdits.get("description");

        if (!isEmpty(newTitle)) {
            validateMinorEdit(session.getTitle(), newTitle, "título");
            validateTitle(newTitle);
            session.setTitle(newTitle.trim());
        }

        if (!isEmpty(newDescription)) {
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
                    String.format(
                            "El cambio en %s excede el 50%% permitido. Considera esto como una edición mayor.",
                            fieldName
                    )
            );
        }
    }
    //#endregion

    //#region Cancel Validation
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

    //#region Business Logic
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
