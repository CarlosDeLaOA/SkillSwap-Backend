package com.project.skillswap.logic.entity.LearningSession.reminders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Componente scheduler para enviar recordatorios de sesiones
 * Ejecuta automáticamente cada hora para verificar sesiones próximas
 * Recordatorio automático 24 horas antes vía email
 */
@Component
public class SessionReminderScheduler {

    //#region Dependencies
    @Autowired
    private SessionReminderService reminderService;
    //#endregion

    //#region Scheduled Tasks
    /**
     * Ejecuta cada hora para verificar y enviar recordatorios
     * Busca sesiones que comienzan en exactamente 24 horas
     *
     * Horario: Cada hora (00:00, 01:00, 02:00, etc.)
     */
    @Scheduled(cron = "0 0 * * * *")  // Cada hora en punto
    public void sendRemindersForUpcomingSessions() {
        System.out.println("=============================================================");
        System.out.println(" [SessionReminderScheduler] 🔔 INICIANDO BÚSQUEDA DE RECORDATORIOS");
        System.out.println("   Buscando sesiones que comienzan en ~24 horas...");
        System.out.println(" [SessionReminderScheduler] Timestamp: " + System.currentTimeMillis());
        System.out.println("=============================================================");

        try {
            /// *** Obtener sesiones próximas a 24 horas
            int remindersCount = reminderService.sendRemindersForSessionsInNextDay();

            System.out.println(" [SessionReminderScheduler] ✅ Ejecución completada");
            System.out.println("   Recordatorios enviados: " + remindersCount);
            System.out.println("=============================================================\n");

        } catch (Exception e) {
            System.err.println(" [SessionReminderScheduler] ❌ Error durante ejecución:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
            System.out.println("=============================================================\n");
        }
    }

    /**
     * ALTERNATIVA: Ejecuta cada 30 minutos (ajustar según necesidad)
     * Descomenta si prefieres verificar más frecuentemente
     */
    // @Scheduled(cron = "0 */30 * * * *")  // Cada 30 minutos
    // public void sendRemindersEveryThirtyMinutes() { ... }

    /**
     * ALTERNATIVA: Ejecuta diariamente a las 9:00 AM
     * Descomenta si prefieres un horario específico
     */
    // @Scheduled(cron = "0 0 9 * * *")  // Cada día a las 9 AM
    // public void sendRemindersDaily() { ... }
    //#endregion
}