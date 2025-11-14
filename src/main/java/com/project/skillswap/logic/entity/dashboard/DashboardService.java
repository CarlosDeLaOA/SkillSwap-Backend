package com.project.skillswap.logic.entity.dashboard;

import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import com.project.skillswap.logic.entity.Skill.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for dashboard business logic
 * Adaptado para trabajar con LearningSession y Booking
 */
@Service
public class DashboardService {

    //#region Dependencies
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private LearnerRepository learnerRepository;

    @Autowired
    private InstructorRepository instructorRepository;
    //#endregion

    //#region Public Methods

    /**
     * Gets learning hours for user based on role
     *
     * @param personId User's person ID
     * @param role User's role (INSTRUCTOR or LEARNER)
     * @return LearningHoursResponse with total hours
     */
    @Transactional(readOnly = true)
    public LearningHoursResponse getLearningHours(Long personId, String role) {
        // Implementation for learning hours
        // This is a placeholder - implement according to your needs
        return new LearningHoursResponse(0, role);
    }

    /**
     * Gets upcoming 5 sessions for user based on role
     *
     * @param personId User's person ID
     * @param role User's role (INSTRUCTOR or LEARNER)
     * @return List of UpcomingSessionResponse
     */
    @Transactional(readOnly = true)
    public List<UpcomingSessionResponse> getUpcomingSessions(Long personId, String role) {
        // Implementation for upcoming sessions
        // This is a placeholder - implement according to your needs
        return new ArrayList<>();
    }

    /**
     * Gets recent credentials for learner
     *
     * @param personId User's person ID
     * @return List of CredentialResponse
     */
    @Transactional(readOnly = true)
    public List<CredentialResponse> getRecentCredentials(Long personId) {
        // Implementation for credentials
        return new ArrayList<>();
    }

    /**
     * Gets recent feedbacks for instructor
     *
     * @param personId User's person ID
     * @return List of FeedbackResponse
     */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getRecentFeedbacks(Long personId) {
        // Implementation for feedbacks
        return new ArrayList<>();
    }

    /**
     * Gets account balance for learner
     *
     * @param personId User's person ID
     * @return AccountBalanceResponse
     */
    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(Long personId) {
        Optional<Learner> learnerOpt = learnerRepository.findByPersonId(personId);

        if (learnerOpt.isEmpty()) {
            return new AccountBalanceResponse(0);
        }

        Learner learner = learnerOpt.get();
        // Convertir BigDecimal a Integer para el balance de skillcoins
        Integer balance = learner.getSkillcoinsBalance() != null
                ? learner.getSkillcoinsBalance().intValue()
                : 0;
        return new AccountBalanceResponse(balance);
    }

    /**
     * Gets monthly achievements for last 4 months for learner
     *
     * @param personId User's person ID
     * @return List of MonthlyAchievementsResponse
     */
    @Transactional(readOnly = true)
    public List<MonthlyAchievementsResponse> getMonthlyAchievements(Long personId) {
        // Implementation for monthly achievements
        return new ArrayList<>();
    }

    /**
     * Gets monthly attendance statistics for instructor
     *
     * @param personId User's person ID
     * @return List of MonthlyAttendanceResponse
     */
    @Transactional(readOnly = true)
    public List<MonthlyAttendanceResponse> getMonthlyAttendance(Long personId) {
        // Implementation for monthly attendance
        return new ArrayList<>();
    }

    /**
     * Gets session statistics by skill for user
     * Groups bookings by skill and counts completed vs pending sessions
     *
     * @param personId User's person ID
     * @param role User's role (INSTRUCTOR or LEARNER)
     * @return List of SkillSessionStatsResponse
     */
    @Transactional(readOnly = true)
    public List<SkillSessionStatsResponse> getSkillSessionStats(Long personId, String role) {
        System.out.println("📊 Obteniendo estadísticas de skills para person_id: " + personId + ", rol: " + role);

        List<Booking> bookings;

        // Get bookings based on role
        if ("LEARNER".equals(role)) {
            Optional<Learner> learnerOpt = learnerRepository.findByPersonId(personId);
            if (learnerOpt.isEmpty()) {
                System.out.println("⚠️ No se encontró learner para person_id: " + personId);
                return createDefaultSkillStats();
            }
            bookings = bookingRepository.findByLearnerId(learnerOpt.get().getId().intValue());
            System.out.println("📚 Encontrados " + bookings.size() + " bookings para el learner");
        } else if ("INSTRUCTOR".equals(role)) {
            Optional<Instructor> instructorOpt = instructorRepository.findByPersonId(personId);
            if (instructorOpt.isEmpty()) {
                System.out.println("⚠️ No se encontró instructor para person_id: " + personId);
                return createDefaultSkillStats();
            }
            // Para instructor, obtenemos bookings de sus sesiones
            bookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getLearningSession() != null &&
                            b.getLearningSession().getInstructor() != null &&
                            b.getLearningSession().getInstructor().getId().equals(instructorOpt.get().getId()))
                    .collect(Collectors.toList());
            System.out.println("👨‍🏫 Encontrados " + bookings.size() + " bookings para el instructor");
        } else {
            System.out.println("⚠️ Rol no reconocido: " + role);
            return createDefaultSkillStats();
        }

        // If no bookings found, return default data
        if (bookings.isEmpty()) {
            System.out.println("⚠️ No hay bookings, retornando datos por defecto");
            return createDefaultSkillStats();
        }

        // Group bookings by skill
        Map<String, SkillStats> skillStatsMap = new HashMap<>();

        for (Booking booking : bookings) {
            LearningSession session = booking.getLearningSession();
            if (session == null) {
                continue;
            }

            Skill skill = session.getSkill();
            if (skill == null || skill.getName() == null || skill.getName().trim().isEmpty()) {
                continue;
            }

            String skillName = skill.getName();
            skillStatsMap.putIfAbsent(skillName, new SkillStats());

            SkillStats stats = skillStatsMap.get(skillName);

            // Determine if session is completed or pending based on SessionStatus
            if (isSessionCompleted(session)) {
                stats.completed++;
            } else {
                stats.pending++;
            }
        }

        // Convert map to response list
        List<SkillSessionStatsResponse> result = skillStatsMap.entrySet().stream()
                .map(entry -> new SkillSessionStatsResponse(
                        entry.getKey(),
                        entry.getValue().completed,
                        entry.getValue().pending
                ))
                .sorted(Comparator.comparing(SkillSessionStatsResponse::getSkillName))
                .collect(Collectors.toList());

        // If no valid skills found, return default data
        if (result.isEmpty()) {
            System.out.println("⚠️ No se encontraron skills válidas, retornando datos por defecto");
            return createDefaultSkillStats();
        }

        System.out.println("✅ Retornando " + result.size() + " skills con estadísticas");
        result.forEach(stat ->
                System.out.println("  - " + stat.getSkillName() + ": " +
                        stat.getCompleted() + " completadas, " +
                        stat.getPending() + " pendientes")
        );

        return result;
    }
    //#endregion

    //#region Private Methods
    /**
     * Determines if a session is completed based on SessionStatus
     *
     * @param session The session to check
     * @return true if completed, false otherwise
     */
    private boolean isSessionCompleted(LearningSession session) {
        if (session.getStatus() == null) {
            return false;
        }

        // Las sesiones FINISHED están completadas
        // Las sesiones SCHEDULED, ACTIVE, CANCELLED están pendientes o no completadas
        return session.getStatus() == SessionStatus.FINISHED;
    }

    /**
     * Creates default skill statistics for testing/empty state
     *
     * @return List with sample skill data
     */
    private List<SkillSessionStatsResponse> createDefaultSkillStats() {
        List<SkillSessionStatsResponse> defaultStats = new ArrayList<>();

        // Add some sample skills with realistic data
        defaultStats.add(new SkillSessionStatsResponse("Java", 7, 3));
        defaultStats.add(new SkillSessionStatsResponse("Python", 5, 5));
        defaultStats.add(new SkillSessionStatsResponse("JavaScript", 8, 2));
        defaultStats.add(new SkillSessionStatsResponse("Inglés", 10, 4));
        defaultStats.add(new SkillSessionStatsResponse("React", 3, 7));

        System.out.println("🎯 Usando datos por defecto para Skills Progress");
        return defaultStats;
    }

    /**
     * Helper class to accumulate skill statistics
     */
    private static class SkillStats {
        int completed = 0;
        int pending = 0;
    }
    //#endregion
}