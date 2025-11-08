package com.project.skillswap.logic.entity.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Repository for dashboard operations using stored procedures
 * Handles all database interactions for dashboard data
 */
@Repository
public class DashboardRepository {

    //#region Dependencies
    @Autowired
    private JdbcTemplate jdbcTemplate;
    //#endregion

    //#region Public Methods
    /**
     * Gets total learning hours for a user
     *
     * @param personId Person ID
     * @param role User role (INSTRUCTOR or LEARNER)
     * @return Total minutes of learning
     */
    public Integer getLearningHours(Long personId, String role) {
        String sql = "CALL sp_get_learning_hours(?, ?)";
        return jdbcTemplate.queryForObject(sql, Integer.class, personId, role);
    }

    /**
     * Gets upcoming 5 sessions for a user
     *
     * @param personId Person ID
     * @param role User role (INSTRUCTOR or LEARNER)
     * @return List of upcoming sessions
     */
    public List<UpcomingSessionResponse> getUpcomingSessions(Long personId, String role) {
        String sql = "CALL sp_get_upcoming_sessions(?, ?)";
        return jdbcTemplate.query(sql, this::mapUpcomingSession, personId, role);
    }

    /**
     * Gets recent credentials for a learner
     *
     * @param personId Person ID
     * @return List of recent credentials
     */
    public List<CredentialResponse> getRecentCredentials(Long personId) {
        String sql = "CALL sp_get_recent_achievements(?, ?)";
        return jdbcTemplate.query(sql, this::mapCredential, personId, "LEARNER");
    }

    /**
     * Gets recent feedbacks for an instructor
     *
     * @param personId Person ID
     * @return List of recent feedbacks
     */
    public List<FeedbackResponse> getRecentFeedbacks(Long personId) {
        String sql = "CALL sp_get_recent_achievements(?, ?)";
        return jdbcTemplate.query(sql, this::mapFeedback, personId, "INSTRUCTOR");
    }
    //#endregion

    //#region Private Mappers
    /**
     * Maps ResultSet to UpcomingSessionResponse
     *
     * @param rs ResultSet
     * @param rowNum Row number
     * @return UpcomingSessionResponse object
     * @throws SQLException If mapping fails
     */
    private UpcomingSessionResponse mapUpcomingSession(ResultSet rs, int rowNum) throws SQLException {
        UpcomingSessionResponse session = new UpcomingSessionResponse();
        session.setId(rs.getLong("id"));
        session.setTitle(rs.getString("title"));
        session.setDescription(rs.getString("description"));
        session.setScheduledDatetime(rs.getTimestamp("scheduled_datetime"));
        session.setDurationMinutes(rs.getInt("duration_minutes"));
        session.setStatus(rs.getString("status"));
        session.setVideoCallLink(rs.getString("video_call_link"));
        session.setSkillName(rs.getString("skill_name"));
        return session;
    }

    /**
     * Maps ResultSet to CredentialResponse
     *
     * @param rs ResultSet
     * @param rowNum Row number
     * @return CredentialResponse object
     * @throws SQLException If mapping fails
     */
    private CredentialResponse mapCredential(ResultSet rs, int rowNum) throws SQLException {
        CredentialResponse credential = new CredentialResponse();
        credential.setId(rs.getLong("id"));
        credential.setSkillName(rs.getString("skill_name"));
        credential.setPercentageAchieved(rs.getDouble("percentage_achieved"));
        credential.setBadgeUrl(rs.getString("badge_url"));
        credential.setObtainedDate(rs.getTimestamp("obtained_date"));
        return credential;
    }

    /**
     * Maps ResultSet to FeedbackResponse
     *
     * @param rs ResultSet
     * @param rowNum Row number
     * @return FeedbackResponse object
     * @throws SQLException If mapping fails
     */
    private FeedbackResponse mapFeedback(ResultSet rs, int rowNum) throws SQLException {
        FeedbackResponse feedback = new FeedbackResponse();
        feedback.setId(rs.getLong("id"));
        feedback.setRating(rs.getInt("rating"));
        feedback.setComment(rs.getString("comment"));
        feedback.setCreationDate(rs.getTimestamp("creation_date"));
        feedback.setLearnerName(rs.getString("learner_name"));
        feedback.setSessionTitle(rs.getString("session_title"));
        return feedback;
    }
    //#endregion
}