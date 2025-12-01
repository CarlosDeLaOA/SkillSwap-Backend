package com.project.skillswap.logic.entity.dashboard;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service for dashboard business logic
 * Handles data processing and orchestration
 */
@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    //#region Dependencies
    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private SessionHistoryService sessionHistoryService;
    //#endregion

    //#region Public Methods
    /**
     * Gets total learning hours for a user
     *
     * @param personId Person ID
     * @param role User role
     * @return Learning hours response
     */
    @Transactional(readOnly = true)
    public LearningHoursResponse getLearningHours(Long personId, String role) {
        Integer totalMinutes = dashboardRepository.getLearningHours(personId, role);
        return new LearningHoursResponse(totalMinutes, role);
    }

    /**
     * Gets upcoming sessions for a user
     *
     * @param personId Person ID
     * @param role User role
     * @return List of upcoming sessions
     */
    @Transactional(readOnly = true)
    public List<UpcomingSessionResponse> getUpcomingSessions(Long personId, String role) {
        return dashboardRepository.getUpcomingSessions(personId, role);
    }

    /**
     * Gets recent credentials for a learner
     *
     * @param personId Person ID
     * @return List of recent credentials
     */
    @Transactional(readOnly = true)
    public List<CredentialResponse> getRecentCredentials(Long personId) {
        return dashboardRepository.getRecentCredentials(personId);
    }

    /**
     * Gets recent feedbacks for an instructor
     *
     * @param personId Person ID
     * @return List of recent feedbacks
     */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getRecentFeedbacks(Long personId) {
        return dashboardRepository.getRecentFeedbacks(personId);
    }

    @Transactional(readOnly = true)
    public List<SkillSessionStatsResponse> getSkillSessionStats(Long personId, String role) {
        return dashboardRepository.getSkillSessionStats(personId, role);
    }

    @Transactional(readOnly = true)
    public List<MonthlyAchievementResponse> getMonthlyAchievements(Long personId) {
        return dashboardRepository.getMonthlyAchievements(personId);
    }

    @Transactional(readOnly = true)
    public List<MonthlyAttendanceResponse> getMonthlyAttendance(Long personId) {
        return dashboardRepository.getMonthlyAttendance(personId);
    }

    /**
     * Gets account balance for a user
     *
     * @param personId Person ID
     * @param role User role (INSTRUCTOR or LEARNER)
     * @return Account balance in SkillCoins
     */
    @Transactional(readOnly = true)
    public Integer getAccountBalance(Long personId, String role) {
        return dashboardRepository.getAccountBalance(personId, role);
    }
    //#endregion

    //#region Session History Methods
    /**
     * Gets historical sessions for a learner with pagination
     *
     * @param learnerId ID of the learner
     * @param page Page number
     * @param size Page size
     * @return Map with paginated sessions and metadata
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLearnerHistoricalSessions(Long learnerId, int page, int size) {
        return sessionHistoryService.getLearnerHistoricalSessions(learnerId, page, size);
    }

    /**
     * Gets details of a specific session for a learner
     *
     * @param sessionId ID of the session
     * @param learnerId ID of the learner
     * @return Map with session details and participant count
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionDetails(Long sessionId, Long learnerId) {
        return sessionHistoryService.getSessionDetails(sessionId, learnerId);
    }
    //#endregion
}