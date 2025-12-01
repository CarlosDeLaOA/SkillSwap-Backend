
package com.project.skillswap.logic.entity.Notification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionAlertScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SessionAlertScheduler.class);

    private final SessionAlertService sessionAlertService;

    public SessionAlertScheduler(SessionAlertService sessionAlertService) {
        this.sessionAlertService = sessionAlertService;
    }

    /**
     * Ejecuta el envío de alertas de sesiones
     */
    //@Scheduled(cron = "0 * * * * *") //QUE SE MANDE CADA MINUTO COMO PRUEBAS
    @Scheduled(cron = "0 0 8 * * MON")
    public void sendSessionAlerts() {
        logger.info("[TESTING] Iniciando envío de alertas de sesiones semanales...");
        sessionAlertService.processAndSendSessionAlerts();
        logger.info("Proceso de alertas de sesiones completado");
    }
}