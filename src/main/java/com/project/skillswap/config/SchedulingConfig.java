package com.project.skillswap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración para habilitar tareas programadas (Scheduled Tasks)
 * Necesario para que @Scheduled funcione en CertificationScheduler
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    //
}