package com.project.skillswap.logic.entity.CommunityDocument;

import org.springframework.data.jpa. repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org. springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityDocumentRepository extends JpaRepository<CommunityDocument, Integer> {

    /**
     * Obtiene todos los documentos de una comunidad ordenados por fecha descendente
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.learningCommunity.id=:communityId ORDER BY cd. uploadDate DESC")
    List<CommunityDocument> findByCommunityId(@Param("communityId") Long communityId);

    /**
     * Obtiene documentos borrados de una comunidad
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd. learningCommunity.id=:communityId")
    List<CommunityDocument> findDeletedByCommunityId(@Param("communityId") Long communityId);
}