package com.project.skillswap.logic.entity.WeeklyReport;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Componente scheduler para el envío automático de reportes semanales.
 */
@Component
public class WeeklyReportScheduler {

    //#region Dependencies
    private final WeeklyReportService weeklyReportService;

    public WeeklyReportScheduler(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }
    //#endregion

    //#region Scheduled Methods
    /**
     * Ejecuta el envío de reportes semanales todos los domingos a las 12:00 PM.
     */
    @Scheduled(cron = "0 0 12 * * SUN")
    //@Scheduled(cron = "0 * * * * *") // Para probar el codigo con facilidad (Sammy o Jose) esto pasaría cada min
    public void sendWeeklyReports() {
        System.out.println("Iniciando generación de reportes semanales...");
        weeklyReportService.generateAndSendWeeklyReports();
        System.out.println("Reportes semanales generados y enviados.");
    }

    /**
     * Limpia reportes antiguos el primer día de cada mes a las 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 1 * *")
    public void cleanOldReports() {
        System.out.println("Iniciando limpieza de reportes antiguos...");
        weeklyReportService.cleanOldReports(6);
        System.out.println("Reportes antiguos eliminados.");
    }
    //#endregion
}