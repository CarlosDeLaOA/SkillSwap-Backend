package com.project.skillswap.logic.entity.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * For LEARNERS, enriches data with booking information
     *
     * @param personId Person ID
     * @param role User role (INSTRUCTOR or LEARNER)
     * @return List of upcoming sessions
     */
    public List<UpcomingSessionResponse> getUpcomingSessions(Long personId, String role) {
        // Obtener sesiones del stored procedure (sin modificar)
        String sql = "CALL sp_get_upcoming_sessions(?, ?)";
        List<UpcomingSessionResponse> sessions = jdbcTemplate.query(sql, this::mapUpcomingSession, personId, role);

        // ✅ Si es LEARNER, enriquecer con booking info
        if ("LEARNER".equals(role) && !sessions.isEmpty()) {
            enrichWithBookingInfo(sessions, personId);
        }

        return sessions;
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

    /**
     * Gets skill session statistics for a user
     * Returns mock data based on user's REAL skills from user_skill table
     *
     * @param personId Person ID
     * @param role User role (INSTRUCTOR or LEARNER)
     * @return List of skill session statistics
     */
    public List<SkillSessionStatsResponse> getSkillSessionStats(Long personId, String role) {
        // Query para obtener las skills REALES del usuario
        String sql = """
        SELECT s.name AS skill_name
        FROM user_skill us
        INNER JOIN skill s ON us.skill_id = s.id
        WHERE us.person_id = ? AND us.active = true
        ORDER BY s.name
    """;

        try {
            List<SkillSessionStatsResponse> stats = jdbcTemplate.query(sql, (rs, rowNum) -> {
                String skillName = rs.getString("skill_name");

                // Generar datos mock aleatorios para cada skill real
                int completed = (int) (Math.random() * 8) + 2;  // Entre 2 y 10
                int pending = (int) (Math.random() * 6) + 1;     // Entre 1 y 7

                return new SkillSessionStatsResponse(skillName, completed, pending);
            }, personId);

            // Si el usuario no tiene skills, retornar lista vacía
            if (stats.isEmpty()) {
                System.out.println("⚠️ Usuario " + personId + " no tiene skills registradas");
            } else {
                System.out.println("✅ Encontradas " + stats.size() + " skills para usuario " + personId);
            }

            return stats;

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo skills del usuario: " + e.getMessage());
            e.printStackTrace();

            // Fallback: retornar lista vacía en caso de error
            return new ArrayList<>();
        }
    }

    // ✅ NUEVO - Monthly Achievements con Mock Data
    public List<MonthlyAchievementResponse> getMonthlyAchievements(Long personId) {
        List<MonthlyAchievementResponse> mockData = new ArrayList<>();
        mockData.add(new MonthlyAchievementResponse("Ago", 3, 1));
        mockData.add(new MonthlyAchievementResponse("Sep", 5, 2));
        mockData.add(new MonthlyAchievementResponse("Oct", 4, 3));
        mockData.add(new MonthlyAchievementResponse("Nov", 6, 2));
        return mockData;
    }

    // ✅ NUEVO - Monthly Attendance con Mock Data
    public List<MonthlyAttendanceResponse> getMonthlyAttendance(Long personId) {
        List<MonthlyAttendanceResponse> mockData = new ArrayList<>();
        mockData.add(new MonthlyAttendanceResponse("Ago", 15, 20));
        mockData.add(new MonthlyAttendanceResponse("Sep", 18, 22));
        mockData.add(new MonthlyAttendanceResponse("Oct", 20, 25));
        mockData.add(new MonthlyAttendanceResponse("Nov", 22, 28));
        return mockData;
    }
    //#endregion

    //#region Private Methods
    /**
     * Enriquece las sesiones con información de booking (solo para LEARNERS)
     * Obtiene el booking_id y booking_type de cada sesión
     *
     * @param sessions Lista de sesiones a enriquecer
     * @param personId ID de la persona (learner)
     */
    private void enrichWithBookingInfo(List<UpcomingSessionResponse> sessions, Long personId) {
        // Obtener IDs de las sesiones
        List<Long> sessionIds = sessions.stream()
                .map(UpcomingSessionResponse::getId)
                .collect(Collectors.toList());

        if (sessionIds.isEmpty()) {
            return;
        }

        // Crear placeholders para el IN clause (?, ?, ?)
        String placeholders = String.join(",", Collections.nCopies(sessionIds.size(), "?"));

        // Query para obtener booking info
        String sql = String.format("""
            SELECT 
                b.learning_session_id,
                b.id AS booking_id,
                b.type AS booking_type
            FROM booking b
            INNER JOIN learner l ON b.learner_id = l.id
            WHERE l.person_id = ?
            AND b.learning_session_id IN (%s)
            AND b.status = 'CONFIRMED'
        """, placeholders);

        // Preparar parámetros: [personId, sessionId1, sessionId2, ...]
        List<Object> params = new ArrayList<>();
        params.add(personId);
        params.addAll(sessionIds);

        try {
            // Ejecutar query y mapear a un Map para acceso rápido
            Map<Long, BookingInfo> bookingMap = jdbcTemplate.query(
                    sql,
                    params.toArray(),
                    (rs, rowNum) -> new BookingInfo(
                            rs.getLong("learning_session_id"),
                            rs.getLong("booking_id"),
                            rs.getString("booking_type")
                    )
            ).stream().collect(Collectors.toMap(
                    BookingInfo::getSessionId,
                    bi -> bi
            ));

            // Enriquecer las sesiones con la info de booking
            sessions.forEach(session -> {
                BookingInfo bookingInfo = bookingMap.get(session.getId());
                if (bookingInfo != null) {
                    session.setBookingId(bookingInfo.getBookingId());
                    session.setBookingType(bookingInfo.getBookingType());
                }
            });

            System.out.println("✅ Enriquecidas " + bookingMap.size() + " sesiones con booking info");

        } catch (Exception e) {
            System.err.println("❌ Error enriqueciendo sesiones con booking info: " + e.getMessage());
            e.printStackTrace();
            // No lanzar excepción - las sesiones simplemente no tendrán booking info
        }
    }

    /**
     * Clase auxiliar para mapear información de booking
     */
    private static class BookingInfo {
        private final Long sessionId;
        private final Long bookingId;
        private final String bookingType;

        public BookingInfo(Long sessionId, Long bookingId, String bookingType) {
            this.sessionId = sessionId;
            this.bookingId = bookingId;
            this.bookingType = bookingType;
        }

        public Long getSessionId() { return sessionId; }
        public Long getBookingId() { return bookingId; }
        public String getBookingType() { return bookingType; }
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

        // Nota: bookingId y bookingType se setearán después en enrichWithBookingInfo()
        // No los intentamos leer del ResultSet porque el SP original no los devuelve

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