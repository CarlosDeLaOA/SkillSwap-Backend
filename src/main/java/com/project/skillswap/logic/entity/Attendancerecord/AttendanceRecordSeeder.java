
package com.project.skillswap.logic.entity.Attendancerecord;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial attendance records in the database
 */
@Order(9)
@Component
public class AttendanceRecordSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceRecordSeeder.class);

    //#region Dependencies
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LearningSessionRepository learningSessionRepository;
    //#endregion

    //#region Constructor
    /**
     * Creates a new AttendanceRecordSeeder instance
     *
     * @param attendanceRecordRepository the attendance record repository
     * @param learningSessionRepository the learning session repository
     */
    public AttendanceRecordSeeder(
            AttendanceRecordRepository attendanceRecordRepository,
            LearningSessionRepository learningSessionRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.learningSessionRepository = learningSessionRepository;
    }
    //#endregion

    //#region Event Handling
    /**
     * Handles the application context refreshed event to seed initial data
     *
     * @param event the context refreshed event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedAttendanceRecords();
    }
    //#endregion

    //#region Seeding Logic
    /**
     * Seeds attendance records into the database
     */
    private void seedAttendanceRecords() {
        List<AttendanceRecordData> attendanceRecordsToCreate = createAttendanceRecordDataList();

        for (AttendanceRecordData recordData : attendanceRecordsToCreate) {
            Optional<LearningSession> session = learningSessionRepository.findById(recordData.learningSessionId);

            if (session.isEmpty()) {
                continue;
            }

            Optional<AttendanceRecord> existingRecord = attendanceRecordRepository.findByLearningSession(session.get());

            if (existingRecord.isPresent()) {
                continue;
            }

            AttendanceRecord attendanceRecord = createAttendanceRecord(recordData, session.get());
            attendanceRecordRepository.save(attendanceRecord);
        }
    }

    /**
     * Creates an AttendanceRecord entity from AttendanceRecordData
     *
     * @param data the attendance record data
     * @param session the learning session
     * @return the created AttendanceRecord entity
     */
    private AttendanceRecord createAttendanceRecord(AttendanceRecordData data, LearningSession session) {
        AttendanceRecord record = new AttendanceRecord();
        record.setLearningSession(session);
        record.setActualDurationMinutes(data.actualDurationMinutes);
        record.setTotalParticipants(data.totalParticipants);
        record.setStartDatetime(data.startDatetime);
        record.setEndDatetime(data.endDatetime);
        return record;
    }

    /**
     * Creates the list of attendance record data to be seeded
     *
     * @return list of AttendanceRecordData objects
     */
    private List<AttendanceRecordData> createAttendanceRecordDataList() {
        List<AttendanceRecordData> records = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        records.add(new AttendanceRecordData(
                1L, 95, 3,
                toDate(now.minusDays(40).withHour(10).withMinute(0)),
                toDate(now.minusDays(40).withHour(11).withMinute(35))
        ));

        records.add(new AttendanceRecordData(
                2L, 125, 2,
                toDate(now.minusDays(30).withHour(14).withMinute(0)),
                toDate(now.minusDays(30).withHour(16).withMinute(5))
        ));

        records.add(new AttendanceRecordData(
                3L, 155, 4,
                toDate(now.minusDays(25).withHour(15).withMinute(0)),
                toDate(now.minusDays(25).withHour(17).withMinute(35))
        ));

        records.add(new AttendanceRecordData(
                4L, 118, 2,
                toDate(now.minusDays(50).withHour(9).withMinute(0)),
                toDate(now.minusDays(50).withHour(10).withMinute(58))
        ));

        records.add(new AttendanceRecordData(
                5L, 160, 3,
                toDate(now.minusDays(45).withHour(11).withMinute(0)),
                toDate(now.minusDays(45).withHour(13).withMinute(40))
        ));

        records.add(new AttendanceRecordData(
                6L, 185, 5,
                toDate(now.minusDays(35).withHour(16).withMinute(0)),
                toDate(now.minusDays(35).withHour(19).withMinute(5))
        ));

        // Session 20: Machine Learning con Python (180 min)
        records.add(new AttendanceRecordData(
                20L, 178, 4,
                toDate(now.minusDays(35).withHour(10).withMinute(0)),
                toDate(now.minusDays(35).withHour(12).withMinute(58))
        ));

        records.add(new AttendanceRecordData(
                21L, 245, 3,
                toDate(now.minusDays(32).withHour(14).withMinute(0)),
                toDate(now.minusDays(32).withHour(18).withMinute(5))
        ));

        records.add(new AttendanceRecordData(
                28L, 122, 2,
                toDate(now.minusDays(28).withHour(9).withMinute(30)),
                toDate(now.minusDays(28).withHour(11).withMinute(32))
        ));

        records.add(new AttendanceRecordData(
                29L, 148, 3,
                toDate(now.minusDays(26).withHour(13).withMinute(0)),
                toDate(now.minusDays(26).withHour(15).withMinute(28))
        ));

        records.add(new AttendanceRecordData(
                30L, 92, 4,
                toDate(now.minusDays(24).withHour(10).withMinute(0)),
                toDate(now.minusDays(24).withHour(11).withMinute(32))
        ));

        records.add(new AttendanceRecordData(
                31L, 118, 3,
                toDate(now.minusDays(22).withHour(15).withMinute(0)),
                toDate(now.minusDays(22).withHour(16).withMinute(58))
        ));

        records.add(new AttendanceRecordData(
                32L, 155, 2,
                toDate(now.minusDays(20).withHour(11).withMinute(0)),
                toDate(now.minusDays(20).withHour(13).withMinute(35))
        ));

        records.add(new AttendanceRecordData(
                34L, 125, 2,
                toDate(now.minusDays(18).withHour(16).withMinute(0)),
                toDate(now.minusDays(18).withHour(18).withMinute(5))
        ));

        records.add(new AttendanceRecordData(
                35L, 148, 4,
                toDate(now.minusDays(16).withHour(9).withMinute(0)),
                toDate(now.minusDays(16).withHour(11).withMinute(28))
        ));

        records.add(new AttendanceRecordData(
                36L, 122, 3,
                toDate(now.minusDays(15).withHour(14).withMinute(0)),
                toDate(now.minusDays(15).withHour(16).withMinute(2))
        ));

        records.add(new AttendanceRecordData(
                38L, 152, 2,
                toDate(now.minusDays(14).withHour(10).withMinute(30)),
                toDate(now.minusDays(14).withHour(13).withMinute(2))
        ));

        records.add(new AttendanceRecordData(
                39L, 88, 5,
                toDate(now.minusDays(13).withHour(8).withMinute(0)),
                toDate(now.minusDays(13).withHour(9).withMinute(28))
        ));

        records.add(new AttendanceRecordData(
                40L, 125, 3,
                toDate(now.minusDays(12).withHour(15).withMinute(0)),
                toDate(now.minusDays(12).withHour(17).withMinute(5))
        ));

        records.add(new AttendanceRecordData(
                42L, 148, 2,
                toDate(now.minusDays(11).withHour(11).withMinute(0)),
                toDate(now.minusDays(11).withHour(13).withMinute(28))
        ));

        records.add(new AttendanceRecordData(
                44L, 118, 3,
                toDate(now.minusDays(10).withHour(16).withMinute(30)),
                toDate(now.minusDays(10).withHour(18).withMinute(28))
        ));

        records.add(new AttendanceRecordData(
                46L, 155, 4,
                toDate(now.minusDays(9).withHour(13).withMinute(0)),
                toDate(now.minusDays(9).withHour(15).withMinute(35))
        ));

        records.add(new AttendanceRecordData(
                47L, 122, 2,
                toDate(now.minusDays(8).withHour(10).withMinute(0)),
                toDate(now.minusDays(8).withHour(12).withMinute(2))
        ));

        records.add(new AttendanceRecordData(
                49L, 78, 6,
                toDate(now.minusDays(7).withHour(7).withMinute(0)),
                toDate(now.minusDays(7).withHour(8).withMinute(18))
        ));

        records.add(new AttendanceRecordData(
                50L, 92, 5,
                toDate(now.minusDays(18).withHour(14).withMinute(0)),
                toDate(now.minusDays(18).withHour(15).withMinute(32))
        ));

        records.add(new AttendanceRecordData(
                51L, 185, 3,
                toDate(now.minusDays(25).withHour(9).withMinute(0)),
                toDate(now.minusDays(25).withHour(12).withMinute(5))
        ));

        records.add(new AttendanceRecordData(
                52L, 118, 4,
                toDate(now.minusDays(30).withHour(17).withMinute(0)),
                toDate(now.minusDays(30).withHour(18).withMinute(58))
        ));

        records.add(new AttendanceRecordData(
                54L, 78, 6,
                toDate(now.minusDays(15).withHour(11).withMinute(0)),
                toDate(now.minusDays(15).withHour(12).withMinute(18))
        ));

        records.add(new AttendanceRecordData(
                55L, 92, 5,
                toDate(now.minusDays(22).withHour(15).withMinute(30)),
                toDate(now.minusDays(22).withHour(17).withMinute(2))
        ));

        records.add(new AttendanceRecordData(
                57L, 152, 4,
                toDate(now.minusDays(28).withHour(10).withMinute(0)),
                toDate(now.minusDays(28).withHour(12).withMinute(32))
        ));

        records.add(new AttendanceRecordData(
                58L, 122, 3,
                toDate(now.minusDays(16).withHour(14).withMinute(0)),
                toDate(now.minusDays(16).withHour(16).withMinute(2))
        ));

        records.add(new AttendanceRecordData(
                59L, 62, 8,
                toDate(now.minusDays(10).withHour(18).withMinute(0)),
                toDate(now.minusDays(10).withHour(19).withMinute(2))
        ));

        records.add(new AttendanceRecordData(
                61L, 148, 3,
                toDate(now.minusDays(19).withHour(9).withMinute(30)),
                toDate(now.minusDays(19).withHour(12).withMinute(8))
        ));
        records.add(new AttendanceRecordData(
                62L, 88, 5,
                toDate(now.minusDays(17).withHour(17).withMinute(0)),
                toDate(now.minusDays(17).withHour(18).withMinute(28))
        ));

        records.add(new AttendanceRecordData(
                64L, 92, 3,
                toDate(now.minusDays(14).withHour(10).withMinute(0)),
                toDate(now.minusDays(14).withHour(11).withMinute(32))
        ));

        records.add(new AttendanceRecordData(
                65L, 118, 4,
                toDate(now.minusDays(13).withHour(16).withMinute(0)),
                toDate(now.minusDays(13).withHour(17).withMinute(58))
        ));

        records.add(new AttendanceRecordData(
                67L, 182, 2,
                toDate(now.minusDays(11).withHour(13).withMinute(0)),
                toDate(now.minusDays(11).withHour(16).withMinute(2))
        ));

        records.add(new AttendanceRecordData(
                68L, 155, 3,
                toDate(now.minusDays(9).withHour(10).withMinute(30)),
                toDate(now.minusDays(9).withHour(13).withMinute(5))
        ));

        records.add(new AttendanceRecordData(
                69L, 48, 10,
                toDate(now.minusDays(8).withHour(12).withMinute(0)),
                toDate(now.minusDays(8).withHour(12).withMinute(48))
        ));
        records.add(new AttendanceRecordData(
                75L, 152, 6,
                toDate(now.minusDays(45).withHour(14).withMinute(0)),
                toDate(now.minusDays(45).withHour(16).withMinute(32))
        ));
        records.add(new AttendanceRecordData(
                76L, 125, 5,
                toDate(now.minusDays(38).withHour(10).withMinute(0)),
                toDate(now.minusDays(38).withHour(12).withMinute(5))
        ));

        records.add(new AttendanceRecordData(
                77L, 92, 8,
                toDate(now.minusDays(32).withHour(16).withMinute(30)),
                toDate(now.minusDays(32).withHour(18).withMinute(2))
        ));

        records.add(new AttendanceRecordData(
                78L, 185, 4,
                toDate(now.minusDays(28).withHour(9).withMinute(0)),
                toDate(now.minusDays(28).withHour(12).withMinute(5))
        ));

        return records;
    }

    /**
     * Converts LocalDateTime to Date
     *
     * @param localDateTime the local date time
     * @return the converted Date
     */
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    //#endregion

    //#region Inner Class
    /**
     * Data class holding information for creating attendance records
     */
    private static class AttendanceRecordData {
        Long learningSessionId;
        Integer actualDurationMinutes;
        Integer totalParticipants;
        Date startDatetime;
        Date endDatetime;

        AttendanceRecordData(Long learningSessionId, Integer actualDurationMinutes,
                             Integer totalParticipants, Date startDatetime, Date endDatetime) {
            this.learningSessionId = learningSessionId;
            this.actualDurationMinutes = actualDurationMinutes;
            this.totalParticipants = totalParticipants;
            this.startDatetime = startDatetime;
            this.endDatetime = endDatetime;
        }
    }
    //#endregion
}