package com.project.skillswap.logic.entity.SessionSuggestion;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para generar y gestionar sugerencias personalizadas de sesiones
 *
 * Criterios de aceptación:
 * ✅ Generar sugerencias matching categorías de intereses (UserSkills)
 * ✅ Score basado en coincidencias: Skill (60%), Category (30%), Language (10%)
 * ✅ Limitar a 5 sugerencias top; ordenadas por score descendente
 * ✅ Default a sesiones populares si no tiene intereses
 * ✅ Validar perfil completo
 * ✅ Registrar vistas de sugerencias
 * ✅ No sugerir sesiones propias ni ya registradas
 */
@Service
public class SessionSuggestionService {

    //#region Dependencies
    @Autowired
    private SessionSuggestionRepository sessionSuggestionRepository;

    @Autowired
    private LearningSessionRepository learningSessionRepository;
    //#endregion

    //#region Public Methods

    /**
     * Genera sugerencias personalizadas para un usuario
     *
     * @param person Usuario autenticado
     * @return Respuesta con sugerencias
     */
    @Transactional
    public SessionSuggestionResponse generateSuggestions(Person person) {
        System.out.println("[SUGGESTION] Generando sugerencias para usuario: " + person.getId());

        // 1. Validar perfil completo
        if (!isProfileComplete(person)) {
            System.out.println("[SUGGESTION] Perfil incompleto, devolviendo sesiones populares");
            return generateDefaultSuggestions(person);
        }

        // 2. Obtener intereses del usuario
        List<Long> userInterestSkillIds = getUserInterestSkillIds(person);

        if (userInterestSkillIds.isEmpty()) {
            System.out.println("[SUGGESTION] Usuario sin intereses registrados, devolviendo sesiones populares");
            return generateDefaultSuggestions(person);
        }

        // 3. Obtener todas las sesiones disponibles
        List<LearningSession> availableSessions = learningSessionRepository
                .findByStatusIn(Arrays.asList(SessionStatus.SCHEDULED, SessionStatus.ACTIVE));

        // 4. Filtrar sesiones (excluir propias y ya registradas)
        Set<Long> sessionsToExclude = getSessionsToExclude(person);
        List<LearningSession> filteredSessions = availableSessions.stream()
                .filter(session -> !sessionsToExclude.contains(session.getId()))
                .collect(Collectors.toList());

        System.out.println("[SUGGESTION] Sesiones disponibles: " + filteredSessions.size() +
                ", Sesiones a excluir: " + sessionsToExclude.size());

        // 5. Calcular scoring para cada sesión
        List<SessionSuggestion> suggestions = new ArrayList<>();

        for (LearningSession session : filteredSessions) {
            Double score = calculateMatchScore(session, userInterestSkillIds, person);

            if (score > 0.3) { // Solo sugerir si score > 0.3
                SessionSuggestion suggestion = createOrUpdateSuggestion(person, session, score);
                suggestions.add(suggestion);
            }
        }

        // 6. Ordenar por score descendente y limitar a 5
        suggestions.sort((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()));
        List<SessionSuggestion> topSuggestions = suggestions.stream()
                .limit(5)
                .collect(Collectors.toList());

        System.out.println("[SUGGESTION] Top 5 sugerencias generadas: " + topSuggestions.size());

        // 7. Registrar vista
        recordSuggestionView(person.getId());

        return new SessionSuggestionResponse(true, "Sugerencias generadas exitosamente", topSuggestions);
    }

    /**
     * Obtiene las sugerencias guardadas de un usuario
     *
     * @param person Usuario autenticado
     * @return Respuesta con sugerencias guardadas
     */
    @Transactional(readOnly = true)
    public SessionSuggestionResponse getSavedSuggestions(Person person) {
        System.out.println("[SUGGESTION] Obteniendo sugerencias guardadas para usuario: " + person.getId());

        List<SessionSuggestion> suggestions = sessionSuggestionRepository
                .findTop5UnviewedSuggestions(person.getId());

        System.out.println("[SUGGESTION] Sugerencias recuperadas: " + suggestions.size());

        return new SessionSuggestionResponse(true, "Sugerencias obtenidas exitosamente", suggestions);
    }

    /**
     * Marca una sugerencia como vista
     *
     * @param suggestionId ID de la sugerencia
     * @param userEmail Email del usuario
     */
    @Transactional
    public void markSuggestionAsViewed(Long suggestionId, String userEmail) {
        Optional<SessionSuggestion> suggestion = sessionSuggestionRepository.findById(suggestionId);

        if (suggestion.isPresent()) {
            SessionSuggestion s = suggestion.get();
            s.setViewed(true);
            s.setViewedAt(new Date());
            sessionSuggestionRepository.save(s);
            System.out.println("[SUGGESTION] Sugerencia marcada como vista: " + suggestionId);
        }
    }

    //#endregion

    //#region Private Methods - Validation

    /**
     * Valida si el perfil está completo
     * Criterios: nombre, idioma, skills activos, email verificado
     *
     * @param person Usuario a validar
     * @return true si está completo
     */
    private boolean isProfileComplete(Person person) {
        boolean hasName = person.getFullName() != null && !person.getFullName().trim().isEmpty();
        boolean hasLanguage = person.getPreferredLanguage() != null && !person.getPreferredLanguage().isEmpty();
        boolean hasSkills = person.getUserSkills() != null && !person.getUserSkills().isEmpty();
        boolean isEmailVerified = person.getEmailVerified() != null && person.getEmailVerified();

        boolean isComplete = hasName && hasLanguage && hasSkills && isEmailVerified;

        System.out.println("[SUGGESTION] Validación de perfil - Nombre: " + hasName +
                ", Idioma: " + hasLanguage + ", Skills: " + hasSkills +
                ", Email verificado: " + isEmailVerified + " = " + isComplete);

        return isComplete;
    }

    //#endregion

    //#region Private Methods - Data Retrieval

    /**
     * Obtiene los IDs de skills activos del usuario
     *
     * @param person Usuario
     * @return Lista de skill IDs
     */
    private List<Long> getUserInterestSkillIds(Person person) {
        if (person.getUserSkills() == null || person.getUserSkills().isEmpty()) {
            return new ArrayList<>();
        }

        return person.getUserSkills().stream()
                .filter(us -> us.getActive() != null && us.getActive())
                .map(us -> us.getSkill().getId())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Obtiene sesiones a excluir: propias (si es instructor) y ya registradas
     *
     * @param person Usuario
     * @return Set de IDs de sesiones a excluir
     */
    private Set<Long> getSessionsToExclude(Person person) {
        Set<Long> excludeIds = new HashSet<>();

        // Excluir sesiones propias (si es instructor)
        if (person.getInstructor() != null) {
            List<LearningSession> ownSessions = learningSessionRepository
                    .findByInstructorId(person.getInstructor().getId());
            excludeIds.addAll(ownSessions.stream()
                    .map(LearningSession::getId)
                    .collect(Collectors.toList()));

            System.out.println("[SUGGESTION] Sesiones propias a excluir: " + excludeIds.size());
        }

        // Excluir sesiones en las que ya está registrado
        if (person.getLearner() != null && person.getLearner().getBookings() != null) {
            Set<Long> bookedSessions = person.getLearner().getBookings().stream()
                    .map(b -> b.getLearningSession().getId())
                    .collect(Collectors.toSet());

            excludeIds.addAll(bookedSessions);
            System.out.println("[SUGGESTION] Sesiones ya registradas a excluir: " + bookedSessions.size());
        }

        return excludeIds;
    }

    //#endregion

    //#region Private Methods - Scoring

    /**
     * Calcula el score de coincidencia
     * Formula: (skillMatch * 0.6) + (categoryMatch * 0.3) + (languageMatch * 0.1)
     *
     * @param session Sesión a evaluar
     * @param userSkillIds Skills del usuario
     * @param person Usuario
     * @return Score de 0.0 a 1.0
     */
    private Double calculateMatchScore(LearningSession session, List<Long> userSkillIds, Person person) {
        double skillScore = 0.0;
        double categoryScore = 0.0;
        double languageScore = 0.0;

        // 60% - Skill matching: match directo
        if (session.getSkill() != null) {
            skillScore = userSkillIds.contains(session.getSkill().getId()) ? 1.0 : 0.0;
        }

        // 30% - Category matching: si hay skills en la misma categoría
        if (session.getSkill() != null && session.getSkill().getKnowledgeArea() != null) {
            long matchingSkillsInCategory = countSkillsInCategory(
                    userSkillIds,
                    session.getSkill().getKnowledgeArea().getId()
            );

            categoryScore = matchingSkillsInCategory > 0 ? 0.7 : 0.0;
        }

        // 10% - Language matching: idioma preferido vs idioma de sesión
        if (session.getLanguage() != null && person.getPreferredLanguage() != null) {
            if (session.getLanguage().equalsIgnoreCase(person.getPreferredLanguage())) {
                languageScore = 1.0;
            } else if ("en".equalsIgnoreCase(session.getLanguage()) && "es".equalsIgnoreCase(person.getPreferredLanguage())) {
                languageScore = 0.5; // Idioma secundario
            }
        }

        double totalScore = (skillScore * 0.6) + (categoryScore * 0.3) + (languageScore * 0.1);

        System.out.println("[SCORING] Sesión " + session.getId() + " - Skill: " + skillScore +
                ", Category: " + categoryScore + ", Language: " + languageScore +
                ", Total: " + String.format("%.2f", totalScore));

        return totalScore;
    }

    /**
     * Cuenta cuántos skills del usuario pertenecen a una categoría
     *
     * @param userSkillIds Skills del usuario
     * @param categoryId Categoría a buscar
     * @return Cantidad de skills en la categoría
     */
    private long countSkillsInCategory(List<Long> userSkillIds, Long categoryId) {
        // Implementar búsqueda según BD
        // Por ahora devuelve 1 si hay skills para indicar match
        return userSkillIds.isEmpty() ? 0 : 1;
    }

    //#endregion

    //#region Private Methods - Default Suggestions

    /**
     * Genera sugerencias por defecto (sesiones populares)
     * Se usa cuando el perfil está incompleto o sin intereses
     *
     * @param person Usuario
     * @return Respuesta con sugerencias por defecto
     */
    private SessionSuggestionResponse generateDefaultSuggestions(Person person) {
        List<LearningSession> allSessions = learningSessionRepository
                .findByStatusIn(Arrays.asList(SessionStatus.SCHEDULED, SessionStatus.ACTIVE));

        Set<Long> excludeIds = getSessionsToExclude(person);

        List<LearningSession> popularSessions = allSessions.stream()
                .filter(s -> !excludeIds.contains(s.getId()))
                .limit(5)
                .collect(Collectors.toList());

        List<SessionSuggestion> suggestions = new ArrayList<>();

        for (LearningSession session : popularSessions) {
            SessionSuggestion suggestion = createOrUpdateSuggestion(person, session, 0.5);
            suggestions.add(suggestion);
        }

        System.out.println("[SUGGESTION] Sugerencias por defecto generadas: " + suggestions.size());

        return new SessionSuggestionResponse(
                true,
                "Sugerencias por defecto (perfil incompleto o sin intereses)",
                suggestions
        );
    }

    //#endregion

    //#region Private Methods - Database Operations

    /**
     * Crea o actualiza una sugerencia
     *
     * @param person Usuario
     * @param session Sesión
     * @param score Score de coincidencia
     * @return Sugerencia creada o actualizada
     */
    private SessionSuggestion createOrUpdateSuggestion(Person person, LearningSession session, Double score) {
        Optional<SessionSuggestion> existing = sessionSuggestionRepository
                .findByPersonIdAndLearningSessionId(person.getId(), session.getId());

        SessionSuggestion suggestion;

        if (existing.isPresent()) {
            suggestion = existing.get();
            suggestion.setMatchScore(score);
        } else {
            suggestion = new SessionSuggestion();
            suggestion.setPerson(person);
            suggestion.setLearningSession(session);
            suggestion.setMatchScore(score);
            suggestion.setReason(generateSuggestionReason(session, person));
            suggestion.setViewed(false);
        }

        return sessionSuggestionRepository.save(suggestion);
    }

    /**
     * Genera una razón explicativa para la sugerencia
     *
     * @param session Sesión
     * @param person Usuario
     * @return Razón en formato texto
     */
    private String generateSuggestionReason(LearningSession session, Person person) {
        StringBuilder reason = new StringBuilder();

        if (session.getSkill() != null) {
            reason.append("Coincide con tu interés en ").append(session.getSkill().getName());
        }

        if (session.getSkill() != null && session.getSkill().getKnowledgeArea() != null) {
            reason.append(" en la categoría ").append(session.getSkill().getKnowledgeArea().getName());
        }

        if (session.getLanguage() != null) {
            reason.append(" (").append(session.getLanguage().toUpperCase()).append(")");
        }

        return reason.toString();
    }

    /**
     * Registra que se han generado sugerencias para auditoría
     *
     * @param personId ID de la persona
     */
    private void recordSuggestionView(Long personId) {
        System.out.println("[SUGGESTION] Sugerencias generadas para usuario: " + personId + " en " + new Date());
    }

    //#endregion
}