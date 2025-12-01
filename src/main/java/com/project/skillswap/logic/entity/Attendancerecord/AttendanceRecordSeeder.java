package com.project.skillswap.logic.entity.Attendancerecord;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Order(7)
@Component
public class AttendanceRecordSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceRecordSeeder.class);

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final BookingRepository bookingRepository;
    private final Random random = new Random();

    public AttendanceRecordSeeder(AttendanceRecordRepository attendanceRecordRepository,
                                  LearningSessionRepository learningSessionRepository,
                                  BookingRepository bookingRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (attendanceRecordRepository.count() > 0) {
            logger.info("AttendanceRecordSeeder: Ya existen registros, omitiendo seed");
            return;
        }
        this.seedAttendanceRecords();
    }

    private void seedAttendanceRecords() {
        // Solo crear registros para sesiones FINISHED
        List<LearningSession> finishedSessions = learningSessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.FINISHED)
                .toList();

        if (finishedSessions.isEmpty()) {
            logger.warn("No hay sesiones finalizadas para crear registros de asistencia");
            return;
        }

        for (LearningSession session : finishedSessions) {
            AttendanceRecord record = createAttendanceRecord(session);
            attendanceRecordRepository.save(record);
        }

        logger.info("AttendanceRecordSeeder: " + finishedSessions.size() + " registros de asistencia creados");
    }

    private AttendanceRecord createAttendanceRecord(LearningSession session) {
        AttendanceRecord record = new AttendanceRecord();

        record.setLearningSession(session);

        int variance = random.nextInt(21) - 10;
        int actualDuration = session.getDurationMinutes() + variance;
        actualDuration = Math.max(actualDuration, 15);
        record.setActualDurationMinutes(actualDuration);
        List<Booking> allBookings = bookingRepository.findAll();
        long attendedCount = allBookings.stream()
                .filter(b -> b.getLearningSession().getId().equals(session.getId()))
                .filter(b -> b.getAttended() != null && b.getAttended())
                .count();

        record.setTotalParticipants((int) attendedCount);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(session.getScheduledDatetime());
        startCal.add(Calendar.MINUTE, random.nextInt(11) - 5); // -5 a +5 minutos
        record.setStartDatetime(startCal.getTime());

        // End datetime = start + actual duration
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(record.getStartDatetime());
        endCal.add(Calendar.MINUTE, actualDuration);
        record.setEndDatetime(endCal.getTime());

        return record;
    }
}