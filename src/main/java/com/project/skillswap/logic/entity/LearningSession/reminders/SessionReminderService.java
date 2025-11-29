package com.project.skillswap.logic.entity.LearningSession.reminders;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import com.project.skillswap.logic.entity.Notification.Notification;
import com.project.skillswap.logic.entity.Notification.NotificationRepository;
import com.project.skillswap.logic.entity.Notification.NotificationType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Servicio para gestionar recordatorios de sesiones
 * Encuentra sesiones que comienzan en 24 horas y envía emails de recordatorio
 * Recordatorio automático 24 horas antes vía email
 */
@Service
public class SessionReminderService {

    //#region Dependencies
    private final LearningSessionRepository learningSessionRepository;
    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final SessionReminderEmailBuilder emailBuilder;
    //#endregion

    //#region Constructor
    /// *** Constructor injection (mejor que @Autowired)
    public SessionReminderService(
            LearningSessionRepository learningSessionRepository,
            NotificationRepository notificationRepository,
            JavaMailSender mailSender,
            SessionReminderEmailBuilder emailBuilder) {
        this.learningSessionRepository = learningSessionRepository;
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
        this.emailBuilder = emailBuilder;
    }
    //#endregion

    //#region Constants
    private static final long REMINDER_WINDOW_MINUTES = 60;  // Ventana de búsqueda: ±60 minutos
    private static final long HOURS_BEFORE_SESSION = 24;     // Recordatorio 24 horas antes
    //#endregion

    //#region Public Methods
    /**
     * Busca sesiones que comienzan en ~24 horas y envía recordatorios
     * Retorna cantidad de recordatorios enviados
     *
     * @return Cantidad de recordatorios enviados exitosamente
     */
    public int sendRemindersForSessionsInNextDay() {
        int remindersCount = 0;

        /// Calcular rango de búsqueda (24h ± 60 minutos)
        Date now = new Date();
        Date reminderTime = calculateReminderTime(now);
        Date windowStart = new Date(reminderTime.getTime() - (REMINDER_WINDOW_MINUTES * 60 * 1000));
        Date windowEnd = new Date(reminderTime.getTime() + (REMINDER_WINDOW_MINUTES * 60 * 1000));

        System.out.println(" [SessionReminderService] Buscando sesiones en rango:");
        System.out.println("   - Desde: " + formatDate(windowStart));
        System.out.println("   - Hasta: " + formatDate(windowEnd));

        try {
            ///  Obtener sesiones programadas en el rango
            List<LearningSession> upcomingSessions = learningSessionRepository
                    .findScheduledSessionsInDateRange(windowStart, windowEnd);

            System.out.println(" [SessionReminderService] Sesiones encontradas: " + upcomingSessions.size());

            /// Procesar cada sesión
            for (LearningSession session : upcomingSessions) {
                if (shouldSendReminder(session)) {
                    boolean sent = sendReminderEmail(session);
                    if (sent) {
                        registerReminderSent(session);
                        remindersCount++;
                        System.out.println("   ✅ Recordatorio enviado para: " + session.getTitle());
                    } else {
                        System.out.println("   ❌ Error enviando recordatorio para: " + session.getTitle());
                    }
                } else {
                    System.out.println("   ⏭️ Saltar sesión: " + session.getTitle() + " (ya enviado)");
                }
            }

        } catch (Exception e) {
            System.err.println(" [SessionReminderService] Error buscando sesiones: " + e.getMessage());
            e.printStackTrace();
        }

        return remindersCount;
    }
    //#endregion

    //#region Private Methods - Validation
    /**
     * Valida si se debe enviar recordatorio para una sesión
     * No enviar si ya se envió anteriormente
     *
     * @param session Sesión a verificar
     * @return true si se debe enviar recordatorio
     */
    private boolean shouldSendReminder(LearningSession session) {
        if (session.getStatus() != SessionStatus.SCHEDULED && session.getStatus() != SessionStatus.ACTIVE) {
            return false;  // Solo recordar sesiones programadas o activas
        }

        /// Verificar si ya se envió recordatorio
        long count = notificationRepository.countByPersonAndTypeAndSendDateAfter(
                session.getInstructor().getPerson(),
                NotificationType.REMINDER,
                new Date(System.currentTimeMillis() - (26 * 60 * 60 * 1000))  // Últimas 26 horas
        );

        return count == 0;  // Si no hay recordatorio reciente, enviar
    }
    //#endregion

    //#region Private Methods - Email
    /**
     * Envía email de recordatorio para una sesión
     *
     * @param session Sesión para la cual enviar recordatorio
     * @return true si se envió exitosamente
     */
    private boolean sendReminderEmail(LearningSession session) {
        try {
            System.out.println("   [Enviando email de recordatorio]");

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            String recipientEmail = session.getInstructor().getPerson().getEmail();

            helper.setFrom("noreply@skillswap.com");
            helper.setTo(recipientEmail);
            helper.setSubject(emailBuilder.buildSubject(session));

            String htmlContent = emailBuilder.buildReminderEmail(session);
            helper.setText(htmlContent, true);

            mailSender.send(msg);

            System.out.println("   [Email enviado a: " + recipientEmail + "]");
            return true;

        } catch (Exception e) {
            System.err.println("   [Error enviando email: " + e.getMessage() + "]");
            return false;
        }
    }

    /**
     * Registra que un recordatorio fue enviado
     *
     * @param session Sesión para la cual se envió recordatorio
     */
    private void registerReminderSent(LearningSession session) {
        try {
            Notification notification = new Notification();
            notification.setPerson(session.getInstructor().getPerson());
            notification.setType(NotificationType.REMINDER);
            notification.setTitle("Recordatorio - " + session.getTitle());
            notification.setMessage("Se envió recordatorio 24 horas antes de tu sesión programada");
            notification.setRead(false);

            notificationRepository.save(notification);

            System.out.println("   [Registro de notificación creado]");

        } catch (Exception e) {
            System.err.println("   [Error registrando notificación: " + e.getMessage() + "]");
        }
    }
    //#endregion

    //#region Private Methods - Utilities
    /**
     * Calcula la fecha/hora objetivo para recordatorios (24 horas desde ahora)
     *
     * @param now Fecha actual
     * @return Fecha calculada
     */
    private Date calculateReminderTime(Date now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.HOUR_OF_DAY, (int) HOURS_BEFORE_SESSION);
        return calendar.getTime();
    }

    /**
     * Formatea una fecha para logging
     *
     * @param date Fecha a formatear
     * @return String formateado
     */
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
    //#endregion
}