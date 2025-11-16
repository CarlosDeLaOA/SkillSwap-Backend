package com.project.skillswap.logic.entity.LearningCommunity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LearningCommunityRepository extends JpaRepository<LearningCommunity, Long> {  // 👈 Cambiar Integer a Long

    /**
     * Obtiene todas las comunidades donde el learner es miembro activo
     */
    @Query("SELECT DISTINCT cm.learningCommunity FROM CommunityMember cm " +
            "WHERE cm.learner.id = :learnerId AND cm.active = true AND cm.learningCommunity.active = true")
    List<LearningCommunity> findCommunitiesByLearnerId(@Param("learnerId") Long learnerId);

    /**
     * Obtiene comunidades del learner con máximo N miembros activos
     */
    @Query("SELECT lc FROM LearningCommunity lc " +
            "JOIN lc.members cm " +
            "WHERE cm.learner.id = :learnerId " +
            "AND cm.active = true " +
            "AND lc.active = true " +
            "AND (SELECT COUNT(cm2) FROM CommunityMember cm2 WHERE cm2.learningCommunity.id = lc.id AND cm2.active = true) <= :maxMembers")
    List<LearningCommunity> findCommunitiesByLearnerIdWithMaxMembers(@Param("learnerId") Long learnerId,
                                                                     @Param("maxMembers") int maxMembers);
}