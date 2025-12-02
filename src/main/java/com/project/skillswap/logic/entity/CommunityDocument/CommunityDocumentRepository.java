package com.project.skillswap.logic.entity.CommunityDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityDocumentRepository extends JpaRepository<CommunityDocument, Integer> {

    /**
     * Obtiene todos los documentos activos de una comunidad ordenados por fecha descendente
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.learningCommunity.id=:communityId AND cd.isDeleted=false ORDER BY cd.uploadDate DESC")
    List<CommunityDocument> findByCommunityId(@Param("communityId") Long communityId);

    /**
     * Obtiene documentos borrados de una comunidad ordenados por fecha de eliminación descendente
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.learningCommunity.id=:communityId AND cd.isDeleted=true ORDER BY cd.deletedAt DESC")
    List<CommunityDocument> findDeletedByCommunityId(@Param("communityId") Long communityId);

    /**
     * Busca un documento activo por ID
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.id=:documentId AND cd.isDeleted=false")
    Optional<CommunityDocument> findActiveById(@Param("documentId") Integer documentId);
}