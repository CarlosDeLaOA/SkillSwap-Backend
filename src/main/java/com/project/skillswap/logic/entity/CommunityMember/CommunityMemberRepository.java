package com.project.skillswap.logic.entity.CommunityMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}