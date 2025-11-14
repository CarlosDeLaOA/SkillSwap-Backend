/**package com.project.skillswap.logic.entity.LearningSession;

import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import com.project.skillswap.logic.entity.Booking.BookingStatus;
import com.project.skillswap.logic.entity.Booking.BookingType;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Seeder específico para agregar datos de prueba para el componente Skills Progress
 /**
@Order(8)
@Component
public class SkillsProgressDataSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private final LearningSessionRepository learningSessionRepository;
    private final InstructorRepository instructorRepository;
    private final LearnerRepository learnerRepository;
    private final SkillRepository skillRepository;
    private final BookingRepository bookingRepository;

    public SkillsProgressDataSeeder(
            LearningSessionRepository learningSessionRepository,
            InstructorRepository instructorRepository,
            LearnerRepository learnerRepository,
            SkillRepository skillRepository,
            BookingRepository bookingRepository) {
        this.learningSessionRepository = learningSessionRepository;
        this.instructorRepository = instructorRepository;
        this.learnerRepository = learnerRepository;
        this.skillRepository = skillRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedSkillsProgressData();
    }

    private void seedSkillsProgressData() {
        System.out.println("🎯 Iniciando seeder de Skills Progress Data...");

        // Verificar que existan los usuarios necesarios
        Optional<Learner> learnerOpt = learnerRepository.findByPersonId(2L);
        Optional<Instructor> instructorOpt = instructorRepository.findByPersonId(1L);

        if (learnerOpt.isEmpty()) {
            System.out.println("⚠️ No se encontró learner con person_id=2");
            return;
        }

        if (instructorOpt.isEmpty()) {
            System.out.println("⚠️ No se encontró instructor con person_id=1");
            return;
        }

        Learner learner = learnerOpt.get();
        Instructor instructor = instructorOpt.get();

        // Obtener todas las skills necesarias de una vez
        List<Skill> allSkills = skillRepository.findAll();
        Map<String, Skill> skillsMap = new HashMap<>();

        for (Skill skill : allSkills) {
            if (skill.getName() != null) {
                skillsMap.put(skill.getName(), skill);
            }
        }

        // Verificar que existan las skills necesarias
        String[] requiredSkills = {"Java", "Python", "JavaScript", "Inglés", "React"};
        for (String skillName : requiredSkills) {
            if (!skillsMap.containsKey(skillName)) {
                System.out.println("⚠️ No se encontró la skill: " + skillName);
                return;
            }
        }

        // Verificar si ya existen sesiones de prueba
        long existingSessions = countExistingTestSessions(learner.getId().intValue(), skillsMap);
        if (existingSessions >= 50) {
            System.out.println("✅ Ya existen " + existingSessions + " sesiones de prueba. Saltando seeder.");
            return;
        }

        System.out.println("📊 Creando sesiones de prueba para Skills Progress...");

        int totalCreated = 0;
        LocalDateTime now = LocalDateTime.now();

        // Crear sesiones para cada skill
        totalCreated += createSessionsForSkill(skillsMap.get("Java"), instructor, learner, 7, 3, now);
        totalCreated += createSessionsForSkill(skillsMap.get("Python"), instructor, learner, 5, 5, now);
        totalCreated += createSessionsForSkill(skillsMap.get("JavaScript"), instructor, learner, 8, 2, now);
        totalCreated += createSessionsForSkill(skillsMap.get("Inglés"), instructor, learner, 10, 4, now);
        totalCreated += createSessionsForSkill(skillsMap.get("React"), instructor, learner, 3, 7, now);

        System.out.println("✅ Seeder completado: " + totalCreated + " sesiones creadas en total");
    }

    private long countExistingTestSessions(Integer learnerId, Map<String, Skill> skillsMap) {
        List<Booking> allBookings = bookingRepository.findByLearnerId(learnerId);

        return allBookings.stream()
                .filter(booking -> {
                    if (booking.getLearningSession() == null || booking.getLearningSession().getSkill() == null) {
                        return false;
                    }
                    String skillName = booking.getLearningSession().getSkill().getName();
                    return skillsMap.containsKey(skillName);
                })
                .count();
    }

    private int createSessionsForSkill(Skill skill, Instructor instructor, Learner learner,
                                       int completedCount, int pendingCount, LocalDateTime now) {
        int created = 0;

        // Crear sesiones completadas (en el pasado)
        for (int i = 0; i < completedCount; i++) {
            LocalDateTime sessionDate = now.minusDays(45 - (i * 5));
            LearningSession session = createSession(skill, instructor, sessionDate,
                    SessionStatus.FINISHED, i);

            LearningSession savedSession = learningSessionRepository.save(session);
            createBooking(savedSession, learner, true);
            created++;
        }

        // Crear sesiones pendientes (en el futuro)
        for (int i = 0; i < pendingCount; i++) {
            LocalDateTime sessionDate = now.plusDays(2 + (i * 3));
            LearningSession session = createSession(skill, instructor, sessionDate,
                    SessionStatus.SCHEDULED, i + completedCount);

            LearningSession savedSession = learningSessionRepository.save(session);
            createBooking(savedSession, learner, false);
            created++;
        }

        System.out.println("  ✅ " + skill.getName() + ": " + created + " sesiones (" +
                completedCount + " completadas, " + pendingCount + " pendientes)");

        return created;
    }

    private LearningSession createSession(Skill skill, Instructor instructor,
                                          LocalDateTime scheduledTime, SessionStatus status,
                                          int index) {
        LearningSession session = new LearningSession();
        session.setInstructor(instructor);
        session.setSkill(skill);
        session.setTitle(skill.getName() + " - Sesión #" + (index + 1));
        session.setDescription("Sesión de práctica y aprendizaje de " + skill.getName());
        session.setScheduledDatetime(toDate(scheduledTime));
        session.setDurationMinutes(60);
        session.setType(SessionType.SCHEDULED);
        session.setMaxCapacity(10);
        session.setIsPremium(false);
        session.setSkillcoinsCost(BigDecimal.ZERO);
        session.setLanguage("es");
        session.setStatus(status);
        session.setVideoCallLink(generateMeetLink(skill.getName(), index));

        return session;
    }

    private void createBooking(LearningSession session, Learner learner, boolean attended) {
        // Verificar si ya existe un booking
        Optional<Booking> existingBooking = bookingRepository.findByLearningSessionAndLearner(
                session, learner);

        if (existingBooking.isPresent()) {
            return;
        }

        Booking booking = new Booking();
        booking.setLearningSession(session);
        booking.setLearner(learner);
        booking.setType(BookingType.INDIVIDUAL);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAccessLink(session.getVideoCallLink());
        booking.setAttended(attended);

        bookingRepository.save(booking);
    }

    private String generateMeetLink(String skillName, int index) {
        String cleanSkillName = skillName.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u");

        return "https://meet.google.com/skills-" + cleanSkillName + "-" +
                String.format("%03d", index);
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
  */