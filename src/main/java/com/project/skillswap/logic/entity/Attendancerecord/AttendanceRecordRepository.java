package com.project.skillswap.logic.entity.Attendancerecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Integer> {
    Optional<AttendanceRecord> findByLearningSession(LearningSession learningSession);
}
