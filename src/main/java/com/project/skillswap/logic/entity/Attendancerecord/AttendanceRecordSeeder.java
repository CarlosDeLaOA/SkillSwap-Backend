package com.project.skillswap.logic.entity.Attendancerecord;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Order(5)
@Component
public class AttendanceRecordSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final Random random = new Random();

    public AttendanceRecordSeeder(
            AttendanceRecordRepository attendanceRecordRepository,
            LearningSessionRepository learningSessionRepository
    ) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.learningSessionRepository = learningSessionRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedAttendanceRecords();
    }

    private void seedAttendanceRecords() {

        long existingRecords = attendanceRecordRepository.count();
        if (existingRecords > 10) {
            System.out.println("⚠️ Ya existen " + existingRecords + " registros de asistencia. Saltando seeder.");
            return;
        }


        if (existingRecords > 0) {
            System.out.println("🗑️ Limpiando " + existingRecords + " registros antiguos...");
            attendanceRecordRepository.deleteAll();
        }


        List<LearningSession> finishedSessions = learningSessionRepository
                .findByStatus(SessionStatus.FINISHED);

        if (finishedSessions.isEmpty()) {
            System.out.println("⚠️ No hay sesiones finalizadas. No se pueden crear registros de asistencia.");
            return;
        }

        System.out.println("📊 Creando registros de asistencia para " + finishedSessions.size() + " sesiones finalizadas...");

        int createdRecords = 0;
        for (LearningSession session : finishedSessions) {
            try {
                AttendanceRecord record = createAttendanceRecordForSession(session);
                attendanceRecordRepository.save(record);
                createdRecords++;


                if (createdRecords % 50 == 0) {
                    System.out.println("  ✅ Creados " + createdRecords + " registros...");
                }
            } catch (Exception e) {
                System.err.println("❌ Error creando registro para sesión " + session.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Se crearon " + createdRecords + " registros de asistencia en total");
    }

    private AttendanceRecord createAttendanceRecordForSession(LearningSession session) {
        LocalDateTime sessionDate = session.getScheduledDatetime()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        AttendanceRecord record = new AttendanceRecord();
        record.setLearningSession(session);


        int maxCapacity = session.getMaxCapacity() != null ? session.getMaxCapacity() : 10;
        double occupancyRate = 0.60 + (random.nextDouble() * 0.30); // 60-90%
        int totalParticipants = Math.max(1, (int) Math.ceil(maxCapacity * occupancyRate));
        record.setTotalParticipants(totalParticipants);


        LocalDateTime startTime = sessionDate.minusMinutes(5 + random.nextInt(6));
        record.setStartDatetime(toDate(startTime));


        int plannedDuration = session.getDurationMinutes() != null ? session.getDurationMinutes() : 60;
        int variationMinutes = -10 + random.nextInt(21); // Entre -10 y +10
        int actualDuration = plannedDuration + variationMinutes;
        actualDuration = Math.max(30, actualDuration);
        record.setActualDurationMinutes(actualDuration);


        LocalDateTime endTime = startTime.plusMinutes(actualDuration);
        record.setEndDatetime(toDate(endTime));

        return record;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}