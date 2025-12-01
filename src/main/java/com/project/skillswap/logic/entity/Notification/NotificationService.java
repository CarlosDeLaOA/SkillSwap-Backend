
package com.project.skillswap.logic.entity.Notification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocument;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMember;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationEmailService notificationEmailService;

    /**
     * Crea y envía notificación cuando se agrega un documento
     */
    @Transactional
    public void notifyDocumentAdded(CommunityDocument document, List<Person> recipients) {

        logger.info("=".repeat(80));
        logger.info(" [NOTIFICATION] Iniciando notifyDocumentAdded");
        logger.info("=".repeat(80));

        logger.info(" [DEBUG] Documento ID: " + document.getId());
        logger.info(" [DEBUG] Documento Título: " + document.getTitle());
        logger.info(" [DEBUG] Cantidad de recipients recibidos: " + recipients.size());

        logger.info("\n [DEBUG] Lista COMPLETA de recipients:");
        for (int i = 0; i < recipients.size(); i++) {
            Person p = recipients.get(i);
            logger.info("  [" + (i+1) + "] Person ID: " + p.getId() +
                    " | Nombre: " + p.getFullName() +
                    " | Email: " + p.getEmail());
        }

        LearningCommunity community = document.getLearningCommunity();
        Person uploader = document.getUploadedBy().getPerson();

        logger.info("\n [DEBUG] Uploader:");
        logger.info("  Person ID: " + uploader.getId() +
                " | Nombre: " + uploader.getFullName() +
                " | Email: " + uploader.getEmail());

        logger.info("\n [DEBUG] Comunidad:");
        logger.info("  ID: " + community.getId() + " | Nombre: " + community.getName());

        String title = "📄 Nuevo documento en " + community.getName();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", uploader.getFullName() + " agregó el documento '" + document.getTitle() + "'");
        metadata.put("eventType", "DOCUMENT_ADDED");
        metadata.put("communityId", community.getId());
        metadata.put("communityName", community.getName());
        metadata.put("documentId", document.getId());
        metadata.put("documentName", document.getTitle());
        metadata.put("uploaderName", uploader.getFullName());
        metadata.put("uploaderId", uploader.getId());

        logger.info("\n [DEBUG] Iniciando envío de notificaciones...");

        int notificationCount = 0;
        int emailCount = 0;
        int skippedCount = 0;

        for (Person recipient : recipients) {
            logger.info("\n [LOOP] Procesando recipient: " + recipient.getFullName() + " (ID: " + recipient.getId() + ")");

            // No notificar al que subió el documento
            if (recipient.getId().equals(uploader.getId())) {
                logger.info(" [SKIP] Es el uploader, saltando...");
                skippedCount++;
                continue;
            }

            logger.info(" [PROCESS] Creando notificación IN-APP...");

            // Crear notificación IN-APP
            Notification notification = new Notification();
            notification.setPerson(recipient);
            notification.setTitle(title);
            notification.setMetadata(metadata);
            notification.setType(NotificationType.IN_APP);
            notification.setRead(false);

            Notification saved = notificationRepository.save(notification);
            notificationCount++;
            logger.info(" [SAVED] Notificación guardada con ID: " + saved.getId());

            // Enviar email
            try {
                logger.info("[EMAIL] Intentando enviar email a: " + recipient.getEmail());
                notificationEmailService.sendDocumentAddedEmail(recipient, community, document, uploader);
                emailCount++;
                logger.info("[EMAIL] Email enviado exitosamente a: " + recipient.getEmail());
            } catch (Exception e) {
                logger.info("[EMAIL] Error al enviar email: " + e.getMessage());
                e.printStackTrace();
            }
        }

        logger.info("\n" + "=".repeat(80));
        logger.info("[RESUMEN]");
        logger.info("  Recipients recibidos: " + recipients.size());
        logger.info("  Recipients procesados: " + (notificationCount));
        logger.info("  Recipients saltados (uploader): " + skippedCount);
        logger.info("  Notificaciones creadas: " + notificationCount);
        logger.info("  Emails enviados: " + emailCount);
        logger.info("=".repeat(80));
    }

    /**
     * Crea y envía notificación cuando alguien se une a la comunidad
     */
    @Transactional
    public void notifyMemberJoined(LearningCommunity community, Person newMember, List<Person> recipients) {

        logger.info("[NOTIFICATION] Notificando nuevo miembro a " + recipients.size() + " personas");

        String title = "👋 " + newMember.getFullName() + " se unió a " + community.getName();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", newMember.getFullName() + " se unió a la comunidad");
        metadata.put("eventType", "MEMBER_JOINED");
        metadata.put("communityId", community.getId());
        metadata.put("communityName", community.getName());
        metadata.put("newMemberName", newMember.getFullName());
        metadata.put("newMemberId", newMember.getId());

        for (Person recipient : recipients) {
            // No notificar al que se acaba de unir
            if (recipient.getId().equals(newMember.getId())) {
                continue;
            }

            Notification notification = new Notification();
            notification.setPerson(recipient);
            notification.setTitle(title);
            notification.setMetadata(metadata);
            notification.setType(NotificationType.IN_APP);
            notification.setRead(false);

            notificationRepository.save(notification);

            try {
                notificationEmailService.sendMemberJoinedEmail(recipient, community, newMember);
                logger.info("[EMAIL] Email enviado a: " + recipient.getEmail());
            } catch (Exception e) {
                logger.info("[EMAIL] Error al enviar email: " + e.getMessage());
            }
        }
    }

    /**
     * Notifica SOLO al creador cuando alguien sale de la comunidad
     */
    @Transactional
    public void notifyMemberLeft(LearningCommunity community, Person leftMember, Person creator) {

        logger.info("[NOTIFICATION] Notificando al creador que un miembro salió");

        String title = "⚠️ " + leftMember.getFullName() + " salió de " + community.getName();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", leftMember.getFullName() + " salió de la comunidad");
        metadata.put("eventType", "MEMBER_LEFT");
        metadata.put("communityId", community.getId());
        metadata.put("communityName", community.getName());
        metadata.put("leftMemberName", leftMember.getFullName());
        metadata.put("leftMemberId", leftMember.getId());

        Notification notification = new Notification();
        notification.setPerson(creator);
        notification.setTitle(title);
        notification.setMetadata(metadata);
        notification.setType(NotificationType.IN_APP);
        notification.setRead(false);

        notificationRepository.save(notification);

        try {
            notificationEmailService.sendMemberLeftEmail(creator, community, leftMember);
            logger.info("[EMAIL] Email enviado al creador: " + creator.getEmail());
        } catch (Exception e) {
            logger.info("[EMAIL] Error al enviar email: " + e.getMessage());
        }
    }

    /**
     * Notifica cuando un miembro obtiene una certificación
     */
    @Transactional
    public void notifyAchievementEarned(LearningCommunity community, Person achiever,
                                        String achievementName, List<Person> recipients) {

        logger.info(" [NOTIFICATION] Notificando nuevo logro a " + recipients.size() + " miembros");

        String title = "🏆 " + achiever.getFullName() + " obtuvo certificación en " + achievementName;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", achiever.getFullName() + " obtuvo la certificación '" + achievementName + "'");
        metadata.put("eventType", "ACHIEVEMENT_EARNED");
        metadata.put("communityId", community.getId());
        metadata.put("communityName", community.getName());
        metadata.put("achieverName", achiever.getFullName());
        metadata.put("achieverId", achiever.getId());
        metadata.put("achievementName", achievementName);

        for (Person recipient : recipients) {
            // No notificar al que obtuvo el logro
            if (recipient.getId().equals(achiever.getId())) {
                continue;
            }

            Notification notification = new Notification();
            notification.setPerson(recipient);
            notification.setTitle(title);
            notification.setMetadata(metadata);
            notification.setType(NotificationType.IN_APP);
            notification.setRead(false);

            notificationRepository.save(notification);

            try {
                notificationEmailService.sendAchievementEarnedEmail(recipient, community, achiever, achievementName);
                logger.info("[EMAIL] Email enviado a: " + recipient.getEmail());
            } catch (Exception e) {
                logger.info("[EMAIL] Error al enviar email: " + e.getMessage());
            }
        }
    }

    /**
     * Obtiene todas las notificaciones de un usuario
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByPerson(Person person) {
        return notificationRepository.findByPersonOrderBySendDateDesc(person);
    }

    /**
     * Obtiene las notificaciones no leídas de un usuario
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Person person) {
        return notificationRepository.findUnreadByPerson(person);
    }

    /**
     * Cuenta las notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public long countUnread(Person person) {
        return notificationRepository.countUnreadByPerson(person);
    }

    /**
     * Marca una notificación como leída
     */
    @Transactional
    public void markAsRead(Long notificationId, Person person) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);

        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();

            // Validar que la notificación pertenece al usuario
            if (notification.getPerson().getId().equals(person.getId())) {
                notification.setRead(true);
                notificationRepository.save(notification);
                logger.info("[NOTIFICATION] Notificación " + notificationId + " marcada como leída");
            }
        }
    }

    /**
     * Marca todas las notificaciones como leídas
     */
    @Transactional
    public void markAllAsRead(Person person) {
        List<Notification> unread = notificationRepository.findUnreadByPerson(person);

        for (Notification notification : unread) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(unread);
        logger.info("[NOTIFICATION] " + unread.size() + " notificaciones marcadas como leídas");
    }

    /**
     * Elimina una notificación
     */
    @Transactional
    public void deleteNotification(Long notificationId, Person person) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);

        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();

            // Validar que la notificación pertenece al usuario
            if (notification.getPerson().getId().equals(person.getId())) {
                notificationRepository.delete(notification);
                logger.info("[NOTIFICATION] Notificación " + notificationId + " eliminada");
            }
        }
    }
}