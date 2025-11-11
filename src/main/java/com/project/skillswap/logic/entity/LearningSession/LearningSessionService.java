package com.project.skillswap.logic.entity.LearningSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class LearningSessionService {

    @Autowired
    private LearningSessionRepository learningSessionRepository;

    //<region desc="Public Methods">
    /**
     * Obtiene todas las sesiones disponibles (SCHEDULED o ACTIVE recientes)
     */
    @Transactional(readOnly = true)
    public List<LearningSession> getAvailableSessions() {
        Date currentDate = new Date();
        Date fiveMinutesAgo = getFiveMinutesAgo(currentDate);

        return learningSessionRepository.findAvailableSessions(currentDate, fiveMinutesAgo);
    }

    /**
     * Obtiene sesiones filtradas por categoría y/o idioma
     */
    @Transactional(readOnly = true)
    public List<LearningSession> getFilteredSessions(Long categoryId, String language) {
        Date currentDate = new Date();
        Date fiveMinutesAgo = getFiveMinutesAgo(currentDate);

        // Si ambos filtros están presentes
        if (categoryId != null && language != null && !language.isEmpty()) {
            return learningSessionRepository.findSessionsByCategoryAndLanguage(
                    currentDate, fiveMinutesAgo, categoryId, language
            );
        }

        // Si solo hay filtro de categoría
        if (categoryId != null) {
            return learningSessionRepository.findSessionsByCategory(
                    currentDate, fiveMinutesAgo, categoryId
            );
        }

        // Si solo hay filtro de idioma
        if (language != null && !language.isEmpty()) {
            return learningSessionRepository.findSessionsByLanguage(
                    currentDate, fiveMinutesAgo, language
            );
        }

        // Si no hay filtros, devolver todas las disponibles
        return getAvailableSessions();
    }
    //</region>

    //<region desc="Private Helper Methods">
    /**
     * Calcula la fecha de hace 5 minutos
     */
    private Date getFiveMinutesAgo(Date currentDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.MINUTE, -5);
        return calendar.getTime();
    }
    //</region>
}