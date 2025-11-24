package com.project.skillswap.logic.entity.CommunityMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, Long> {

    /**
     * Obtiene todos los miembros activos de una comunidad
     */
    @Query("SELECT cm FROM CommunityMember cm " +
            "WHERE cm.learningCommunity.id = :communityId AND cm.active = true")
    List<CommunityMember> findActiveMembersByCommunityId(@Param("communityId") Long communityId);

    /**
     * Cuenta miembros activos de una comunidad
     */
    @Query("SELECT COUNT(cm) FROM CommunityMember cm " +
            "WHERE cm.learningCommunity.id = :communityId AND cm.active = true")
    long countActiveMembersByCommunityId(@Param("communityId") Long communityId);

    /**
     * Busca todas las membresías activas de un learner específico.
     * Útil para verificar si un learner ya pertenece a una o varias comunidades.
     */
    List<CommunityMember> findByLearnerIdAndActiveTrue(Long learnerId);

    /**
     * Busca un miembro activo de una comunidad por IDs de comunidad y learner.
     */
    @Query("SELECT cm FROM CommunityMember cm " +
            "WHERE cm.learningCommunity.id = :communityId " +
            "AND cm.learner.id = :learnerId " +
            "AND cm.active = true")
    Optional<CommunityMember> findActiveMemberByCommunityIdAndLearnerId(
            @Param("communityId") Long communityId,
            @Param("learnerId") Long learnerId
    );

    /**
     * Verifica si un learner es miembro activo de alguna comunidad.
     */
    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
            "FROM CommunityMember cm " +
            "WHERE cm.learner.id = :learnerId AND cm.active = true")
    boolean existsByLearnerIdAndActiveTrue(@Param("learnerId") Long learnerId);

    @Query("SELECT cm FROM CommunityMember cm WHERE cm.learner.person.id = :personId AND cm.active = true")
    List<CommunityMember> findActiveMembersByPersonId(@Param("personId") Long personId);

}
