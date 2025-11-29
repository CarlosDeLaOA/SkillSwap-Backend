package com.project.skillswap.logic.entity.Feedback;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeder que agrega columnas a la tabla feedback automáticamente
 * Se ejecuta solo UNA VEZ al iniciar la aplicación
 */
@Order(1)  // Ejecuta primero, antes que otros seeders
@Component
public class FeedbackAudioMigrationSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private final JdbcTemplate jdbcTemplate;

    public FeedbackAudioMigrationSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        System.out.println("========================================");
        System.out.println("[FeedbackAudioMigrationSeeder] Iniciando migración de tablas");
        System.out.println("========================================");

        try {
            // Verificar si las columnas ya existen
            if (! columnExists("feedback", "duration_seconds")) {
                System.out.println("[FeedbackAudioMigrationSeeder] Agregando columna duration_seconds...");
                jdbcTemplate.execute(
                        "ALTER TABLE feedback ADD COLUMN duration_seconds INT COMMENT 'Duración del audio en segundos'"
                );
                System.out.println("[FeedbackAudioMigrationSeeder] ✅ Columna duration_seconds agregada");
            } else {
                System.out.println("[FeedbackAudioMigrationSeeder] ℹ  Columna duration_seconds ya existe");
            }

            if (!columnExists("feedback", "processing_date")) {
                System.out.println("[FeedbackAudioMigrationSeeder] Agregando columna processing_date...");
                jdbcTemplate.execute(
                        "ALTER TABLE feedback ADD COLUMN processing_date DATETIME COMMENT 'Fecha de procesamiento/transcripción'"
                );
                System.out.println("[FeedbackAudioMigrationSeeder]  Columna processing_date agregada");
            } else {
                System.out.println("[FeedbackAudioMigrationSeeder] ℹ  Columna processing_date ya existe");
            }

            // Crear índices si no existen
            if (!indexExists("feedback", "idx_feedback_processing_date")) {
                System.out.println("[FeedbackAudioMigrationSeeder] Creando índice idx_feedback_processing_date...");
                jdbcTemplate.execute(
                        "CREATE INDEX idx_feedback_processing_date ON feedback(processing_date)"
                );
                System.out.println("[FeedbackAudioMigrationSeeder]  Índice idx_feedback_processing_date creado");
            } else {
                System.out.println("[FeedbackAudioMigrationSeeder] ℹ  Índice idx_feedback_processing_date ya existe");
            }

            if (!indexExists("feedback", "idx_feedback_duration")) {
                System.out.println("[FeedbackAudioMigrationSeeder] Creando índice idx_feedback_duration...");
                jdbcTemplate.execute(
                        "CREATE INDEX idx_feedback_duration ON feedback(duration_seconds)"
                );
                System.out.println("[FeedbackAudioMigrationSeeder]  Índice idx_feedback_duration creado");
            } else {
                System.out.println("[FeedbackAudioMigrationSeeder] ℹ  Índice idx_feedback_duration ya existe");
            }

            System.out.println("========================================");
            System.out.println("[FeedbackAudioMigrationSeeder]  Migración completada exitosamente");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("[FeedbackAudioMigrationSeeder]  Error en migración:");
            System.err.println("   " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
        }
    }

    /**
     * Verifica si una columna existe en una tabla
     */
    private boolean columnExists(String tableName, String columnName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?  AND TABLE_SCHEMA = DATABASE()",
                    new Object[]{tableName, columnName},
                    Integer.class
            );
            return count != null && count > 0;
        } catch (Exception e) {
            System.out.println("[FeedbackAudioMigrationSeeder] Error verificando columna: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un índice existe en una tabla
     */
    private boolean indexExists(String tableName, String indexName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_NAME = ? AND INDEX_NAME = ? AND TABLE_SCHEMA = DATABASE()",
                    new Object[]{tableName, indexName},
                    Integer.class
            );
            return count != null && count > 0;
        } catch (Exception e) {
            System.out.println("[FeedbackAudioMigrationSeeder] Error verificando índice: " + e.getMessage());
            return false;
        }
    }
}