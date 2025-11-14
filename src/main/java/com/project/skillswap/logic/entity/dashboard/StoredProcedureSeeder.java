package com.project.skillswap.logic.entity.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeder que crea los stored procedures automáticamente
 * Order 1 para ejecutarse ANTES que todos los demás seeders
 */
@Order(1)
@Component
public class StoredProcedureSeeder implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static boolean ejecutado = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (ejecutado) {
            return;
        }

        System.out.println("\n🔧 ========================================");
        System.out.println("🔧 Verificando stored procedures del dashboard");
        System.out.println("🔧 ========================================");

        crearStoredProcedures();

        ejecutado = true;

        System.out.println("✅ Stored procedures listos");
        System.out.println("🔧 ========================================\n");
    }

    private void crearStoredProcedures() {
        crearProcedureMonthlyAchievements();
        crearProcedureMonthlyAttendance();
    }

    private void crearProcedureMonthlyAchievements() {
        try {
            // Verificar si ya existe
            String checkSql = "SELECT COUNT(*) FROM information_schema.ROUTINES " +
                    "WHERE ROUTINE_SCHEMA = DATABASE() " +
                    "AND ROUTINE_NAME = 'sp_get_monthly_achievements'";

            Integer existe = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (existe != null && existe > 0) {
                System.out.println("✓ sp_get_monthly_achievements ya existe");
                return;
            }

            System.out.println("📝 Creando sp_get_monthly_achievements...");

            // Crear el stored procedure (compatible con MySQL/MariaDB)
            String sql =
                    "CREATE PROCEDURE sp_get_monthly_achievements(IN p_person_id BIGINT) " +
                            "BEGIN " +
                            "    DECLARE v_learner_id BIGINT; " +
                            "    " +
                            "    SELECT id INTO v_learner_id FROM learner WHERE person_id = p_person_id; " +
                            "    " +
                            "    IF v_learner_id IS NULL THEN " +
                            "        SELECT " +
                            "            DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL n.n MONTH), '%b') as month, " +
                            "            0 as credentials, " +
                            "            0 as certificates " +
                            "        FROM (SELECT 0 as n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) n " +
                            "        ORDER BY n.n DESC; " +
                            "    ELSE " +
                            "        SELECT " +
                            "            DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL months.n MONTH), '%b') as month, " +
                            "            COALESCE(cred.total, 0) as credentials, " +
                            "            COALESCE(cert.total, 0) as certificates " +
                            "        FROM (SELECT 0 as n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) months " +
                            "        LEFT JOIN ( " +
                            "            SELECT " +
                            "                MONTH(obtained_date) as mes, " +
                            "                YEAR(obtained_date) as anio, " +
                            "                COUNT(*) as total " +
                            "            FROM credential " +
                            "            WHERE learner_id = v_learner_id " +
                            "            AND obtained_date >= DATE_SUB(CURDATE(), INTERVAL 4 MONTH) " +
                            "            GROUP BY YEAR(obtained_date), MONTH(obtained_date) " +
                            "        ) cred ON MONTH(DATE_SUB(CURDATE(), INTERVAL months.n MONTH)) = cred.mes " +
                            "               AND YEAR(DATE_SUB(CURDATE(), INTERVAL months.n MONTH)) = cred.anio " +
                            "        LEFT JOIN ( " +
                            "            SELECT " +
                            "                MONTH(issue_date) as mes, " +
                            "                YEAR(issue_date) as anio, " +
                            "                COUNT(*) as total " +
                            "            FROM certification " +
                            "            WHERE learner_id = v_learner_id " +
                            "            AND issue_date >= DATE_SUB(CURDATE(), INTERVAL 4 MONTH) " +
                            "            GROUP BY YEAR(issue_date), MONTH(issue_date) " +
                            "        ) cert ON MONTH(DATE_SUB(CURDATE(), INTERVAL months.n MONTH)) = cert.mes " +
                            "               AND YEAR(DATE_SUB(CURDATE(), INTERVAL months.n MONTH)) = cert.anio " +
                            "        ORDER BY months.n DESC; " +
                            "    END IF; " +
                            "END";

            jdbcTemplate.execute(sql);
            System.out.println("✅ sp_get_monthly_achievements creado exitosamente");

        } catch (Exception e) {
            System.err.println("❌ Error creando sp_get_monthly_achievements: " + e.getMessage());
            System.err.println("💡 Si el procedimiento ya existe, esto es normal");
        }
    }

    private void crearProcedureMonthlyAttendance() {
        try {
            // Verificar si ya existe
            String checkSql = "SELECT COUNT(*) FROM information_schema.ROUTINES " +
                    "WHERE ROUTINE_SCHEMA = DATABASE() " +
                    "AND ROUTINE_NAME = 'sp_get_monthly_attendance'";

            Integer existe = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (existe != null && existe > 0) {
                System.out.println("✓ sp_get_monthly_attendance ya existe");
                return;
            }

            System.out.println("📝 Creando sp_get_monthly_attendance...");

            // Crear el stored procedure (compatible con MySQL/MariaDB)
            String sql =
                    "CREATE PROCEDURE sp_get_monthly_attendance(IN p_person_id BIGINT) " +
                            "BEGIN " +
                            "    DECLARE v_instructor_id BIGINT; " +
                            "    " +
                            "    SELECT id INTO v_instructor_id FROM instructor WHERE person_id = p_person_id; " +
                            "    " +
                            "    IF v_instructor_id IS NULL THEN " +
                            "        SELECT " +
                            "            DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL n.n MONTH), '%b') as month, " +
                            "            0 as presentes, " +
                            "            0 as registrados " +
                            "        FROM (SELECT 0 as n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) n " +
                            "        ORDER BY n.n DESC; " +
                            "    ELSE " +
                            "        SELECT " +
                            "            DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL months.n MONTH), '%b') as month, " +
                            "            COALESCE(att.presentes, 0) as presentes, " +
                            "            COALESCE(att.registrados, 0) as registrados " +
                            "        FROM (SELECT 0 as n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) months " +
                            "        LEFT JOIN ( " +
                            "            SELECT " +
                            "                MONTH(ls.scheduled_datetime) as mes, " +
                            "                YEAR(ls.scheduled_datetime) as anio, " +
                            "                SUM(CASE WHEN b.attended = TRUE THEN 1 ELSE 0 END) as presentes, " +
                            "                COUNT(b.id) as registrados " +
                            "            FROM learning_session ls " +
                            "            INNER JOIN booking b ON b.learning_session_id = ls.id " +
                            "            WHERE ls.instructor_id = v_instructor_id " +
                            "            AND ls.status = 'FINISHED' " +
                            "            AND ls.scheduled_datetime >= DATE_SUB(CURDATE(), INTERVAL 4 MONTH) " +
                            "            GROUP BY YEAR(ls.scheduled_datetime), MONTH(ls.scheduled_datetime) " +
                            "        ) att ON MONTH(DATE_SUB(CURDATE(), INTERVAL months.n MONTH)) = att.mes " +
                            "              AND YEAR(DATE_SUB(CURDATE(), INTERVAL months.n MONTH)) = att.anio " +
                            "        ORDER BY months.n DESC; " +
                            "    END IF; " +
                            "END";

            jdbcTemplate.execute(sql);
            System.out.println("✅ sp_get_monthly_attendance creado exitosamente");

        } catch (Exception e) {
            System.err.println("❌ Error creando sp_get_monthly_attendance: " + e.getMessage());
            System.err.println("💡 Si el procedimiento ya existe, esto es normal");
        }
    }
}