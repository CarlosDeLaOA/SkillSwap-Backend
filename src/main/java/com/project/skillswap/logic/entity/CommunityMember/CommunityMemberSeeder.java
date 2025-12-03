package com.project.skillswap.logic.entity.CommunityMember;

import jakarta.transaction.Transactional;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;
import java.util.*;

@Order(12)
@Component
public class CommunityMemberSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CommunityMemberSeeder.class);

    private final CommunityMemberRepository communityMemberRepository;
    private final LearningCommunityRepository learningCommunityRepository;
    private final LearnerRepository learnerRepository;
    private final Random random = new Random();

    public CommunityMemberSeeder(CommunityMemberRepository communityMemberRepository,
                                 LearningCommunityRepository learningCommunityRepository,
                                 LearnerRepository learnerRepository) {
        this.communityMemberRepository = communityMemberRepository;
        this.learningCommunityRepository = learningCommunityRepository;
        this.learnerRepository = learnerRepository;
    }
    @Transactional
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (communityMemberRepository.count() > 0) {
            logger.info("CommunityMemberSeeder: Ya existen miembros, omitiendo seed");
            return;
        }
        this.seedCommunityMembers();
    }

    private void seedCommunityMembers() {
        List<LearningCommunity> communities = learningCommunityRepository.findAll();
        List<Learner> allLearners = learnerRepository.findAll();

        if (communities.isEmpty() || allLearners.isEmpty()) {
            logger.warn("No hay comunidades o learners para crear membresías");
            return;
        }

        // SET para rastrear learners que ya tienen comunidad
        Set<Long> learnersWithCommunity = new HashSet<>();

        // Identificar learners específicos por email
        Learner sammyToruno = findLearnerByEmail(allLearners, "storunos@ucenfotec.ac.cr");
        Learner camilaMorales = findLearnerByEmail(allLearners, "cmoralesso@ucenfotec.ac.cr");
        Learner miaSolano = findLearnerByEmail(allLearners, "moralescamila500@outlook.com");

        // Lista de learners disponibles
        List<Learner> availableLearners = new ArrayList<>(allLearners);

        // EXCLUIR a Sammy Toruño de las comunidades
        if (sammyToruno != null) {
            availableLearners.remove(sammyToruno);
            logger.info("Sammy Toruño será EXCLUIDA de todas las comunidades");
        }

        Collections.shuffle(availableLearners);

        int totalMembers = 0;
        boolean camilaAndMiaAssigned = false;

        for (LearningCommunity community : communities) {
            // El creador siempre es miembro con rol CREATOR
            CommunityMember creatorMember = new CommunityMember();
            creatorMember.setLearningCommunity(community);
            creatorMember.setLearner(community.getCreator());
            creatorMember.setRole(MemberRole.CREATOR);
            creatorMember.setActive(true);
            creatorMember.setJoinDate(community.getCreationDate());

            communityMemberRepository.save(creatorMember);
            learnersWithCommunity.add(community.getCreator().getId());
            totalMembers++;

            // EN LA PRIMERA COMUNIDAD DISPONIBLE: agregar a Camila y Mia juntas
            if (!camilaAndMiaAssigned && camilaMorales != null && miaSolano != null) {
                // Verificar que ninguna sea la creadora
                boolean camilaIsCreator = camilaMorales.getId().equals(community.getCreator().getId());
                boolean miaIsCreator = miaSolano.getId().equals(community.getCreator().getId());

                if (!camilaIsCreator) {
                    CommunityMember camilaMember = new CommunityMember();
                    camilaMember.setLearningCommunity(community);
                    camilaMember.setLearner(camilaMorales);
                    camilaMember.setRole(MemberRole.MEMBER);
                    camilaMember.setActive(true);

                    Calendar joinCal = Calendar.getInstance();
                    joinCal.setTime(community.getCreationDate());
                    joinCal.add(Calendar.DAY_OF_MONTH, random.nextInt(5) + 1);
                    camilaMember.setJoinDate(joinCal.getTime());

                    communityMemberRepository.save(camilaMember);
                    learnersWithCommunity.add(camilaMorales.getId());
                    totalMembers++;
                    logger.info("Camila Morales agregada a: " + community.getName());
                }

                if (!miaIsCreator) {
                    CommunityMember miaMember = new CommunityMember();
                    miaMember.setLearningCommunity(community);
                    miaMember.setLearner(miaSolano);
                    miaMember.setRole(MemberRole.MEMBER);
                    miaMember.setActive(true);

                    Calendar joinCal = Calendar.getInstance();
                    joinCal.setTime(community.getCreationDate());
                    joinCal.add(Calendar.DAY_OF_MONTH, random.nextInt(5) + 2);
                    miaMember.setJoinDate(joinCal.getTime());

                    communityMemberRepository.save(miaMember);
                    learnersWithCommunity.add(miaSolano.getId());
                    totalMembers++;
                    logger.info("Mia Solano agregada a: " + community.getName());
                }

                camilaAndMiaAssigned = true;
            }

            int membersToAdd = 3 + random.nextInt(7); // 3-9 miembros adicionales
            int addedMembers = 0;

            for (Learner learner : availableLearners) {
                // Verificar que no sea el creador
                if (learner.getId().equals(community.getCreator().getId())) {
                    continue;
                }

                // Verificar que NO esté ya en otra comunidad
                if (learnersWithCommunity.contains(learner.getId())) {
                    continue;
                }

                CommunityMember member = new CommunityMember();
                member.setLearningCommunity(community);
                member.setLearner(learner);
                member.setRole(MemberRole.MEMBER);
                member.setActive(true);

                // Fecha de unión (después de la creación de la comunidad)
                Calendar joinCal = Calendar.getInstance();
                joinCal.setTime(community.getCreationDate());
                joinCal.add(Calendar.DAY_OF_MONTH, random.nextInt(30) + 1);
                member.setJoinDate(joinCal.getTime());

                communityMemberRepository.save(member);
                learnersWithCommunity.add(learner.getId());
                addedMembers++;
                totalMembers++;

                if (addedMembers >= membersToAdd) {
                    break;
                }
            }
        }

        logger.info("CommunityMemberSeeder: " + totalMembers + " membresías creadas");
        if (sammyToruno != null) {
            logger.info("✓ Sammy Toruño NO tiene comunidad (ID: " + sammyToruno.getId() + ")");
        }
        if (camilaAndMiaAssigned) {
            logger.info("✓ Camila Morales y Mia Solano están en la MISMA comunidad");
        }
    }

    private Learner findLearnerByEmail(List<Learner> learners, String email) {
        return learners.stream()
                .filter(l -> l.getPerson().getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }
}