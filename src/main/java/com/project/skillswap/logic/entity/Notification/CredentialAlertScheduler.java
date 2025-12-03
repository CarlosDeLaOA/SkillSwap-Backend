
package com.project.skillswap.logic.entity.Notification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler para alertas de credenciales cercanas a certificado
 * Se ejecuta diariamente a las 8:00 AM
 */
@Component
public class CredentialAlertScheduler {
    private static final Logger logger = LoggerFactory.getLogger(CredentialAlertScheduler.class);

    private final CredentialAlertService credentialAlertService;

    public CredentialAlertScheduler(CredentialAlertService credentialAlertService) {
        this.credentialAlertService = credentialAlertService;
    }

    /**
     * Ejecuta el envío de alertas de credenciales todos los días a las 8:00 AM
     */
    @Scheduled(cron = "0 0 8 * * *")
     //@Scheduled(cron = "0 * * * * *") //PARA PROBARLO QUE LLEGUE CADA MINUTO
    public void sendCredentialAlerts() {
        logger.info("Iniciando envío de alertas de credenciales...");
        credentialAlertService.processAndSendCredentialAlerts();
        logger.info("Proceso de alertas de credenciales completado");
    }
}