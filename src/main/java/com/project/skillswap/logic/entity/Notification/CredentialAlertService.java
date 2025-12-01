
package com.project.skillswap.logic.entity.Notification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Notification.CredentialAlertDTO;
import com.project.skillswap.logic.entity.Credential.CredentialRepository;
import com.project.skillswap.logic.entity.Notification.Notification;
import com.project.skillswap.logic.entity.Notification.NotificationRepository;
import com.project.skillswap.logic.entity.Notification.NotificationType;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para gestionar alertas de credenciales cercanas a certificado
 */
@Service
public class CredentialAlertService {
    private static final Logger logger = LoggerFactory.getLogger(CredentialAlertService.class);

    private final CredentialRepository credentialRepository;
    private final NotificationRepository notificationRepository;
    private final PersonRepository personRepository;
    private final AlertEmailService alertEmailService;

    public CredentialAlertService(
            CredentialRepository credentialRepository,
            NotificationRepository notificationRepository,
            PersonRepository personRepository,
            AlertEmailService alertEmailService) {
        this.credentialRepository = credentialRepository;
        this.notificationRepository = notificationRepository;
        this.personRepository = personRepository;
        this.alertEmailService = alertEmailService;
    }

    /**
     * Procesa y envía alertas a learners que tienen 8 o 9 credenciales
     */
    /**
     * Procesa y envía alertas SIN verificar duplicados (para testing)
     */
    @Transactional
    public void processAndSendCredentialAlerts() {
        logger.info("Buscando learners cercanos a obtener certificado...");

        // Query SIN validación de notificaciones previas
        List<CredentialAlertDTO> alerts = credentialRepository.findLearnersCloseToAchievingCertificate();


        int successCount = 0;
        int errorCount = 0;

        for (CredentialAlertDTO alert : alerts) {
            try {
                // Enviar email
                alertEmailService.sendCredentialAlert(alert);

                // Guardar notificación en BD
                saveNotification(alert);

                successCount++;

            } catch (MessagingException e) {
                errorCount++;
                logger.info("Error al enviar alerta a " + alert.getLearnerName() + ": " + e.getMessage());
            } catch (Exception e) {
                errorCount++;
                logger.info("Error inesperado para " + alert.getLearnerName() + ": " + e.getMessage());
            }
        }

        logger.info("Resumen: " + successCount + " exitosas, " + errorCount + " fallidas");
    }

    /**
     * Guarda una notificación en la base de datos
     */
    private void saveNotification(CredentialAlertDTO alert) {
        // El DTO ya tiene el learnerId, pero necesitamos el personId
        // Vamos a buscarlo por email que es único
        Person person = personRepository.findByEmail(alert.getLearnerEmail())
                .orElse(null);

        if (person == null) {
            logger.info("No se encontró person con email: " + alert.getLearnerEmail());
            return;
        }

        Notification notification = new Notification();
        notification.setPerson(person);
        notification.setType(NotificationType.CREDENTIAL_ALERT);
        notification.setTitle("¡Cerca de tu certificado en " + alert.getSkillName() + "!");

        // Crear metadata con información de la alerta
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", "Tienes " + alert.getCredentialCount() + " de 10 credenciales en " + alert.getSkillName());
        metadata.put("skillId", alert.getSkillId());
        metadata.put("skillName", alert.getSkillName());
        metadata.put("credentialCount", alert.getCredentialCount());
        metadata.put("remainingCredentials", alert.getRemainingCredentials());
        metadata.put("eventType", "CREDENTIAL_ALERT");

        notification.setMetadata(metadata);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    /**
     * Obtiene preview de alertas que se enviarían (para testing)
     */
    public List<CredentialAlertDTO> getAlertsPreview() {
        return credentialRepository.findLearnersCloseToAchievingCertificate();
    }
}