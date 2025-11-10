package com.project.skillswap.logic.entity.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes database stored procedures on application startup
 * Creates all necessary stored procedures for dashboard operations
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    //#region Dependencies
    @Autowired
    private JdbcTemplate jdbcTemplate;
    //#endregion

    //#region CommandLineRunner Implementation
    /**
     * Executes on application startup to create stored procedures
     *
     * @param args Command line arguments
     * @throws Exception If stored procedure creation fails
     */
    @Override
    public void run(String... args) throws Exception {
        createStoredProcedures();
    }
    //#endregion

    //#region Private Methods
    /**
     * Creates all stored procedures for dashboard operations
     * Drops existing procedures before creating new ones
     */
    private void createStoredProcedures() {
        try {
            dropExistingProcedures();
            createLearningHoursProcedure();
            createUpcomingSessionsProcedure();
            createRecentAchievementsProcedure();
        } catch (Exception e) {
            System.err.println("Error creating stored procedures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Drops existing stored procedures if they exist
     */
    private void dropExistingProcedures() {
        String[] dropStatements = {
                "DROP PROCEDURE IF EXISTS sp_get_learning_hours",
                "DROP PROCEDURE IF EXISTS sp_get_upcoming_sessions",
                "DROP PROCEDURE IF EXISTS sp_get_recent_achievements"
        };

        for (String sql : dropStatements) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                System.err.println("Warning dropping procedure: " + e.getMessage());
            }
        }
    }

    /**
     * Creates stored procedure to calculate total learning hours
     * Calculates based on user role (INSTRUCTOR or LEARNER)
     */
    private void createLearningHoursProcedure() {
        String sql =
                "CREATE PROCEDURE sp_get_learning_hours(IN p_person_id BIGINT, IN p_role VARCHAR(20)) " +
                        "BEGIN " +
                        "    IF p_role = 'INSTRUCTOR' THEN " +
                        "        SELECT COALESCE(SUM(ls.duration_minutes), 0) AS total_minutes " +
                        "        FROM learning_session ls " +
                        "        INNER JOIN instructor i ON ls.instructor_id = i.id " +
                        "        WHERE i.person_id = p_person_id " +
                        "        AND ls.status = 'FINISHED'; " +
                        "    ELSE " +
                        "        SELECT COALESCE(SUM(ls.duration_minutes), 0) AS total_minutes " +
                        "        FROM learning_session ls " +
                        "        INNER JOIN booking b ON b.learning_session_id = ls.id " +
                        "        INNER JOIN learner l ON b.learner_id = l.id " +
                        "        WHERE l.person_id = p_person_id " +
                        "        AND b.attended = TRUE " +
                        "        AND ls.status = 'FINISHED'; " +
                        "    END IF; " +
                        "END";

        jdbcTemplate.execute(sql);
    }

    /**
     * Creates stored procedure to get upcoming 5 sessions
     * Returns different sessions based on user role
     */
    private void createUpcomingSessionsProcedure() {
        String sql =
                "CREATE PROCEDURE sp_get_upcoming_sessions(IN p_person_id BIGINT, IN p_role VARCHAR(20)) " +
                        "BEGIN " +
                        "    IF p_role = 'INSTRUCTOR' THEN " +
                        "        SELECT " +
                        "            ls.id, " +
                        "            ls.title, " +
                        "            ls.description, " +
                        "            ls.scheduled_datetime, " +
                        "            ls.duration_minutes, " +
                        "            ls.status, " +
                        "            ls.video_call_link, " +
                        "            s.name AS skill_name " +
                        "        FROM learning_session ls " +
                        "        INNER JOIN instructor i ON ls.instructor_id = i.id " +
                        "        INNER JOIN skill s ON ls.skill_id = s.id " +
                        "        WHERE i.person_id = p_person_id " +
                        "        AND ls.scheduled_datetime > NOW() " +
                        "        AND ls.status IN ('SCHEDULED', 'CONFIRMED') " +
                        "        ORDER BY ls.scheduled_datetime ASC " +
                        "        LIMIT 5; " +
                        "    ELSE " +
                        "        SELECT " +
                        "            ls.id, " +
                        "            ls.title, " +
                        "            ls.description, " +
                        "            ls.scheduled_datetime, " +
                        "            ls.duration_minutes, " +
                        "            ls.status, " +
                        "            ls.video_call_link, " +
                        "            s.name AS skill_name " +
                        "        FROM learning_session ls " +
                        "        INNER JOIN booking b ON b.learning_session_id = ls.id " +
                        "        INNER JOIN learner l ON b.learner_id = l.id " +
                        "        INNER JOIN skill s ON ls.skill_id = s.id " +
                        "        WHERE l.person_id = p_person_id " +
                        "        AND ls.scheduled_datetime > NOW() " +
                        "        AND b.status = 'CONFIRMED' " +
                        "        AND ls.status IN ('SCHEDULED', 'CONFIRMED') " +
                        "        ORDER BY ls.scheduled_datetime ASC " +
                        "        LIMIT 5; " +
                        "    END IF; " +
                        "END";

        jdbcTemplate.execute(sql);
    }

    /**
     * Creates stored procedure to get recent achievements
     * Returns credentials for LEARNER, feedbacks for INSTRUCTOR
     */
    private void createRecentAchievementsProcedure() {
        String sql =
                "CREATE PROCEDURE sp_get_recent_achievements(IN p_person_id BIGINT, IN p_role VARCHAR(20)) " +
                        "BEGIN " +
                        "    IF p_role = 'LEARNER' THEN " +
                        "        SELECT " +
                        "            c.id, " +
                        "            s.name AS skill_name, " +
                        "            c.percentage_achieved, " +
                        "            c.badge_url, " +
                        "            c.obtained_date " +
                        "        FROM credential c " +
                        "        INNER JOIN learner l ON c.learner_id = l.id " +
                        "        INNER JOIN skill s ON c.skill_id = s.id " +
                        "        WHERE l.person_id = p_person_id " +
                        "        ORDER BY c.obtained_date DESC " +
                        "        LIMIT 10; " +
                        "    ELSE " +
                        "        SELECT " +
                        "            f.id, " +
                        "            f.rating, " +
                        "            f.comment, " +
                        "            f.creation_date, " +
                        "            p.full_name AS learner_name, " +
                        "            ls.title AS session_title " +
                        "        FROM feedback f " +
                        "        INNER JOIN learning_session ls ON f.learning_session_id = ls.id " +
                        "        INNER JOIN instructor i ON ls.instructor_id = i.id " +
                        "        INNER JOIN learner l ON f.learner_id = l.id " +
                        "        INNER JOIN person p ON l.person_id = p.id " +
                        "        WHERE i.person_id = p_person_id " +
                        "        ORDER BY f.creation_date DESC " +
                        "        LIMIT 10; " +
                        "    END IF; " +
                        "END";

        jdbcTemplate.execute(sql);
    }
    //#endregion
}