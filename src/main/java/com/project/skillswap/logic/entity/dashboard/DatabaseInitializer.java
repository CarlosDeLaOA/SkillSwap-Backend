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
            createAccountBalanceProcedure();
            createMonthlyAchievementsProcedure();
            createSkillSessionStatsProcedure();
            createMonthlyAttendanceProcedure();
            System.out.println("All stored procedures created successfully");
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
                "DROP PROCEDURE IF EXISTS sp_get_recent_achievements",
                "DROP PROCEDURE IF EXISTS sp_get_account_balance",
                "DROP PROCEDURE IF EXISTS sp_get_monthly_achievements",
                "DROP PROCEDURE IF EXISTS sp_get_skill_session_stats",
                "DROP PROCEDURE IF EXISTS sp_get_monthly_attendance"
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
        System.out.println("✓ Stored procedure 'sp_get_recent_achievements' created");
    }

    private void createAccountBalanceProcedure() {
        String sql =
                "CREATE PROCEDURE sp_get_account_balance(IN p_person_id BIGINT) " +
                        "BEGIN " +
                        "    SELECT COALESCE(l.skillcoins_balance, 0) AS skill_coins " +
                        "    FROM learner l " +
                        "    WHERE l.person_id = p_person_id; " +
                        "END";
        jdbcTemplate.execute(sql);
        System.out.println("✓ Stored procedure 'sp_get_account_balance' created");
    }

    private void createMonthlyAchievementsProcedure() {
        String sql =
                "CREATE PROCEDURE sp_get_monthly_achievements(IN p_person_id BIGINT) " +
                        "BEGIN " +
                        "    SELECT " +
                        "        DATE_FORMAT(c.obtained_date, '%b') AS month, " +
                        "        COUNT(CASE WHEN c.percentage_achieved >= 100 THEN 1 END) AS certificates, " +
                        "        COUNT(CASE WHEN c.percentage_achieved < 100 THEN 1 END) AS credentials " +
                        "    FROM credential c " +
                        "    INNER JOIN learner l ON c.learner_id = l.id " +
                        "    WHERE l.person_id = p_person_id " +
                        "    AND c.obtained_date >= DATE_SUB(CURDATE(), INTERVAL 4 MONTH) " +
                        "    GROUP BY YEAR(c.obtained_date), MONTH(c.obtained_date) " +
                        "    ORDER BY YEAR(c.obtained_date) ASC, MONTH(c.obtained_date) ASC " +
                        "    LIMIT 4; " +
                        "END";
        jdbcTemplate.execute(sql);
        System.out.println("✓ Stored procedure 'sp_get_monthly_achievements' created");
    }

    /**
     * CORRECCIÓN: Este stored procedure ahora maneja correctamente los casos
     * donde no hay datos y retorna siempre al menos una fila con valores en 0
     */
    private void createSkillSessionStatsProcedure() {
        String sql =
                "CREATE PROCEDURE sp_get_skill_session_stats(IN p_person_id BIGINT, IN p_role VARCHAR(20)) " +
                        "BEGIN " +
                        "    IF p_role = 'INSTRUCTOR' THEN " +
                        "        SELECT " +
                        "            COALESCE(s.name, 'Sin habilidades') AS skill_name, " +
                        "            COALESCE(COUNT(CASE WHEN ls.status = 'COMPLETED' THEN 1 END), 0) AS completed, " +
                        "            COALESCE(COUNT(CASE WHEN ls.status IN ('SCHEDULED', 'CONFIRMED') AND ls.scheduled_datetime > NOW() THEN 1 END), 0) AS pending " +
                        "        FROM instructor i " +
                        "        LEFT JOIN learning_session ls ON ls.instructor_id = i.id " +
                        "        LEFT JOIN skill s ON ls.skill_id = s.id " +
                        "        WHERE i.person_id = p_person_id " +
                        "        GROUP BY s.id, s.name " +
                        "        UNION ALL " +
                        "        SELECT 'Sin habilidades' AS skill_name, 0 AS completed, 0 AS pending " +
                        "        WHERE NOT EXISTS ( " +
                        "            SELECT 1 FROM instructor i2 " +
                        "            INNER JOIN learning_session ls2 ON ls2.instructor_id = i2.id " +
                        "            WHERE i2.person_id = p_person_id " +
                        "        ) " +
                        "        LIMIT 1; " +
                        "    ELSE " +
                        "        SELECT " +
                        "            COALESCE(s.name, 'Sin habilidades') AS skill_name, " +
                        "            COALESCE(COUNT(CASE WHEN ls.status = 'COMPLETED' AND b.attended = TRUE THEN 1 END), 0) AS completed, " +
                        "            COALESCE(COUNT(CASE WHEN ls.status IN ('SCHEDULED', 'CONFIRMED') AND ls.scheduled_datetime > NOW() AND b.status = 'CONFIRMED' THEN 1 END), 0) AS pending " +
                        "        FROM learner l " +
                        "        LEFT JOIN booking b ON b.learner_id = l.id " +
                        "        LEFT JOIN learning_session ls ON b.learning_session_id = ls.id " +
                        "        LEFT JOIN skill s ON ls.skill_id = s.id " +
                        "        WHERE l.person_id = p_person_id " +
                        "        GROUP BY s.id, s.name " +
                        "        UNION ALL " +
                        "        SELECT 'Sin habilidades' AS skill_name, 0 AS completed, 0 AS pending " +
                        "        WHERE NOT EXISTS ( " +
                        "            SELECT 1 FROM learner l2 " +
                        "            INNER JOIN booking b2 ON b2.learner_id = l2.id " +
                        "            WHERE l2.person_id = p_person_id " +
                        "        ) " +
                        "        LIMIT 1; " +
                        "    END IF; " +
                        "END";
        jdbcTemplate.execute(sql);
        System.out.println("✓ Stored procedure 'sp_get_skill_session_stats' created");
    }

    private void createMonthlyAttendanceProcedure() {
        String sql =
                "CREATE PROCEDURE sp_get_monthly_attendance(IN p_person_id BIGINT) " +
                        "BEGIN " +
                        "    SELECT " +
                        "        DATE_FORMAT(ar.start_datetime, '%b') AS month, " +
                        "        COALESCE(SUM(ar.total_participants), 0) AS presentes, " +
                        "        COALESCE(COUNT(b.id), 0) AS registrados " +
                        "    FROM learning_session ls " +
                        "    INNER JOIN instructor i ON ls.instructor_id = i.id " +
                        "    LEFT JOIN attendance_record ar ON ar.learning_session_id = ls.id " +
                        "    LEFT JOIN booking b ON b.learning_session_id = ls.id " +
                        "    WHERE i.person_id = p_person_id " +
                        "    AND ar.start_datetime >= DATE_SUB(CURDATE(), INTERVAL 4 MONTH) " +
                        "    GROUP BY YEAR(ar.start_datetime), MONTH(ar.start_datetime) " +
                        "    ORDER BY YEAR(ar.start_datetime) ASC, MONTH(ar.start_datetime) ASC " +
                        "    LIMIT 4; " +
                        "END";
        jdbcTemplate.execute(sql);
        System.out.println("✓ Stored procedure 'sp_get_monthly_attendance' created");
    }
}