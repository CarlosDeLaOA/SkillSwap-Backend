
package com.project.skillswap.logic.entity.Notification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Notification.SessionAlertDTO;
import com.project.skillswap.logic.entity.Notification.UserSessionAlertDTO;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Notification.Notification;
import com.project.skillswap.logic.entity.Notification.NotificationRepository;
import com.project.skillswap.logic.entity.Notification.NotificationType;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Servicio para gestionar alertas de sesiones próximas
 */
@Service
public class SessionAlertService {
    private static final Logger logger = LoggerFactory.getLogger(SessionAlertService.class);

    private final LearningSessionRepository learningSessionRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final AlertEmailService alertEmailService;

    public SessionAlertService(
            LearningSessionRepository learningSessionRepository,
            BookingRepository bookingRepository,
            NotificationRepository notificationRepository,
            AlertEmailService alertEmailService) {
        this.learningSessionRepository = learningSessionRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.alertEmailService = alertEmailService;
    }

    /**
     * Procesa y envía alertas de sesiones próximas (próximos 7 días)
     */
    /**
     * Procesa y envía alertas de sesiones SIN verificar duplicados (para testing)
     */
    @Transactional
    public void processAndSendSessionAlerts() {
        logger.info("[TESTING] Buscando sesiones programadas para esta semana...");

        // Calcular rango de fechas (hoy + 7 días)
        Date startDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(LocalDate.now().plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Obtener sesiones y bookings
        List<LearningSession> sessions = learningSessionRepository.findScheduledSessionsInDateRange(startDate, endDate);
        List<Booking> bookings = bookingRepository.findActiveBookingsInDateRange(startDate, endDate);

        logger.info("Sesiones encontradas: " + sessions.size());
        logger.info("Bookings encontrados: " + bookings.size());

        // Agrupar por usuario
        Map<Long, UserSessionAlertDTO> userAlertsMap = new HashMap<>();

        // Procesar sesiones como instructor
        for (LearningSession session : sessions) {
            Person instructor = session.getInstructor().getPerson();
            Long personId = instructor.getId();

            UserSessionAlertDTO userAlert = userAlertsMap.computeIfAbsent(personId,
                    id -> new UserSessionAlertDTO(id, instructor.getFullName(), instructor.getEmail()));

            SessionAlertDTO sessionDTO = new SessionAlertDTO(
                    session.getId(),
                    session.getTitle(),
                    session.getSkill().getName(),
                    session.getScheduledDatetime(),
                    session.getDurationMinutes(),
                    "INSTRUCTOR",
                    instructor.getFullName(),
                    session.getVideoCallLink()
            );

            userAlert.getInstructorSessions().add(sessionDTO);
        }

        // Procesar bookings como learner
        for (Booking booking : bookings) {
            Person learner = booking.getLearner().getPerson();
            Long personId = learner.getId();
            LearningSession session = booking.getLearningSession();

            UserSessionAlertDTO userAlert = userAlertsMap.computeIfAbsent(personId,
                    id -> new UserSessionAlertDTO(id, learner.getFullName(), learner.getEmail()));

            SessionAlertDTO sessionDTO = new SessionAlertDTO(
                    session.getId(),
                    session.getTitle(),
                    session.getSkill().getName(),
                    session.getScheduledDatetime(),
                    session.getDurationMinutes(),
                    "LEARNER",
                    session.getInstructor().getPerson().getFullName(),
                    session.getVideoCallLink()
            );

            userAlert.getLearnerSessions().add(sessionDTO);
        }

        // Enviar alertas
        int successCount = 0;
        int errorCount = 0;

        for (UserSessionAlertDTO userAlert : userAlertsMap.values()) {
            if (userAlert.hasSessions()) {
                try {
                    alertEmailService.sendSessionAlert(userAlert);
                    saveNotification(userAlert);
                    successCount++;
                    logger.info("Alerta de sesiones enviada a: " + userAlert.getFullName());
                } catch (MessagingException e) {
                    errorCount++;
                    logger.info("Error al enviar alerta a " + userAlert.getFullName() + ": " + e.getMessage());
                } catch (Exception e) {
                    errorCount++;
                    logger.info("Error inesperado para " + userAlert.getFullName() + ": " + e.getMessage());
                }
            }
        }

        logger.info("Resumen: " + successCount + " exitosas, " + errorCount + " fallidas");
    }

    /**
     * Guarda una notificación en la base de datos
     */
    private void saveNotification(UserSessionAlertDTO userAlert) {
        Person person = new Person();
        person.setId(userAlert.getPersonId());

        Notification notification = new Notification();
        notification.setPerson(person);
        notification.setType(NotificationType.SESSION_REMINDER);
        notification.setTitle("Sesiones programadas para esta semana");

        // Crear metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", "Tienes " + userAlert.getTotalSessions() + " sesión(es) programadas para esta semana");
        metadata.put("totalSessions", userAlert.getTotalSessions());
        metadata.put("instructorSessionsCount", userAlert.getInstructorSessions().size());
        metadata.put("learnerSessionsCount", userAlert.getLearnerSessions().size());
        metadata.put("eventType", "SESSION_REMINDER");

        notification.setMetadata(metadata);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    /**
     * Obtiene preview de alertas que se enviarían (para testing)
     */
    public Map<Long, UserSessionAlertDTO> getAlertsPreview() {
        Date startDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(LocalDate.now().plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<LearningSession> sessions = learningSessionRepository.findScheduledSessionsInDateRange(startDate, endDate);
        List<Booking> bookings = bookingRepository.findActiveBookingsInDateRange(startDate, endDate);

        Map<Long, UserSessionAlertDTO> userAlertsMap = new HashMap<>();

        for (LearningSession session : sessions) {
            Person instructor = session.getInstructor().getPerson();
            Long personId = instructor.getId();

            UserSessionAlertDTO userAlert = userAlertsMap.computeIfAbsent(personId,
                    id -> new UserSessionAlertDTO(id, instructor.getFullName(), instructor.getEmail()));

            SessionAlertDTO sessionDTO = new SessionAlertDTO(
                    session.getId(),
                    session.getTitle(),
                    session.getSkill().getName(),
                    session.getScheduledDatetime(),
                    session.getDurationMinutes(),
                    "INSTRUCTOR",
                    instructor.getFullName(),
                    session.getVideoCallLink()
            );

            userAlert.getInstructorSessions().add(sessionDTO);
        }

        for (Booking booking : bookings) {
            Person learner = booking.getLearner().getPerson();
            Long personId = learner.getId();
            LearningSession session = booking.getLearningSession();

            UserSessionAlertDTO userAlert = userAlertsMap.computeIfAbsent(personId,
                    id -> new UserSessionAlertDTO(id, learner.getFullName(), learner.getEmail()));

            SessionAlertDTO sessionDTO = new SessionAlertDTO(
                    session.getId(),
                    session.getTitle(),
                    session.getSkill().getName(),
                    session.getScheduledDatetime(),
                    session.getDurationMinutes(),
                    "LEARNER",
                    session.getInstructor().getPerson().getFullName(),
                    session.getVideoCallLink()
            );

            userAlert.getLearnerSessions().add(sessionDTO);
        }

        return userAlertsMap;
    }
}