package com.project.skillswap.config;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración para habilitar tareas programadas (Scheduled Tasks)
 * Necesario para que @Scheduled funcione en CertificationScheduler
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);

}