package com.project.skillswap.logic.entity.Feedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(FeedbackAudioMigrationSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    public FeedbackAudioMigrationSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info("========================================");
        logger.info("[FeedbackAudioMigrationSeeder] Iniciando migración de tablas");
        logger.info("========================================");

        try {
            // Verificar si las columnas ya existen
            if (! columnExists("feedback", "duration_seconds")) {
                logger.info("[FeedbackAudioMigrationSeeder] Agregando columna duration_seconds...");
                jdbcTemplate.execute(
                        "ALTER TABLE feedback ADD COLUMN duration_seconds INT COMMENT 'Duración del audio en segundos'"
                );
                logger.info("[FeedbackAudioMigrationSeeder] ✅ Columna duration_seconds agregada");
            } else {
                logger.info("[FeedbackAudioMigrationSeeder] ℹ  Columna duration_seconds ya existe");
            }

            if (!columnExists("feedback", "processing_date")) {
                logger.info("[FeedbackAudioMigrationSeeder] Agregando columna processing_date...");
                jdbcTemplate.execute(
                        "ALTER TABLE feedback ADD COLUMN processing_date DATETIME COMMENT 'Fecha de procesamiento/transcripción'"
                );
                logger.info("[FeedbackAudioMigrationSeeder]  Columna processing_date agregada");
            } else {
                logger.info("[FeedbackAudioMigrationSeeder] ℹ  Columna processing_date ya existe");
            }

            // Crear índices si no existen
            if (!indexExists("feedback", "idx_feedback_processing_date")) {
                logger.info("[FeedbackAudioMigrationSeeder] Creando índice idx_feedback_processing_date...");
                jdbcTemplate.execute(
                        "CREATE INDEX idx_feedback_processing_date ON feedback(processing_date)"
                );
                logger.info("[FeedbackAudioMigrationSeeder]  Índice idx_feedback_processing_date creado");
            } else {
                logger.info("[FeedbackAudioMigrationSeeder] ℹ  Índice idx_feedback_processing_date ya existe");
            }

            if (!indexExists("feedback", "idx_feedback_duration")) {
                logger.info("[FeedbackAudioMigrationSeeder] Creando índice idx_feedback_duration...");
                jdbcTemplate.execute(
                        "CREATE INDEX idx_feedback_duration ON feedback(duration_seconds)"
                );
                logger.info("[FeedbackAudioMigrationSeeder]  Índice idx_feedback_duration creado");
            } else {
                logger.info("[FeedbackAudioMigrationSeeder] ℹ  Índice idx_feedback_duration ya existe");
            }

            logger.info("========================================");
            logger.info("[FeedbackAudioMigrationSeeder]  Migración completada exitosamente");
            logger.info("========================================");

        } catch (Exception e) {
            logger.info("========================================");
            logger.info("[FeedbackAudioMigrationSeeder]  Error en migración:");
            logger.info("   " + e.getMessage());
            logger.info("========================================");
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
            logger.info("[FeedbackAudioMigrationSeeder] Error verificando columna: " + e.getMessage());
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
            logger.info("[FeedbackAudioMigrationSeeder] Error verificando índice: " + e.getMessage());
            return false;
        }
    }
}