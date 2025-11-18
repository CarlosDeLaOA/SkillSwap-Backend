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

        sessions.add(new LearningSessionData(1L, 12L, "Español Conversacional Básico",
                "Mejora tu fluidez hablando sobre temas cotidianos: saludos, compras, viajes.",
                toDate(now.minusDays(40)), 90, SessionType.SCHEDULED, 12, false, BigDecimal.ZERO, "es", SessionStatus.FINISHED,
                "https://meet.google.com/maria-esp-001"));
        sessions.add(new LearningSessionData(1L, 12L, "Gramática Española Intermedia",
                "Domina el uso del subjuntivo, tiempos compuestos y conectores.",
                toDate(now.minusDays(30)), 120, SessionType.SCHEDULED, 10, true, new BigDecimal("8.00"), "es", SessionStatus.FINISHED,
                "https://meet.google.com/maria-gram-002"));

        sessions.add(new LearningSessionData(3L, 2L, "Python desde Cero",
                "Aprende variables, funciones, listas y POO con proyectos reales.",
                toDate(now.minusDays(25)), 150, SessionType.SCHEDULED, 15, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/sophie-py-001"));
        sessions.add(new LearningSessionData(3L, 5L, "React Avanzado: Hooks y Context",
                "Crea apps dinámicas con useState, useEffect y useContext.",
                toDate(now.plusDays(3)), 180, SessionType.IMMEDIATE, 8, true, new BigDecimal("18.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/sophie-react-002"));

        sessions.add(new LearningSessionData(9L, 18L, "Caligrafía Japonesa para Principiantes",
                "Aprende hiragana y katakana con pincel tradicional. Incluye materiales.",
                toDate(now.minusDays(50)), 120, SessionType.SCHEDULED, 8, false, BigDecimal.ZERO, "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-cal-201"));
        sessions.add(new LearningSessionData(9L, 18L, "Sumi-e: Pintura con Tinta",
                "Técnicas de pincel para paisajes y naturaleza. Filosofía zen incluida.",
                toDate(now.minusDays(45)), 150, SessionType.SCHEDULED, 6, true, new BigDecimal("22.00"), "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-ink-203"));

        sessions.add(new LearningSessionData(9L, 21L, "Machine Learning con Python",
                "Regresión, clasificación y clustering con scikit-learn.",
                toDate(now.minusDays(35)), 180, SessionType.SCHEDULED, 12, true, new BigDecimal("25.00"), "es", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-ml-103"));
        sessions.add(new LearningSessionData(9L, 21L, "Deep Learning con TensorFlow",
                "Redes neuronales, CNN y RNN. Proyecto final incluido.",
                toDate(now.plusDays(7)), 240, SessionType.SCHEDULED, 10, true, new BigDecimal("35.00"), "es", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-dl-108"));

        sessions.add(new LearningSessionData(9L, 16L, "Guitarra para Principiantes",
                "Acordes básicos, ritmo y canciones populares.",
                toDate(now.minusDays(20)), 90, SessionType.SCHEDULED, 5, false, BigDecimal.ZERO, "pt", SessionStatus.FINISHED,
                "https://meet.google.com/lucas-guitar-001"));

        sessions.add(new LearningSessionData(6L, 8L, "SQL Avanzado: Joins y Subqueries",
                "Optimiza consultas complejas y mejora rendimiento.",
                toDate(now.minusDays(15)), 120, SessionType.SCHEDULED, 15, true, new BigDecimal("15.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/ahmed-sql-001"));

        sessions.add(new LearningSessionData(1L, 12L, "Español para Viajes",
                "Vocabulario útil en aeropuertos, hoteles y restaurantes.",
                toDate(now.minusDays(28)), 75, SessionType.SCHEDULED, 15, false, BigDecimal.ZERO, "es", SessionStatus.FINISHED,
                "https://meet.google.com/maria-viajes-003"));
        sessions.add(new LearningSessionData(9L, 12L, "Español de Negocios",
                "Redacción de correos, presentaciones y reuniones formales.",
                toDate(now.minusDays(22)), 120, SessionType.SCHEDULED, 8, true, new BigDecimal("12.00"), "es", SessionStatus.FINISHED,
                "https://meet.google.com/maria-negocios-004"));

        sessions.add(new LearningSessionData(3L, 3L, "JavaScript Moderno: ES6+",
                "Arrow functions, destructuring, async/await y módulos.",
                toDate(now.minusDays(18)), 150, SessionType.SCHEDULED, 12, true, new BigDecimal("15.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/sophie-js-003"));
        sessions.add(new LearningSessionData(9L, 3L, "DOM y Eventos Avanzados",
                "Manipulación avanzada del DOM y delegación de eventos.",
                toDate(now.plusDays(5)), 120, SessionType.SCHEDULED, 10, true, new BigDecimal("12.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/sophie-dom-004"));

        sessions.add(new LearningSessionData(9L, 18L, "Origami Avanzado",
                "Figuras complejas: grullas, dragones y flores.",
                toDate(now.minusDays(38)), 90, SessionType.SCHEDULED, 6, true, new BigDecimal("18.00"), "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-orig-205"));
        sessions.add(new LearningSessionData(9L, 18L, "Ikebana Moderna",
                "Arreglos florales contemporáneos con enfoque minimalista.",
                toDate(now.minusDays(32)), 120, SessionType.SCHEDULED, 8, true, new BigDecimal("20.00"), "ja", SessionStatus.FINISHED,
                "https://meet.google.com/yuki-ike-206"));

        sessions.add(new LearningSessionData(9L, 21L, "Análisis de Datos con Pandas",
                "Limpieza, transformación y visualización de datos.",
                toDate(now.minusDays(26)), 180, SessionType.SCHEDULED, 15, true, new BigDecimal("22.00"), "es", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-pandas-109"));
        sessions.add(new LearningSessionData(12L, 21L, "Visualización con Seaborn",
                "Gráficos avanzados: heatmaps, pairplots y más.",
                toDate(now.plusDays(10)), 120, SessionType.SCHEDULED, 12, true, new BigDecimal("18.00"), "es", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-seaborn-110"));

        sessions.add(new LearningSessionData(4L, 16L, "Guitarra Eléctrica: Riffs y Solos",
                "Técnicas de bending, tapping y escalas pentatónicas.",
                toDate(now.minusDays(15)), 90, SessionType.SCHEDULED, 5, true, new BigDecimal("15.00"), "pt", SessionStatus.FINISHED,
                "https://meet.google.com/lucas-electric-002"));
        sessions.add(new LearningSessionData(9L, 16L, "Fingerstyle Acústico",
                "Patrones rítmicos y melodías simultáneas.",
                toDate(now.plusDays(8)), 75, SessionType.SCHEDULED, 6, false, BigDecimal.ZERO, "pt", SessionStatus.SCHEDULED,
                "https://meet.google.com/lucas-finger-003"));

        sessions.add(new LearningSessionData(6L, 8L, "Optimización de Consultas SQL",
                "Índices, EXPLAIN y mejores prácticas.",
                toDate(now.minusDays(12)), 120, SessionType.SCHEDULED, 10, true, new BigDecimal("18.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/ahmed-sql-opt-002"));
        sessions.add(new LearningSessionData(6L, 8L, "SQL para Business Intelligence",
                "Reportes, KPIs y dashboards con SQL.",
                toDate(now.plusDays(12)), 150, SessionType.SCHEDULED, 12, true, new BigDecimal("20.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/ahmed-bi-003"));

        sessions.add(new LearningSessionData(17L, 7L, "Álgebra Lineal para ML",
                "Vectores, matrices, eigenvalores y aplicaciones.",
                toDate(now.minusDays(30)), 180, SessionType.SCHEDULED, 8, true, new BigDecimal("25.00"), "ru", SessionStatus.FINISHED,
                "https://meet.google.com/elena-algebra-001"));
        sessions.add(new LearningSessionData(9L, 7L, "Cálculo Diferencial",
                "Límites, derivadas y optimización.",
                toDate(now.plusDays(15)), 150, SessionType.SCHEDULED, 10, true, new BigDecimal("22.00"), "ru", SessionStatus.SCHEDULED,
                "https://meet.google.com/elena-calculo-002"));

        sessions.add(new LearningSessionData(19L, 17L, "Mandarín Básico: Pinyin y Tonos",
                "Pronunciación correcta y primeras frases.",
                toDate(now.minusDays(20)), 90, SessionType.SCHEDULED, 12, false, BigDecimal.ZERO, "zh", SessionStatus.FINISHED,
                "https://meet.google.com/li-pinyin-001"));
        sessions.add(new LearningSessionData(19L, 17L, "Conversación en Mandarín",
                "Diálogos cotidianos: compras, saludos, direcciones.",
                toDate(now.plusDays(6)), 120, SessionType.SCHEDULED, 10, true, new BigDecimal("15.00"), "zh", SessionStatus.SCHEDULED,
                "https://meet.google.com/li-conversacion-002"));

        sessions.add(new LearningSessionData(20L, 8L, "Diseño UI/UX con Figma",
                "Prototipado, wireframes y diseño de interfaces.",
                toDate(now.minusDays(14)), 180, SessionType.SCHEDULED, 15, true, new BigDecimal("20.00"), "de", SessionStatus.FINISHED,
                "https://meet.google.com/clara-figma-001"));
        sessions.add(new LearningSessionData(20L, 8L, "Branding y Identidad Visual",
                "Logotipos, paletas y guías de estilo.",
                toDate(now.plusDays(18)), 150, SessionType.SCHEDULED, 12, true, new BigDecimal("22.00"), "de", SessionStatus.SCHEDULED,
                "https://meet.google.com/clara-branding-002"));

        sessions.add(new LearningSessionData(14L, 3L, "Yoga para Principiantes",
                "Posturas básicas, respiración y relajación.",
                toDate(now.minusDays(10)), 60, SessionType.SCHEDULED, 20, false, BigDecimal.ZERO, "pt", SessionStatus.FINISHED,
                "https://meet.google.com/marco-yoga-001"));
        sessions.add(new LearningSessionData(14L, 3L, "Vinyasa Flow",
                "Secuencias dinámicas y sincronización con respiración.",
                toDate(now.plusDays(4)), 75, SessionType.SCHEDULED, 15, true, new BigDecimal("10.00"), "pt", SessionStatus.SCHEDULED,
                "https://meet.google.com/marco-vinyasa-002"));

        sessions.add(new LearningSessionData(7L, 3L, "Fotografía con Móvil",
                "Técnicas de composición, luz natural y edición en apps gratuitas.",
                toDate(now.minusDays(18)), 90, SessionType.SCHEDULED, 20, false, BigDecimal.ZERO, "es", SessionStatus.FINISHED,
                "https://meet.google.com/ana-foto-movil-050"));
        sessions.add(new LearningSessionData(7L, 3L, "Fotografía Nocturna",
                "Larga exposición, trípodes y edición de estrellas.",
                toDate(now.plusDays(9)), 120, SessionType.SCHEDULED, 10, true, new BigDecimal("18.00"), "es", SessionStatus.SCHEDULED,
                "https://meet.google.com/ana-nocturna-051"));
        sessions.add(new LearningSessionData(11L, 7L, "Microservicios con Spring Boot",
                "Patrones, API Gateway, Docker y despliegue.",
                toDate(now.minusDays(25)), 180, SessionType.IMMEDIATE, 8, true, new BigDecimal("30.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/david-micro-052"));
        sessions.add(new LearningSessionData(11L, 7L, "Clean Architecture en Java",
                "Capas, inyección de dependencias y testing.",
                toDate(now.plusDays(14)), 150, SessionType.SCHEDULED, 12, true, new BigDecimal("25.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/david-clean-053"));
        sessions.add(new LearningSessionData(10L, 1L, "Pasta Casera desde Cero",
                "Amasado, corte y salsas tradicionales.",
                toDate(now.minusDays(30)), 120, SessionType.SCHEDULED, 6, true, new BigDecimal("22.00"), "it", SessionStatus.FINISHED,
                "https://meet.google.com/isa-pasta-054"));
        sessions.add(new LearningSessionData(10L, 1L, "Pizza Napoletana Auténtica",
                "Masa de 48h, horno y técnica profesional.",
                toDate(now.plusDays(5)), 90, SessionType.SCHEDULED, 8, true, new BigDecimal("20.00"), "it", SessionStatus.SCHEDULED,
                "https://meet.google.com/isa-pizza-055"));
        sessions.add(new LearningSessionData(16L, 10L, "SEO Técnico 2025",
                "Core Web Vitals, indexación y schema.org.",
                toDate(now.minusDays(20)), 150, SessionType.SCHEDULED, 15, true, new BigDecimal("18.00"), "es", SessionStatus.FINISHED,
                "https://meet.google.com/miguel-seo-056"));
        sessions.add(new LearningSessionData(16L, 10L, "Google Ads Avanzado",
                "Campañas de remarketing y conversiones.",
                toDate(now.plusDays(11)), 120, SessionType.SCHEDULED, 12, true, new BigDecimal("15.00"), "es", SessionStatus.SCHEDULED,
                "https://meet.google.com/miguel-ads-057"));
        sessions.add(new LearningSessionData(13L, 13L, "Francés para Viajes",
                "Frases clave en aeropuertos, restaurantes y transporte.",
                toDate(now.minusDays(15)), 75, SessionType.SCHEDULED, 18, false, BigDecimal.ZERO, "fr", SessionStatus.FINISHED,
                "https://meet.google.com/natalie-viajes-058"));
        sessions.add(new LearningSessionData(13L, 13L, "Francés Comercial",
                "Redacción de emails y presentaciones formales.",
                toDate(now.plusDays(7)), 90, SessionType.SCHEDULED, 10, true, new BigDecimal("12.00"), "fr", SessionStatus.SCHEDULED,
                "https://meet.google.com/natalie-comercial-059"));
        sessions.add(new LearningSessionData(15L, 4L, "Gestión del Tiempo con GTD",
                "Método Getting Things Done aplicado a la vida real.",
                toDate(now.minusDays(22)), 90, SessionType.SCHEDULED, 20, false, BigDecimal.ZERO, "en", SessionStatus.FINISHED,
                "https://meet.google.com/sara-gtd-060"));
        sessions.add(new LearningSessionData(15L, 4L, "Hablar en Público sin Miedo",
                "Técnicas de oratoria, voz y manejo de nervios.",
                toDate(now.plusDays(16)), 120, SessionType.SCHEDULED, 15, true, new BigDecimal("15.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/sara-oratoria-061"));
        sessions.add(new LearningSessionData(21L, 10L, "Git Avanzado: Branching y CI/CD",
                "Estrategias de ramas, rebase y pipelines.",
                toDate(now.minusDays(12)), 120, SessionType.SCHEDULED, 12, true, new BigDecimal("18.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/thomas-git-062"));
        sessions.add(new LearningSessionData(21L, 10L, "GitHub Actions desde Cero",
                "Automatización de pruebas y despliegues.",
                toDate(now.plusDays(13)), 90, SessionType.SCHEDULED, 10, true, new BigDecimal("15.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/thomas-actions-063"));
        sessions.add(new LearningSessionData(8L, 2L, "Automatización con Python",
                "Scripts para tareas repetitivas: Excel, PDFs, web scraping.",
                toDate(now.minusDays(28)), 150, SessionType.SCHEDULED, 15, true, new BigDecimal("20.00"), "en", SessionStatus.FINISHED,
                "https://meet.google.com/oliver-auto-064"));
        sessions.add(new LearningSessionData(8L, 2L, "Python para Finanzas",
                "Análisis de datos financieros con pandas y yfinance.",
                toDate(now.plusDays(20)), 180, SessionType.SCHEDULED, 10, true, new BigDecimal("25.00"), "en", SessionStatus.SCHEDULED,
                "https://meet.google.com/oliver-finanzas-065"));
        sessions.add(new LearningSessionData(20L, 8L, "Tipografía y Jerarquía Visual",
                "Cómo elegir fuentes y organizar información.",
                toDate(now.minusDays(16)), 120, SessionType.SCHEDULED, 12, true, new BigDecimal("18.00"), "de", SessionStatus.FINISHED,
                "https://meet.google.com/clara-tipo-066"));
        sessions.add(new LearningSessionData(20L, 8L, "Motion Graphics con After Effects",
                "Animaciones para redes sociales y presentaciones.",
                toDate(now.plusDays(8)), 150, SessionType.SCHEDULED, 8, true, new BigDecimal("22.00"), "de", SessionStatus.SCHEDULED,
                "https://meet.google.com/clara-motion-067"));
        sessions.add(new LearningSessionData(14L, 3L, "Yoga Restaurativo",
                "Posturas pasivas con soportes para relajación profunda.",
                toDate(now.minusDays(10)), 60, SessionType.SCHEDULED, 20, false, BigDecimal.ZERO, "pt", SessionStatus.FINISHED,
                "https://meet.google.com/marco-restaurativo-068"));
        sessions.add(new LearningSessionData(14L, 3L, "Yoga para Office Workers",
                "Estiramientos en silla para aliviar tensiones diarias.",
                toDate(now.plusDays(3)), 45, SessionType.SCHEDULED, 25, false, BigDecimal.ZERO, "pt", SessionStatus.SCHEDULED,
                "https://meet.google.com/marco-oficina-069"));

        sessions.add(new LearningSessionData(9L, 2L, "Python para Data Science",
                "Pandas, NumPy, visualización de datos y análisis exploratorio.",
                toDate(now.plusDays(5)), 180, SessionType.SCHEDULED, 15, true, new BigDecimal("28.00"), "es", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-datascience-201"));

        sessions.add(new LearningSessionData(9L, 10L, "DevOps con Docker y Kubernetes",
                "Contenedores, orquestación y despliegue en la nube.",
                toDate(now.plusDays(10)), 150, SessionType.SCHEDULED, 12, true, new BigDecimal("32.00"), "es", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-devops-202"));

        sessions.add(new LearningSessionData(9L, 21L, "MLOps: ML en Producción",
                "Pipelines de ML, monitoreo de modelos y CI/CD para Data Science.",
                toDate(now.plusDays(15)), 200, SessionType.SCHEDULED, 10, true, new BigDecimal("40.00"), "es", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-mlops-203"));

        sessions.add(new LearningSessionData(9L, 3L, "JavaScript Avanzado: Async/Await",
                "Promesas, async/await, event loop y manejo de errores asíncronos.",
                toDate(now.plusDays(12)), 120, SessionType.SCHEDULED, 20, true, new BigDecimal("22.00"), "es", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-jsasync-204"));

        sessions.add(new LearningSessionData(9L, 5L, "React + TypeScript",
                "Desarrollo con React usando TypeScript para mayor robustez.",
                toDate(now.plusDays(18)), 180, SessionType.SCHEDULED, 15, true, new BigDecimal("35.00"), "es", SessionStatus.SCHEDULED,
                "https://meet.google.com/carlos-reactts-205"));

        sessions.add(new LearningSessionData(9L, 2L, "Python Intermedio: Decoradores",
                "Decoradores, generadores, context managers y programación funcional.",
                toDate(now.minusDays(45)), 150, SessionType.SCHEDULED, 15, true, new BigDecimal("25.00"), "es", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-pyinter-101"));

        sessions.add(new LearningSessionData(9L, 8L, "SQL para Análisis de Datos",
                "Window functions, CTEs, optimización de queries complejas.",
                toDate(now.minusDays(38)), 120, SessionType.SCHEDULED, 12, true, new BigDecimal("20.00"), "es", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-sqlanalysis-102"));

        sessions.add(new LearningSessionData(9L, 10L, "Git Colaborativo: Workflows",
                "Git flow, trunk-based development y resolución de conflictos.",
                toDate(now.minusDays(32)), 90, SessionType.SCHEDULED, 18, false, BigDecimal.ZERO, "es", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-gitflow-103"));

        sessions.add(new LearningSessionData(9L, 3L, "Node.js Backend con Express",
                "APIs RESTful, middleware, autenticación y conexión a bases de datos.",
                toDate(now.minusDays(28)), 180, SessionType.SCHEDULED, 12, true, new BigDecimal("30.00"), "es", SessionStatus.FINISHED,
                "https://meet.google.com/carlos-nodejs-104"));

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