package com.project.skillswap.logic.entity.LearningCommunity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningCommunityRepository extends JpaRepository<LearningCommunity, Long> {

    /**
     * Obtiene todas las comunidades donde el learner es miembro activo
     */
    @Query("SELECT DISTINCT cm.learningCommunity FROM CommunityMember cm " +
            "WHERE cm.learner.id = :learnerId " +
            "AND cm.active = true " +
            "AND cm.learningCommunity.active = true")
    List<LearningCommunity> findCommunitiesByLearnerId(@Param("learnerId") Long learnerId);

    /**
     * Obtiene comunidades del learner con máximo N miembros activos
     */
    @Query("SELECT lc FROM LearningCommunity lc " +
            "JOIN lc.members cm " +
            "WHERE cm.learner.id = :learnerId " +
            "AND cm.active = true " +
            "AND lc.active = true " +
            "AND (SELECT COUNT(cm2) FROM CommunityMember cm2 " +
            "     WHERE cm2.learningCommunity.id = lc.id AND cm2.active = true) <= :maxMembers")
    List<LearningCommunity> findCommunitiesByLearnerIdWithMaxMembers(
            @Param("learnerId") Long learnerId,
            @Param("maxMembers") int maxMembers
    );

    /**
     * Busca una comunidad por su código de invitación.
     */
    Optional<LearningCommunity> findByInvitationCode(String invitationCode);

    /**
     * Verifica si existe una comunidad con el código de invitación especificado.
     */
    boolean existsByInvitationCode(String invitationCode);

    /**
     * Busca todas las comunidades activas creadas por un learner específico.
     */
    @Query("SELECT lc FROM LearningCommunity lc " +
            "WHERE lc.creator.id = :creatorId AND lc.active = true")
    List<LearningCommunity> findActiveCommunitiesByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * Busca todas las comunidades activas.
     */
    List<LearningCommunity> findByActiveTrue();
}
