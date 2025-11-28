package com.project.skillswap. logic.entity.LearningSession;

import com.project.skillswap.logic.entity. Instructor. Instructor;
import com.project.skillswap.logic.entity.Person. Person;
import com.project. skillswap.logic.entity. Skill.Skill;
import com.project.skillswap.logic.entity. Skill.SkillRepository;
import com.project.skillswap.logic.entity.UserSkill.UserSkill;
import com.project.skillswap. logic.entity.UserSkill. UserSkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

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
    /**
     * Obtiene todas las sesiones disponibles (SCHEDULED o ACTIVE recientes)
     *
     * @return Lista de sesiones disponibles
     */
    @Transactional(readOnly = true)
    public List<LearningSession> getAvailableSessions() {
        Date currentDate = new Date();
        Date fiveMinutesAgo = getFiveMinutesAgo(currentDate);

        return learningSessionRepository.findAvailableSessions(currentDate, fiveMinutesAgo);
    }

    /**
     * Obtiene sesiones filtradas por categoría y/o idioma
     *
     * @param categoryId ID de la categoría (opcional)
     * @param language Idioma de la sesión (opcional)
     * @return Lista de sesiones filtradas
     */
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

        if (language != null && !  language.isEmpty()) {
            return learningSessionRepository.findSessionsByLanguage(
                    currentDate, fiveMinutesAgo, language
            );
        }

        return getAvailableSessions();
    }

    /**
     * Obtiene una sesión por ID con validación de propiedad
     *
     * VALIDACIONES:
     * - Si es Instructor: debe ser propietario de la sesión
     * - Si es Learner: puede ver sesiones en estado SCHEDULED o ACTIVE
     * - Si no tiene rol: lanza excepción
     *
     * @param sessionId ID de la sesión
     * @param authenticatedPerson Persona autenticada
     * @return Sesión encontrada
     * @throws IllegalArgumentException Si la sesión no existe o no tiene acceso
     * @throws IllegalStateException Si no tiene rol válido
     */
    @Transactional(readOnly = true)
    public LearningSession getSessionById(Long sessionId, Person authenticatedPerson) {
        // ✅ Paso 1: Validar que la sesión existe
        Optional<LearningSession> sessionOptional = learningSessionRepository.findById(sessionId);

        if (sessionOptional.isEmpty()) {
            throw new IllegalArgumentException("La sesión no existe");
        }

        LearningSession session = sessionOptional.get();

        // ✅ Paso 2: Validar acceso según el rol del usuario
        // Si es Instructor: debe ser propietario
        if (authenticatedPerson.getInstructor() != null) {
            if (! session.getInstructor().getId().equals(authenticatedPerson.getInstructor().getId())) {
                throw new IllegalArgumentException("No tienes permiso para acceder a esta sesión");
            }
        }
        // Si es Learner: puede ver sesiones SCHEDULED o ACTIVE
        else if (authenticatedPerson.getLearner() != null) {
            if (session.getStatus() != SessionStatus.SCHEDULED && session.getStatus() != SessionStatus.ACTIVE) {
                throw new IllegalArgumentException("Esta sesión no está disponible para visualizar");
            }
        }
        // Si no tiene rol válido: error
        else {
            throw new IllegalStateException("Usuario sin rol válido (instructor o learner)");
        }

        return session;
    }

    /**
     * Obtiene una sesión por ID con validación de propiedad (solo para instructores)
     * Usado por endpoints de gestión de sesiones.
     * Solo el propietario (instructor) de la sesión puede acceder a ella.
     *
     * @param sessionId ID de la sesión
     * @param authenticatedPerson Persona autenticada (debe ser instructor propietario)
     * @return Sesión encontrada
     * @throws IllegalStateException Si no es instructor
     * @throws IllegalArgumentException Si no es propietario o no existe la sesión
     */
    @Transactional(readOnly = true)
    public LearningSession getSessionByIdForInstructor(Long sessionId, Person authenticatedPerson) {
        // ✅ Paso 1: Validar que sea instructor
        validateInstructorRole(authenticatedPerson);

        // ✅ Paso 2: Obtener sesión
        Optional<LearningSession> sessionOptional = learningSessionRepository.findById(sessionId);

        if (sessionOptional.isEmpty()) {
            throw new IllegalArgumentException("La sesión no existe");
        }

        LearningSession session = sessionOptional.get();

        // ✅ Paso 3: Validar que sea propietario
        if (! session.getInstructor().getId().equals(authenticatedPerson.getInstructor().getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a esta sesión");
        }

        return session;
    }
    //#endregion

    //#region Public Methods - Create
    /**
     * Crea una nueva sesión y genera el videoCallLink inmediatamente
     *
     * FLUJO:
     * 1.Valida que el usuario sea instructor
     * 2.   Valida todos los campos obligatorios
     * 3.   Guarda la sesión en DRAFT
     * 4. Genera automáticamente el enlace de videollamada: http://frontend/app/video-call/{sessionId}
     * 5.Guarda el enlace en la BD
     *
     * @param session Sesión a crear con todos los datos
     * @param authenticatedPerson Persona autenticada que crea la sesión
     * @return Sesión creada y guardada con videoCallLink
     * @throws IllegalArgumentException Si las validaciones fallan
     * @throws IllegalStateException Si el usuario no es instructor
     */
    @Transactional
    public LearningSession createSession(LearningSession session, Person authenticatedPerson) {
        // ✅ Paso 1: Validar rol de instructor
        validateInstructorRole(authenticatedPerson);

        // ✅ Paso 2: Obtener instructor del usuario autenticado
        Instructor instructor = authenticatedPerson.getInstructor();

        // ✅ Paso 3: Validar todos los campos obligatorios
        validateTitle(session.getTitle());
        validateDescription(session.getDescription());
        validateDuration(session. getDurationMinutes());
        validateCapacity(session.getMaxCapacity());
        validateScheduledDatetime(session.getScheduledDatetime());

        // ✅ Paso 4: Validar y normalizar idioma
        String language = validateAndNormalizeLanguage(session.getLanguage());
        session.setLanguage(language);

        // ✅ Paso 5: Validar que el instructor tiene la skill
        Skill skill = validateAndGetSkill(session.getSkill());
        validateInstructorHasExpertSkill(authenticatedPerson. getId(), skill);

        // ✅ Paso 6: Asignar valores iniciales
        session.setInstructor(instructor);
        session. setSkill(skill);
        session. setType(SessionType.SCHEDULED);
        session.setStatus(SessionStatus.DRAFT);

        // ✅ Paso 7: Guardar sesión en BD para obtener ID
        LearningSession savedSession = learningSessionRepository.save(session);

        System.out.println("📝 [LearningSessionService] Session created with ID: " + savedSession.getId());

        // ✅ Paso 8: GENERAR ENLACE DE VIDEOLLAMADA
        // Formato: http://localhost:4200/app/video-call/{sessionId}
        String videoCallLink = frontendBaseUrl + "/app/video-call/" + savedSession.getId();
        savedSession.setVideoCallLink(videoCallLink);

        // ✅ Paso 9: Guardar sesión con el enlace
        savedSession = learningSessionRepository.save(savedSession);

        System.out.println("🔗 [LearningSessionService] Video call link assigned: " + videoCallLink);

        return savedSession;
    }
    //#endregion

    //#region Public Methods - Publish
    /**
     * Publica una sesión cambiando su estado y haciéndola visible
     *
     * FLUJO:
     * 1.   Valida que el usuario es instructor propietario
     * 2.   Valida que la sesión esté completa
     * 3. Aplica ediciones menores si existen (título, descripción)
     * 4.Determina el estado: ACTIVE si comienza en <30 min, SCHEDULED si es después
     * 5.Verifica que el enlace de videollamada existe (genera si falta)
     * 6.   Guarda cambios
     * 7.Envía email de confirmación al instructor
     *
     * @param sessionId ID de la sesión a publicar
     * @param authenticatedPerson Persona autenticada
     * @param minorEdits Ediciones menores opcionales (título y descripción)
     * @return Sesión publicada
     * @throws IllegalArgumentException Si las validaciones fallan
     */
    @Transactional
    public LearningSession publishSession(Long sessionId, Person authenticatedPerson, Map<String, String> minorEdits) {
        // ✅ Paso 1: Validar rol de instructor
        validateInstructorRole(authenticatedPerson);

        // ✅ Paso 2: Obtener sesión y validar permisos
        LearningSession session = getSessionByIdForInstructor(sessionId, authenticatedPerson);

        // ✅ Paso 3: Validar que la sesión esté completa
        validateSessionIsComplete(session);

        // ✅ Paso 4: Aplicar ediciones menores si existen
        if (minorEdits != null) {
            applyMinorEdits(session, minorEdits);
        }

        // ✅ Paso 5: Determinar estado final (SCHEDULED o ACTIVE)
        SessionStatus newStatus = determinePublishStatus(session. getScheduledDatetime());
        session.setStatus(newStatus);

        // ✅ Paso 6: Guardar cambios
        LearningSession publishedSession = learningSessionRepository. save(session);

        // ✅ Paso 7: VALIDAR O GENERAR ENLACE DE VIDEOLLAMADA
        // Si por algún motivo no existe, lo generamos aquí
        if (publishedSession.getVideoCallLink() == null ||
                publishedSession.getVideoCallLink().trim().isEmpty()) {

            String videoCallLink = frontendBaseUrl + "/app/video-call/" + publishedSession. getId();
            publishedSession. setVideoCallLink(videoCallLink);
            publishedSession = learningSessionRepository.save(publishedSession);

            System.out.println(" [LearningSessionService] Video call link was missing, assigned: " + videoCallLink);
        }

        // ✅ Paso 8: Enviar email de confirmación al instructor
        try {
            boolean emailSent = sessionEmailService.sendSessionCreationEmail(
                    publishedSession,
                    authenticatedPerson
            );

            if (emailSent) {
                System.out.println(" [LearningSessionService] Email de confirmación enviado");
            } else {
                System.out.println(" [LearningSessionService] Email no enviado (validación fallida)");
            }
        } catch (Exception e) {
            System.err.println(" [LearningSessionService] Error enviando email: " + e.getMessage());
        }

        return publishedSession;
    }
    //#endregion

    //#region Public Methods - Cancel
    /**
     * Cancela una sesión de aprendizaje con validaciones completas
     *
     * FLUJO:
     * 1.Valida que el usuario es instructor propietario
     * 2.Valida que la sesión puede cancelarse
     * 3.   Obtiene lista de participantes registrados
     * 4.  Cambia estado a CANCELLED
     * 5. Registra razón y fecha de cancelación
     * 6.  Envía notificaciones a todos los participantes
     *
     * @param sessionId ID de la sesión a cancelar
     * @param authenticatedPerson Persona autenticada que cancela
     * @param reason Razón de cancelación (opcional)
     * @return Sesión cancelada con metadata actualizada
     * @throws IllegalArgumentException Si las validaciones fallan
     * @throws IllegalStateException Si el usuario no es el creador
     */
    @Transactional
    public LearningSession cancelSession(Long sessionId, Person authenticatedPerson, String reason) {
        // ✅ Paso 1: Validar rol de instructor
        validateInstructorRole(authenticatedPerson);

        // ✅ Paso 2: Obtener sesión con validación de permisos
        LearningSession session = getSessionByIdForInstructor(sessionId, authenticatedPerson);

        // ✅ Paso 3: Validar que puede cancelarse
        validateSessionCanBeCancelled(session);
        validateIsSessionOwner(session, authenticatedPerson);

        // ⚠️ Advertencia si está ACTIVE
        if (session.getStatus() == SessionStatus.ACTIVE) {
            System.out.println("️ [WARNING] Cancelling ACTIVE session - requires additional confirmation");
        }

        // ✅ Paso 4: Obtener lista de participantes para notificar
        List<String> participantEmails = session.getBookings().stream()
                .map(booking -> booking.getLearner().getPerson().getEmail())
                .filter(email -> email != null && !  email.isEmpty())
                .toList();

        int participantsCount = participantEmails.size();

        // ✅ Paso 5: Actualizar estado de la sesión
        session.setStatus(SessionStatus.CANCELLED);
        session.setCancellationReason(reason != null ?  reason. trim() : "Sin razón especificada");
        session.setCancellationDate(new Date());
        session.setCancelledByInstructorId(authenticatedPerson.getInstructor().getId());

        // ✅ Paso 6: Guardar cambios
        LearningSession cancelledSession = learningSessionRepository.save(session);

        System.out.println(String.format(
                " [SUCCESS] Session %d cancelled by instructor %d.  Participants to notify: %d",
                sessionId,
                authenticatedPerson.getInstructor().getId(),
                participantsCount
        ));

        // ✅ Paso 7: Enviar notificaciones a participantes (asíncrono)
        if (!  participantEmails.isEmpty()) {
            try {
                int emailsSent = sessionNotificationService.sendCancellationNotifications(
                        cancelledSession,
                        participantEmails
                );
                System.out.println(String.format(
                        "📧 [EMAIL] Sent %d/%d cancellation notifications",
                        emailsSent,
                        participantsCount
                ));
            } catch (Exception e) {
                System.err.println(" [ERROR] Failed to send some notification emails: " + e.getMessage());
            }
        }

        return cancelledSession;
    }
    //#endregion

    //#region Private Methods - Validation
    /**
     * Valida que el usuario tenga rol de Instructor
     *
     * @param person Persona a validar
     * @throws IllegalStateException Si no es instructor
     */
    private void validateInstructorRole(Person person) {
        if (person. getInstructor() == null) {
            throw new IllegalStateException("rol no autorizado");
        }
    }

    /**
     * Valida el título de la sesión
     *
     * @param title Título a validar
     * @throws IllegalArgumentException Si el título no cumple los requisitos
     */
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

    /**
     * Valida la descripción de la sesión
     *
     * @param description Descripción a validar
     * @throws IllegalArgumentException Si la descripción no cumple los requisitos
     */
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

    /**
     * Valida la duración de la sesión
     *
     * @param durationMinutes Duración en minutos
     * @throws IllegalArgumentException Si la duración está fuera del rango permitido
     */
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

    /**
     * Valida la capacidad máxima de participantes
     *
     * @param maxCapacity Capacidad máxima
     * @throws IllegalArgumentException Si la capacidad está fuera del rango permitido
     */
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

    /**
     * Valida la fecha y hora programada
     *
     * @param scheduledDatetime Fecha y hora programada
     * @throws IllegalArgumentException Si la fecha es nula o en el pasado
     */
    private void validateScheduledDatetime(Date scheduledDatetime) {
        if (scheduledDatetime == null) {
            throw new IllegalArgumentException("La fecha y hora de la sesión son obligatorias");
        }

        Date now = new Date();
        if (scheduledDatetime.before(now)) {
            throw new IllegalArgumentException("La fecha y hora de la sesión no pueden estar en el pasado");
        }
    }

    /**
     * Valida y normaliza el idioma de la sesión
     *
     * @param language Idioma solicitado
     * @return Idioma validado o español por defecto
     */
    private String validateAndNormalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return DEFAULT_LANGUAGE;
        }

        String normalizedLanguage = language.trim().toLowerCase();

        if (!  VALID_LANGUAGES.contains(normalizedLanguage)) {
            return DEFAULT_LANGUAGE;
        }

        return normalizedLanguage;
    }

    /**
     * Valida que el skill existe, está activo y lo extrae del objeto
     *
     * @param skill Skill a validar
     * @return Skill validado desde la base de datos
     * @throws IllegalArgumentException Si el skill no existe o no está activo
     */
    private Skill validateAndGetSkill(Skill skill) {
        if (skill == null || skill.getId() == null) {
            throw new IllegalArgumentException("La habilidad es obligatoria");
        }

        Optional<Skill> skillOptional = skillRepository.findById(skill.getId());

        if (skillOptional.isEmpty()) {
            throw new IllegalArgumentException("La habilidad seleccionada no existe");
        }

        Skill dbSkill = skillOptional.get();

        if (!  dbSkill.getActive()) {
            throw new IllegalArgumentException("La habilidad seleccionada no está activa");
        }

        return dbSkill;
    }

    /**
     * Valida que el instructor tiene la skill como experta
     *
     * @param personId ID de la persona
     * @param skill Skill a validar
     * @throws IllegalArgumentException Si el instructor no tiene la skill
     */
    private void validateInstructorHasExpertSkill(Long personId, Skill skill) {
        List<UserSkill> userSkills = userSkillRepository.findActiveUserSkillsByPersonId(personId);

        boolean hasSkill = userSkills.stream()
                .anyMatch(us -> us.getSkill().getId().equals(skill.getId()));

        if (!hasSkill) {
            throw new IllegalArgumentException(
                    String.format("No tienes la habilidad '%s' en tu perfil.  " +
                                    "Solo puedes crear sesiones de habilidades que dominas.",
                            skill.getName())
            );
        }
    }

    /**
     * Valida que la sesión esté completa antes de publicar
     *
     * @param session Sesión a validar
     * @throws IllegalArgumentException Si falta algún campo obligatorio
     */
    private void validateSessionIsComplete(LearningSession session) {
        List<String> missingFields = new ArrayList<>();

        if (session.getTitle() == null || session.getTitle().trim().isEmpty()) {
            missingFields.add("título");
        }
        if (session.getDescription() == null || session.getDescription(). trim().isEmpty()) {
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
    /**
     * Aplica ediciones menores a título y descripción
     *
     * @param session Sesión a editar
     * @param minorEdits Ediciones a aplicar
     * @throws IllegalArgumentException Si las ediciones exceden el 50% de cambio
     */
    private void applyMinorEdits(LearningSession session, Map<String, String> minorEdits) {
        String newTitle = minorEdits.get("title");
        String newDescription = minorEdits.get("description");

        if (newTitle != null && !  newTitle.trim().isEmpty()) {
            validateMinorEdit(session. getTitle(), newTitle, "título");
            validateTitle(newTitle);
            session.setTitle(newTitle. trim());
        }

        if (newDescription != null && ! newDescription.trim().isEmpty()) {
            validateMinorEdit(session. getDescription(), newDescription, "descripción");
            validateDescription(newDescription);
            session.setDescription(newDescription.trim());
        }
    }

    /**
     * Valida que un cambio menor no exceda el 50% de la longitud original
     *
     * @param original Texto original
     * @param edited Texto editado
     * @param fieldName Nombre del campo
     * @throws IllegalArgumentException Si el cambio excede el 50%
     */
    private void validateMinorEdit(String original, String edited, String fieldName) {
        int originalLength = original.length();
        int editedLength = edited.length();

        double changePercentage = Math.abs(editedLength - originalLength) / (double) originalLength;

        if (changePercentage > MAX_EDIT_CHANGE_PERCENTAGE) {
            throw new IllegalArgumentException(
                    String.format("El cambio en %s excede el 50%% permitido.  " +
                            "Considera esto como una edición mayor.", fieldName)
            );
        }
    }
    //#endregion

    //#region Private Methods - Cancel Validation
    /**
     * Valida que la sesión pueda ser cancelada
     *
     * @param session Sesión a validar
     * @throws IllegalArgumentException Si la sesión ya está cancelada o finalizada
     */
    private void validateSessionCanBeCancelled(LearningSession session) {
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new IllegalArgumentException("La sesión ya está cancelada");
        }

        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new IllegalArgumentException("No se puede cancelar una sesión que ya finalizó");
        }
    }

    /**
     * Valida que el usuario sea el creador de la sesión
     *
     * @param session Sesión a validar
     * @param authenticatedPerson Persona autenticada
     * @throws IllegalStateException Si el usuario no es el creador
     */
    private void validateIsSessionOwner(LearningSession session, Person authenticatedPerson) {
        if (! session.getInstructor().getId().equals(authenticatedPerson.getInstructor().getId())) {
            throw new IllegalStateException("Solo el creador de la sesión puede cancelarla");
        }
    }
    //#endregion

    //#region Private Methods - Business Logic
    /**
     * Determina el estado de la sesión basándose en la fecha programada
     *
     * @param scheduledDatetime Fecha y hora programada
     * @return Estado de la sesión (ACTIVE o SCHEDULED)
     */
    private SessionStatus determineSessionStatus(Date scheduledDatetime) {
        Date now = new Date();
        long diffInMillis = scheduledDatetime.getTime() - now.getTime();
        long diffInMinutes = diffInMillis / (60 * 1000);

        if (diffInMinutes <= IMMEDIATE_SESSION_THRESHOLD_MINUTES) {
            return SessionStatus.ACTIVE;
        }

        return SessionStatus.SCHEDULED;
    }

    /**
     * Determina el estado al publicar (ACTIVE o SCHEDULED)
     *
     * @param scheduledDatetime Fecha programada
     * @return Estado apropiado para publicación
     */
    private SessionStatus determinePublishStatus(Date scheduledDatetime) {
        return determineSessionStatus(scheduledDatetime);
    }

    /**
     * Calcula la fecha de hace 5 minutos
     *
     * @param currentDate Fecha actual
     * @return Fecha de hace 5 minutos
     */
    private Date getFiveMinutesAgo(Date currentDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar. MINUTE, -5);
        return calendar.getTime();
    }
    //#endregion
}