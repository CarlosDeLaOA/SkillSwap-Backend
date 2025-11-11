package com.project.skillswap.logic.entity.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Service for dashboard business logic
 * Handles data processing and orchestration
 */
@Service
public class DashboardService {

    //#region Dependencies
    @Autowired
    private DashboardRepository dashboardRepository;
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

    /**
     * Gets account balance for a learner
     *
     * @param personId Person ID
     * @return Account balance response
     */
    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(Long personId) {
        Integer skillCoins = dashboardRepository.getAccountBalance(personId);
        return new AccountBalanceResponse(skillCoins != null ? skillCoins : 0);
    }
    /**
     * Gets monthly achievements for last 4 months
     *
     * @param personId Person ID
     * @return List of monthly achievements
     */
    @Transactional(readOnly = true)
    public List<MonthlyAchievementsResponse> getMonthlyAchievements(Long personId) {
        return dashboardRepository.getMonthlyAchievements(personId);
    }

    /**
     * Gets session statistics grouped by skill
     *
     * @param personId Person ID
     * @param role User role
     * @return List of skill session statistics
     */
    @Transactional(readOnly = true)
    public List<SkillSessionStatsResponse> getSkillSessionStats(Long personId, String role) {
        return dashboardRepository.getSkillSessionStats(personId, role);
    }
    //#endregion
}