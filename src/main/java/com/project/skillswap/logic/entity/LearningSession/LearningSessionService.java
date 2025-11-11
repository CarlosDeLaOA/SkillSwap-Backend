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

        return learningSessionRepository.findFilteredSessions(
                currentDate, fiveMinutesAgo, categoryId, language
        );
    }

    /**
     * Calcula la fecha de hace 5 minutos
     */
    private Date getFiveMinutesAgo(Date currentDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.MINUTE, -5);
        return calendar.getTime();
    }
}