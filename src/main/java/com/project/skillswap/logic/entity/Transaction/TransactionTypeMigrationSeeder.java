package com.project.skillswap.logic.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migración para agregar el tipo WITHDRAWAL al enum de Transaction
 * Este seeder se ejecuta automáticamente al iniciar el backend
 */
@Order(1)  // Se ejecuta primero, antes de otros seeders
@Component
public class TransactionTypeMigrationSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(TransactionTypeMigrationSeeder.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean alreadySetup = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (alreadySetup) {
            return;
        }

        logger.info("[MIGRATION] Verificando columna 'type' de transaction...");

        try {
            // Intentar obtener el tipo de columna actual
            String currentType = jdbcTemplate.queryForObject(
                    "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "AND TABLE_NAME = 'transaction' " +
                            "AND COLUMN_NAME = 'type'",
                    String.class
            );

            logger.info("[MIGRATION] Tipo actual de columna: " + currentType);

            // Verificar si ya tiene WITHDRAWAL
            if (currentType != null && !currentType.contains("WITHDRAWAL")) {
                logger.info("[MIGRATION]  Falta 'WITHDRAWAL' en el enum, actualizando...");

                // Ejecutar ALTER TABLE para agregar WITHDRAWAL
                jdbcTemplate.execute(
                        "ALTER TABLE transaction " +
                                "MODIFY COLUMN type ENUM('PURCHASE', 'SESSION_PAYMENT', 'COLLECTION', 'REFUND', 'WITHDRAWAL') NOT NULL"
                );

                logger.info("[MIGRATION]  Columna 'type' actualizada con éxito");
            } else {
                logger.info("[MIGRATION]  Columna 'type' ya tiene WITHDRAWAL, no se requiere migración");
            }

        } catch (Exception e) {
            logger.info("[MIGRATION]  Error verificando/actualizando columna 'type': " + e.getMessage());
            e.printStackTrace();
        }

        alreadySetup = true;
    }
}