package com.project.skillswap.logic.entity.LearningCommunity;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Order(11)
@Component
public class LearningCommunitySeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(LearningCommunitySeeder.class);

    private final LearningCommunityRepository learningCommunityRepository;
    private final LearnerRepository learnerRepository;
    private final Random random = new Random();

    public LearningCommunitySeeder(LearningCommunityRepository learningCommunityRepository,
                                   LearnerRepository learnerRepository) {
        this.learningCommunityRepository = learningCommunityRepository;
        this.learnerRepository = learnerRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (learningCommunityRepository.count() > 0) {
            logger.info("LearningCommunitySeeder: Ya existen comunidades, omitiendo seed");
            return;
        }
        this.seedLearningCommunities();
    }

    private void seedLearningCommunities() {
        List<Learner> learners = learnerRepository.findAll();

        if (learners.isEmpty()) {
            logger.warn("No hay learners para crear comunidades");
            return;
        }

        String[] communityNames = {
                "Expertos en Programación",
                "Amantes de la Cocina",
                "Club de Idiomas",
                "Artistas Digitales",
                "Deportistas Comprometidos",
                "Comunicadores Efectivos",
                "Innovadores Tecnológicos",
                "Maestros de las Artes",
                "Emprendedores en Acción",
                "Desarrolladores Full Stack",
                "Chefs en Formación",
                "Políglotas Apasionados",
                "Diseñadores Creativos",
                "Atletas Motivados",
                "Líderes del Futuro",
                "Pensadores Críticos",
                "Fotógrafos Profesionales",
                "Músicos Talentosos",
                "Escritores Creativos",
                "Analistas de Datos",
                "Especialistas en Marketing",
                "Arquitectos de Software",
                "Nutricionistas en Red",
                "Yoga y Bienestar",
                "Cineastas Emergentes",
                "Gamers Pro",
                "Educadores Innovadores",
                "Científicos de Datos",
                "Especialistas en IA",
                "Community Managers",
                "Diseñadores UX/UI",
                "Traders y Finanzas",
                "Terapeutas Holísticos",
                "Influencers Digitales",
                "Podcasters Creativos",
                "Blogueros Exitosos",
                "Coaches Certificados",
                "Consultores Estratégicos",
                "Ingenieros Civiles",
                "Arquitectos Modernos",
                "Psicólogos en Red",
                "Veterinarios Comprometidos",
                "Enfermeras Profesionales",
                "Abogados en Formación",
                "Contadores Expertos",
                "Economistas Analíticos",
                "Sociólogos Urbanos",
                "Antropólogos Culturales",
                "Historiadores Apasionados",
                "Filósofos Contemporáneos",
                "Matemáticos Brillantes",
                "Físicos Curiosos",
                "Químicos Innovadores",
                "Biólogos Ambientales",
                "Geólogos Exploradores",
                "Astrónomos Soñadores",
                "Oceanógrafos Aventureros",
                "Ecologistas Comprometidos",
                "Ambientalistas Activos",
                "Recicladores Responsables"
        };

        String[] descriptions = {
                "Una comunidad dedicada al aprendizaje colaborativo y el crecimiento profesional.",
                "Espacio para compartir conocimientos, experiencias y aprender juntos.",
                "Grupo de personas apasionadas por mejorar sus habilidades constantemente.",
                "Comunidad enfocada en el desarrollo de competencias y networking.",
                "Lugar de encuentro para aprender, practicar y crecer en comunidad.",
                "Red de apoyo mutuo para alcanzar objetivos de aprendizaje.",
                "Comunidad vibrante de estudiantes comprometidos con la excelencia.",
                "Espacio colaborativo para intercambiar ideas y conocimientos.",
                "Grupo dedicado a la mejora continua y el aprendizaje activo.",
                "Comunidad inclusiva para todos los niveles de experiencia."
        };

        // Crear entre 60-80 comunidades
        int communitiesToCreate = 60 + random.nextInt(21);
        List<Learner> shuffledLearners = new ArrayList<>(learners);
        Collections.shuffle(shuffledLearners);

        for (int i = 0; i < communitiesToCreate && i < shuffledLearners.size(); i++) {
            Learner creator = shuffledLearners.get(i);

            LearningCommunity community = new LearningCommunity();
            community.setCreator(creator);
            community.setName(communityNames[i % communityNames.length] +
                    (i >= communityNames.length ? " " + (i / communityNames.length + 1) : ""));
            community.setDescription(descriptions[random.nextInt(descriptions.length)]);
            community.setMaxMembers(5 + random.nextInt(11)); // 5-15 miembros
            community.setInvitationCode(generateInvitationCode());
            community.setActive(true);

            // Fecha de creación (últimos 6 meses)
            Calendar creationCal = Calendar.getInstance();
            creationCal.add(Calendar.DAY_OF_MONTH, -random.nextInt(180));
            community.setCreationDate(creationCal.getTime());

            learningCommunityRepository.save(community);
        }

        logger.info("LearningCommunitySeeder: " + communitiesToCreate + " comunidades creadas");
    }

    private String generateInvitationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}