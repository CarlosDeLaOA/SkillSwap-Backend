package com.project.skillswap.logic.entity.LearningSession;

import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial learning sessions in the database
 */
@Order(4)
@Component
public class LearningSessionSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final LearningSessionRepository learningSessionRepository;
    private final InstructorRepository instructorRepository;
    private final SkillRepository skillRepository;
    //#endregion

    //#region Constructor
    public LearningSessionSeeder(
            LearningSessionRepository learningSessionRepository,
            InstructorRepository instructorRepository,
            SkillRepository skillRepository) {
        this.learningSessionRepository = learningSessionRepository;
        this.instructorRepository = instructorRepository;
        this.skillRepository = skillRepository;
    }
    //#endregion

    //#region Event Handling
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedLearningSessions();
    }
    //#endregion

    //#region Seeding Logic
    private void seedLearningSessions() {
        List<LearningSessionData> sessionsToCreate = createLearningSessionDataList();

        for (LearningSessionData sessionData : sessionsToCreate) {
            Optional<Instructor> instructor = instructorRepository.findById(sessionData.instructorId.intValue());
            Optional<Skill> skill = skillRepository.findById(sessionData.skillId);

            if (instructor.isEmpty() || skill.isEmpty()) {
                continue;
            }

            LearningSession session = createLearningSession(sessionData, instructor.get(), skill.get());
            learningSessionRepository.save(session);
        }

    }

    private LearningSession createLearningSession(LearningSessionData data, Instructor instructor, Skill skill) {
        LearningSession session = new LearningSession();
        session.setInstructor(instructor);
        session.setSkill(skill);
        session.setTitle(data.title);
        session.setDescription(data.description);
        session.setScheduledDatetime(data.scheduledDatetime);
        session.setDurationMinutes(data.durationMinutes);
        session.setType(data.type);
        session.setMaxCapacity(data.maxCapacity);
        session.setIsPremium(data.isPremium);
        session.setSkillcoinsCost(data.skillcoinsCost);
        session.setLanguage(data.language);
        session.setStatus(data.status);
        session.setVideoCallLink(data.videoCallLink);
        return session;
    }

    private List<LearningSessionData> createLearningSessionDataList() {
        List<LearningSessionData> sessions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        sessions.add(new LearningSessionData(
                6L, 21L, "Introduction to Data Science",
                "Learn the fundamentals of data science and analytics",
                toDate(now.minusDays(45)), 120, SessionType.SCHEDULED,
                15, true, new BigDecimal("15.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-ds-001"
        ));

        sessions.add(new LearningSessionData(
                6L, 21L, "Machine Learning Fundamentals",
                "Deep dive into ML algorithms and applications",
                toDate(now.minusDays(30)), 150, SessionType.SCHEDULED,
                12, true, new BigDecimal("20.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-ml-002"
        ));

        sessions.add(new LearningSessionData(
                6L, 21L, "Data Pipelines with Python",
                "Build robust data pipelines using Python",
                toDate(now.minusDays(20)), 90, SessionType.SCHEDULED,
                10, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-dp-003"
        ));

        sessions.add(new LearningSessionData(
                6L, 21L, "Advanced Machine Learning Techniques",
                "Explore advanced ML concepts and neural networks",
                toDate(now.minusDays(15)), 180, SessionType.IMMEDIATE,
                8, true, new BigDecimal("25.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-aml-004"
        ));

        sessions.add(new LearningSessionData(
                6L, 21L, "Deep Learning Workshop",
                "Hands-on workshop on deep learning architectures",
                toDate(now.plusDays(2)), 120, SessionType.SCHEDULED,
                15, true, new BigDecimal("30.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-dl-005"
        ));

        sessions.add(new LearningSessionData(
                6L, 21L, "Natural Language Processing",
                "Introduction to NLP techniques and applications",
                toDate(now.plusDays(5)), 90, SessionType.SCHEDULED,
                12, false, BigDecimal.ZERO, "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-nlp-006"
        ));

        sessions.add(new LearningSessionData(
                6L, 21L, "Computer Vision Basics",
                "Learn image processing and computer vision",
                toDate(now.plusDays(8)), 120, SessionType.SCHEDULED,
                10, true, new BigDecimal("20.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-cv-007"
        ));

        sessions.add(new LearningSessionData(
                6L, 21L, "Big Data Analytics",
                "Working with large datasets and distributed computing",
                toDate(now.plusDays(12)), 150, SessionType.SCHEDULED,
                15, true, new BigDecimal("25.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-bda-008"
        ));

        sessions.add(new LearningSessionData(
                6L, 21L, "AI Ethics and Responsible AI",
                "Understanding ethical considerations in AI development",
                toDate(now.plusDays(15)), 60, SessionType.SCHEDULED,
                20, false, BigDecimal.ZERO, "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-ethics-009"
        ));

        sessions.add(new LearningSessionData(
                1L, 1L, "Java Basics Workshop",
                "Introduction to Java programming",
                toDate(now.minusDays(35)), 90, SessionType.SCHEDULED,
                15, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/java-basic-001"
        ));

        sessions.add(new LearningSessionData(
                1L, 2L, "Python for Beginners",
                "Start your Python programming journey",
                toDate(now.minusDays(25)), 120, SessionType.SCHEDULED,
                15, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/python-beg-002"
        ));

        sessions.add(new LearningSessionData(
                3L, 3L, "JavaScript Fundamentals",
                "Learn JavaScript from scratch",
                toDate(now.minusDays(18)), 90, SessionType.SCHEDULED,
                12, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/js-fund-003"
        ));

        sessions.add(new LearningSessionData(
                1L, 1L, "Advanced Java Programming",
                "Deep dive into Java frameworks",
                toDate(now.plusDays(3)), 120, SessionType.SCHEDULED,
                10, true, new BigDecimal("10.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/java-adv-004"
        ));

        sessions.add(new LearningSessionData(
                3L, 4L, "React Development",
                "Building modern web apps with React",
                toDate(now.plusDays(6)), 150, SessionType.SCHEDULED,
                12, true, new BigDecimal("15.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/react-dev-005"
        ));

        sessions.add(new LearningSessionData(
                1L, 2L, "Python Advanced Topics",
                "Advanced Python programming concepts",
                toDate(now.plusDays(10)), 120, SessionType.SCHEDULED,
                10, false, BigDecimal.ZERO, "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/python-adv-006"
        ));

        sessions.add(new LearningSessionData(
                3L, 5L, "Angular Framework",
                "Complete guide to Angular development",
                toDate(now.plusDays(14)), 180, SessionType.SCHEDULED,
                15, true, new BigDecimal("20.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/angular-guide-007"
        ));

        sessions.add(new LearningSessionData(
                1L, 1L, "Java Spring Boot",
                "Building REST APIs with Spring Boot",
                toDate(now.plusDays(18)), 150, SessionType.SCHEDULED,
                12, true, new BigDecimal("18.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/spring-boot-008"
        ));

        sessions.add(new LearningSessionData(
                9L, 16L, "Guitar for Beginners",
                "Learn to play guitar from scratch",
                toDate(now.plusDays(2)), 60, SessionType.SCHEDULED,
                5, false, BigDecimal.ZERO, "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/stu-vwxy-zab"
        ));

        sessions.add(new LearningSessionData(
                12L, 21L, "Data Science Fundamentals",
                "Introduction to data analysis and statistical methods",
                toDate(now.minusDays(50)), 120, SessionType.SCHEDULED,
                15, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-ds-101"
        ));

        sessions.add(new LearningSessionData(
                12L, 21L, "Python for Data Analysis",
                "Using pandas and numpy for data manipulation",
                toDate(now.minusDays(48)), 150, SessionType.SCHEDULED,
                12, true, new BigDecimal("15.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-py-102"
        ));

        sessions.add(new LearningSessionData(
                12L, 21L, "Machine Learning Basics",
                "Introduction to supervised and unsupervised learning",
                toDate(now.minusDays(45)), 180, SessionType.SCHEDULED,
                10, true, new BigDecimal("20.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-ml-103"
        ));

        sessions.add(new LearningSessionData(
                12L, 21L, "Data Visualization with Python",
                "Creating insightful visualizations using matplotlib and seaborn",
                toDate(now.minusDays(42)), 120, SessionType.SCHEDULED,
                15, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-viz-104"
        ));

        sessions.add(new LearningSessionData(
                12L, 21L, "SQL for Data Scientists",
                "Advanced SQL queries and database optimization",
                toDate(now.minusDays(38)), 150, SessionType.SCHEDULED,
                12, true, new BigDecimal("12.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-sql-105"
        ));

        sessions.add(new LearningSessionData(
                12L, 21L, "Statistical Analysis Workshop",
                "Hypothesis testing and statistical inference",
                toDate(now.minusDays(35)), 120, SessionType.SCHEDULED,
                10, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-stats-106"
        ));

        sessions.add(new LearningSessionData(
                12L, 21L, "Deep Learning Introduction",
                "Neural networks and deep learning fundamentals",
                toDate(now.minusDays(30)), 180, SessionType.SCHEDULED,
                8, true, new BigDecimal("25.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-dl-107"
        ));

        sessions.add(new LearningSessionData(
                12L, 21L, "Time Series Analysis",
                "Forecasting and time series modeling",
                toDate(now.minusDays(25)), 120, SessionType.SCHEDULED,
                12, true, new BigDecimal("18.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-ts-108"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Japanese Calligraphy Basics",
                "Introduction to traditional Japanese brush writing",
                toDate(now.minusDays(55)), 120, SessionType.SCHEDULED,
                8, false, BigDecimal.ZERO, "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-cal-201"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Advanced Calligraphy Techniques",
                "Mastering different calligraphy styles",
                toDate(now.minusDays(52)), 150, SessionType.SCHEDULED,
                8, true, new BigDecimal("20.00"), "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-cal-202"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Ink Painting Workshop",
                "Traditional sumi-e painting techniques",
                toDate(now.minusDays(48)), 180, SessionType.SCHEDULED,
                6, true, new BigDecimal("25.00"), "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-ink-203"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Japanese Art History",
                "Understanding the evolution of Japanese art",
                toDate(now.minusDays(45)), 120, SessionType.SCHEDULED,
                10, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-hist-204"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Origami and Paper Arts",
                "Traditional Japanese paper folding techniques",
                toDate(now.minusDays(42)), 90, SessionType.SCHEDULED,
                12, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-orig-205"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Ikebana - Flower Arrangement",
                "Japanese art of flower arrangement",
                toDate(now.minusDays(38)), 120, SessionType.SCHEDULED,
                8, true, new BigDecimal("15.00"), "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-ike-206"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Tea Ceremony Introduction",
                "Understanding the Japanese tea ceremony tradition",
                toDate(now.minusDays(35)), 150, SessionType.SCHEDULED,
                10, true, new BigDecimal("22.00"), "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-tea-207"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Zen Meditation and Art",
                "Combining meditation with artistic expression",
                toDate(now.minusDays(32)), 120, SessionType.SCHEDULED,
                12, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-zen-208"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Modern Japanese Design",
                "Contemporary applications of traditional aesthetics",
                toDate(now.minusDays(28)), 150, SessionType.SCHEDULED,
                10, true, new BigDecimal("18.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-mod-209"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Calligraphy Masterclass",
                "Advanced techniques for experienced practitioners",
                toDate(now.minusDays(25)), 180, SessionType.SCHEDULED,
                6, true, new BigDecimal("30.00"), "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-master-210"
        ));

        sessions.add(new LearningSessionData(
                9L, 18L, "Japanese Aesthetics Philosophy",
                "Wabi-sabi and other aesthetic principles",
                toDate(now.minusDays(20)), 120, SessionType.SCHEDULED,
                15, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-aes-211"
        ));


        return sessions;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    //#endregion

    //#region Inner Class
    private static class LearningSessionData {
        Long instructorId;
        Long skillId;
        String title;
        String description;
        Date scheduledDatetime;
        Integer durationMinutes;
        SessionType type;
        Integer maxCapacity;
        Boolean isPremium;
        BigDecimal skillcoinsCost;
        String language;
        SessionStatus status;
        String videoCallLink;

        LearningSessionData(Long instructorId, Long skillId, String title, String description,
                            Date scheduledDatetime, Integer durationMinutes, SessionType type,
                            Integer maxCapacity, Boolean isPremium, BigDecimal skillcoinsCost,
                            String language, SessionStatus status, String videoCallLink) {
            this.instructorId = instructorId;
            this.skillId = skillId;
            this.title = title;
            this.description = description;
            this.scheduledDatetime = scheduledDatetime;
            this.durationMinutes = durationMinutes;
            this.type = type;
            this.maxCapacity = maxCapacity;
            this.isPremium = isPremium;
            this.skillcoinsCost = skillcoinsCost;
            this.language = language;
            this.status = status;
            this.videoCallLink = videoCallLink;
        }
    }
    //#endregion
}