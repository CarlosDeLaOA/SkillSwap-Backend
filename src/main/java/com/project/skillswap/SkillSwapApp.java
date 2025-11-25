package com.project.skillswap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal de SkillSwap.
 * Habilita scheduling para tareas automáticas como recordatorios de sesiones.
 */
@SpringBootApplication
@EnableScheduling
public class SkillSwapApp {

    public static void main(String[] args) {
        SpringApplication.run(SkillSwapApp.class, args);
    }

}