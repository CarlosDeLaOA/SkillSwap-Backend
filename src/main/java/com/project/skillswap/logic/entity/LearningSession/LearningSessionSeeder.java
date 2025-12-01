package com.project.skillswap.logic.entity.LearningSession;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Order(5)
@Component
public class LearningSessionSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(LearningSessionSeeder.class);

    private final LearningSessionRepository learningSessionRepository;
    private final InstructorRepository instructorRepository;
    private final SkillRepository skillRepository;
    private final Random random = new Random();

    public LearningSessionSeeder(LearningSessionRepository learningSessionRepository,
                                 InstructorRepository instructorRepository,
                                 SkillRepository skillRepository) {
        this.learningSessionRepository = learningSessionRepository;
        this.instructorRepository = instructorRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (learningSessionRepository.count() > 0) {
            logger.info("LearningSessionSeeder: Ya existen sesiones, omitiendo seed");
            return;
        }
        this.seedLearningSessions();
    }

    private void seedLearningSessions() {
        List<Instructor> instructors = instructorRepository.findAll();
        List<Skill> skills = skillRepository.findAll();

        if (instructors.isEmpty() || skills.isEmpty()) {
            logger.warn("No hay instructores o skills para crear sesiones");
            return;
        }

        String[] sessionTitles = {
                "Introducción a {skill}",
                "Fundamentos de {skill}",
                "{skill} para Principiantes",
                "{skill} Nivel Intermedio",
                "{skill} Avanzado",
                "Masterclass de {skill}",
                "Taller Práctico de {skill}",
                "Técnicas Avanzadas en {skill}",
                "Domina {skill} en una Sesión",
                "Secretos y Tips de {skill}",
                "De Cero a Experto en {skill}",
                "Práctica Intensiva de {skill}",
                "Mejora tus Habilidades en {skill}",
                "{skill}: Tips y Trucos",
                "Sesión Express de {skill}"
        };

        String[] descriptions = {
                "Aprende los conceptos fundamentales y comienza tu camino en {skill}. Una sesión diseñada para principiantes.",
                "Desarrolla habilidades prácticas y aumenta tu confianza en {skill} con ejercicios guiados.",
                "Explora técnicas avanzadas y lleva tu conocimiento de {skill} al siguiente nivel.",
                "Una experiencia de aprendizaje completa que cubre desde lo básico hasta conceptos complejos.",
                "Sesión práctica donde aplicarás lo aprendido en casos reales de {skill}.",
                "Descubre los secretos que usan los expertos en {skill} para lograr resultados excepcionales.",
                "Mejora tu dominio en {skill} con ejercicios prácticos y feedback personalizado.",
                "Aprende de forma interactiva y dinámica los aspectos esenciales de {skill}.",
                "Sesión enfocada en resolver tus dudas y mejorar tus habilidades en {skill}.",
                "Conviértete en un experto en {skill} con esta sesión intensiva y práctica."
        };

        int sessionId = 1;

        // Crear sesiones pasadas (últimos 6 meses) y futuras (próximos 2 meses)
        for (Instructor instructor : instructors) {
            // Determinar cuántas sesiones crear para este instructor
            int pastSessions = 20 + random.nextInt(30); // 20-50 sesiones pasadas
            int futureSessions = 10 + random.nextInt(5); // 10-15 sesiones futuras

            // SESIONES PASADAS (FINISHED)
            for (int i = 0; i < pastSessions; i++) {
                Skill randomSkill = skills.get(random.nextInt(skills.size()));
                LearningSession session = createSession(
                        instructor,
                        randomSkill,
                        sessionTitles,
                        descriptions,
                        sessionId++,
                        true // es sesión pasada
                );
                learningSessionRepository.save(session);
            }

            // SESIONES FUTURAS (SCHEDULED/CONFIRMED)
            for (int i = 0; i < futureSessions; i++) {
                Skill randomSkill = skills.get(random.nextInt(skills.size()));
                LearningSession session = createSession(
                        instructor,
                        randomSkill,
                        sessionTitles,
                        descriptions,
                        sessionId++,
                        false // es sesión futura
                );
                learningSessionRepository.save(session);
            }
        }

        logger.info("LearningSessionSeeder: Sesiones creadas exitosamente");
    }

    private LearningSession createSession(Instructor instructor, Skill skill,
                                          String[] titles, String[] descriptions,
                                          int sessionId, boolean isPast) {
        LearningSession session = new LearningSession();

        session.setInstructor(instructor);
        session.setSkill(skill);

        // Título y descripción
        String title = titles[random.nextInt(titles.length)].replace("{skill}", skill.getName());
        String description = descriptions[random.nextInt(descriptions.length)].replace("{skill}", skill.getName());
        session.setTitle(title);
        session.setDescription(description);

        // Fecha y hora
        if (isPast) {
            // Sesiones en los últimos 6 meses
            session.setScheduledDatetime(getDateInPast());
            session.setStatus(SessionStatus.FINISHED);
        } else {
            // Sesiones futuras (próximos 2 meses)
            session.setScheduledDatetime(getDateInFuture());
            session.setStatus(random.nextBoolean() ? SessionStatus.SCHEDULED : SessionStatus.SCHEDULED);
        }

        // Duración (30, 45, 60, 90, 120 minutos)
        int[] durations = {30, 45, 60, 90, 120};
        session.setDurationMinutes(durations[random.nextInt(durations.length)]);

        // Tipo de sesión
        session.setType(SessionType.SCHEDULED);

        // Capacidad
        session.setMaxCapacity(random.nextInt(6) + 5); // 5-10 personas

        // Premium o gratuita
        boolean isPremium = random.nextDouble() < 0.3; // 30% son premium
        session.setIsPremium(isPremium);
        session.setSkillcoinsCost(isPremium ? BigDecimal.valueOf(10 + random.nextInt(40)) : BigDecimal.ZERO);

        // Idioma
        session.setLanguage("es");

        // Link de videollamada
        session.setVideoCallLink("http://localhost:4200/app/video-call/" + sessionId);

        // Fecha de creación
        Calendar creationCal = Calendar.getInstance();
        creationCal.setTime(session.getScheduledDatetime());
        creationCal.add(Calendar.DAY_OF_MONTH, -random.nextInt(7) - 1); // Creada 1-7 días antes
        session.setCreationDate(creationCal.getTime());

        return session;
    }

    private Date getDateInPast() {
        Calendar cal = Calendar.getInstance();
        // Últimos 6 meses
        int daysAgo = random.nextInt(180); // 0-180 días atrás
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);

        // Hora aleatoria entre 8:00 y 20:00
        cal.set(Calendar.HOUR_OF_DAY, 8 + random.nextInt(12));
        cal.set(Calendar.MINUTE, random.nextInt(4) * 15); // 0, 15, 30, 45
        cal.set(Calendar.SECOND, 0);

        return cal.getTime();
    }

    private Date getDateInFuture() {
        Calendar cal = Calendar.getInstance();
        // Próximos 2 meses
        int daysAhead = random.nextInt(60) + 1; // 1-60 días adelante
        cal.add(Calendar.DAY_OF_MONTH, daysAhead);

        // Hora aleatoria entre 8:00 y 20:00
        cal.set(Calendar.HOUR_OF_DAY, 8 + random.nextInt(12));
        cal.set(Calendar.MINUTE, random.nextInt(4) * 15); // 0, 15, 30, 45
        cal.set(Calendar.SECOND, 0);

        return cal.getTime();
    }
}