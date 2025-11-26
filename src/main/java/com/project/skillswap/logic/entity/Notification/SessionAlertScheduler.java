package com.project.skillswap.logic.entity.Notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler para alertas de sesiones próximas
 * Se ejecuta cada lunes a las 8:00 AM
 */
@Component
public class SessionAlertScheduler {

    private final SessionAlertService sessionAlertService;

    public SessionAlertScheduler(SessionAlertService sessionAlertService) {
        this.sessionAlertService = sessionAlertService;
    }

    /**
     * Ejecuta el envío de alertas de sesiones cada lunes a las 8:00 AM
     */
    @Scheduled(cron = "0 0 8 * * MON")
    // @Scheduled(cron = "0 * * * * *") // Para testing: cada minuto
    public void sendSessionAlerts() {
        System.out.println("⏰ Iniciando envío de alertas de sesiones semanales...");
        sessionAlertService.processAndSendSessionAlerts();
        System.out.println("✅ Proceso de alertas de sesiones completado");
    }
}