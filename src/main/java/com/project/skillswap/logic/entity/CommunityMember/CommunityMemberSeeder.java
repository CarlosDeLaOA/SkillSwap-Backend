package com.project.skillswap.logic.entity.CommunityMember;

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
        List<Learner> learners = learnerRepository.findAll();

        if (communities.isEmpty() || learners.isEmpty()) {
            logger.warn("No hay comunidades o learners para crear membresías");
            return;
        }

        int totalMembers = 0;

        for (LearningCommunity community : communities) {
            // El creador siempre es miembro con rol CREATOR
            CommunityMember creatorMember = new CommunityMember();
            creatorMember.setLearningCommunity(community);
            creatorMember.setLearner(community.getCreator());
            creatorMember.setRole(MemberRole.CREATOR);
            creatorMember.setActive(true);

            // Fecha de unión = fecha de creación de la comunidad
            creatorMember.setJoinDate(community.getCreationDate());

            communityMemberRepository.save(creatorMember);
            totalMembers++;

            // Agregar miembros adicionales (entre 3 y maxMembers-1)
            int membersToAdd = 3 + random.nextInt(Math.max(1, community.getMaxMembers() - 4));

            // Mezclar learners para selección aleatoria
            List<Learner> availableLearners = new ArrayList<>(learners);
            Collections.shuffle(availableLearners);

            int addedMembers = 0;
            for (Learner learner : availableLearners) {
                // No agregar al creador de nuevo
                if (learner.getId().equals(community.getCreator().getId())) {
                    continue;
                }

                // Verificar que no esté ya en la comunidad
                boolean alreadyMember = communityMemberRepository.findAll().stream()
                        .anyMatch(m -> m.getLearningCommunity().getId().equals(community.getId())
                                && m.getLearner().getId().equals(learner.getId()));

                if (alreadyMember) {
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
                joinCal.add(Calendar.DAY_OF_MONTH, random.nextInt(30) + 1); // 1-30 días después
                member.setJoinDate(joinCal.getTime());

                communityMemberRepository.save(member);
                addedMembers++;
                totalMembers++;

                if (addedMembers >= membersToAdd) {
                    break;
                }
            }
        }

        logger.info("CommunityMemberSeeder: " + totalMembers + " membresías creadas");
    }
}