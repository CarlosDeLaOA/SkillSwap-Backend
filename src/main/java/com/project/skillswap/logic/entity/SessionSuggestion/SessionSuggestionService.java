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

/*
 Servicio para generar y gestionar sugerencias de sesiones. ***
*/
@Service
public class SessionSuggestionService {

    @Autowired
    private SessionSuggestionRepository sessionSuggestionRepository;

    @Autowired
    private LearningSessionRepository learningSessionRepository;

    @Transactional
    public SessionSuggestionResponse generateSuggestions(Person person) {
        if (person == null) {
            return new SessionSuggestionResponse(false, "Person is null", Collections.emptyList());
        }

        if (!isProfileComplete(person)) {
            return generateDefaultSuggestions(person);
        }

        List<Long> userInterestSkillIds = getUserInterestSkillIds(person);

        if (userInterestSkillIds.isEmpty()) {
            return generateDefaultSuggestions(person);
        }

        List<LearningSession> availableSessions = learningSessionRepository
                .findByStatusIn(Arrays.asList(SessionStatus.SCHEDULED, SessionStatus.ACTIVE));

        Set<Long> sessionsToExclude = getSessionsToExclude(person);
        List<LearningSession> filteredSessions = availableSessions.stream()
                .filter(session -> !sessionsToExclude.contains(session.getId()))
                .collect(Collectors.toList());

        List<SessionSuggestion> suggestions = new ArrayList<>();

        for (LearningSession session : filteredSessions) {
            Double score = calculateMatchScore(session, userInterestSkillIds, person);
            if (score != null && score > 0.3) {
                SessionSuggestion suggestion = createOrUpdateSuggestion(person, session, score);
                suggestions.add(suggestion);
            }
        }

        suggestions.sort((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()));
        List<SessionSuggestion> topSuggestions = suggestions.stream().limit(5).collect(Collectors.toList());

        // opcional: persistir topSuggestions o realizar auditoría ***
        return new SessionSuggestionResponse(true, "Sugerencias generadas exitosamente", topSuggestions);
    }

    @Transactional(readOnly = true)
    public SessionSuggestionResponse getSavedSuggestions(Person person) {
        List<SessionSuggestion> suggestions = sessionSuggestionRepository.findTop5UnviewedSuggestions(person.getId());
        return new SessionSuggestionResponse(true, "Sugerencias obtenidas exitosamente", suggestions);
    }

    @Transactional
    public void markSuggestionAsViewed(Long suggestionId, String userEmail) {
        Optional<SessionSuggestion> suggestion = sessionSuggestionRepository.findById(suggestionId);
        if (suggestion.isPresent()) {
            SessionSuggestion s = suggestion.get();
            s.setViewed(true);
            s.setViewedAt(new Date());
            sessionSuggestionRepository.save(s);
        }
    }

    private boolean isProfileComplete(Person person) {
        boolean hasName = person.getFullName() != null && !person.getFullName().trim().isEmpty();
        boolean hasLanguage = person.getPreferredLanguage() != null && !person.getPreferredLanguage().isEmpty();
        boolean hasSkills = person.getUserSkills() != null && !person.getUserSkills().isEmpty();
        boolean isEmailVerified = person.getEmailVerified() != null && person.getEmailVerified();

        return hasName && hasLanguage && hasSkills && isEmailVerified;
    }

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

    private Set<Long> getSessionsToExclude(Person person) {
        Set<Long> excludeIds = new HashSet<>();
        if (person.getInstructor() != null) {
            List<LearningSession> ownSessions = learningSessionRepository.findByInstructorId(person.getInstructor().getId());
            excludeIds.addAll(ownSessions.stream().map(LearningSession::getId).collect(Collectors.toList()));
        }
        if (person.getLearner() != null && person.getLearner().getBookings() != null) {
            Set<Long> bookedSessions = person.getLearner().getBookings().stream()
                    .map(b -> b.getLearningSession().getId())
                    .collect(Collectors.toSet());
            excludeIds.addAll(bookedSessions);
        }
        return excludeIds;
    }

    private Double calculateMatchScore(LearningSession session, List<Long> userSkillIds, Person person) {
        double skillScore = 0.0;
        double categoryScore = 0.0;
        double languageScore = 0.0;

        if (session.getSkill() != null) {
            skillScore = userSkillIds.contains(session.getSkill().getId()) ? 1.0 : 0.0;
        }

        if (session.getSkill() != null && session.getSkill().getKnowledgeArea() != null) {
            long matchingSkillsInCategory = countSkillsInCategory(userSkillIds, session.getSkill().getKnowledgeArea().getId());
            categoryScore = matchingSkillsInCategory > 0 ? 0.7 : 0.0;
        }

        if (session.getLanguage() != null && person.getPreferredLanguage() != null) {
            if (session.getLanguage().equalsIgnoreCase(person.getPreferredLanguage())) {
                languageScore = 1.0;
            } else if ("en".equalsIgnoreCase(session.getLanguage()) && "es".equalsIgnoreCase(person.getPreferredLanguage())) {
                languageScore = 0.5;
            }
        }

        return (skillScore * 0.6) + (categoryScore * 0.3) + (languageScore * 0.1);
    }

    private long countSkillsInCategory(List<Long> userSkillIds, Long categoryId) {
        // placeholder: contar skills reales por categoría si se requiere consultas adicionales ***
        return userSkillIds.isEmpty() ? 0 : 1;
    }

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

        return new SessionSuggestionResponse(true, "Sugerencias por defecto", suggestions);
    }

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
            sessionSuggestionRepository.save(suggestion);
        }
        return suggestion;
    }

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
}