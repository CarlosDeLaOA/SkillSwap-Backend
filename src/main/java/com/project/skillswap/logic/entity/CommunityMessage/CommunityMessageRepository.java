package com.project.skillswap.logic.entity.CommunityMessage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestionar mensajes de comunidades.
 */
@Repository
public interface CommunityMessageRepository extends JpaRepository<CommunityMessage, Long> {

    /**
     * Obtiene todos los mensajes de una comunidad ordenados por fecha.
     *
     * @param communityId ID de la comunidad
     * @return lista de mensajes ordenados por fecha ascendente
     */
    @Query("SELECT cm FROM CommunityMessage cm " +
            "WHERE cm.learningCommunity.id = :communityId " +
            "ORDER BY cm.sentDate ASC")
    List<CommunityMessage> findByCommunityIdOrderBySentDateAsc(@Param("communityId") Long communityId);

    /**
     * Obtiene los últimos N mensajes de una comunidad.
     *
     * @param communityId ID de la comunidad
     * @param limit número máximo de mensajes a obtener
     * @return lista de los últimos mensajes
     */
    @Query(value = "SELECT * FROM community_message " +
            "WHERE learning_community_id = :communityId " +
            "ORDER BY sent_date DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<CommunityMessage> findLastMessagesByCommunityId(@Param("communityId") Long communityId,
                                                         @Param("limit") int limit);

    /**
     * Cuenta el número total de mensajes en una comunidad.
     *
     * @param communityId ID de la comunidad
     * @return número total de mensajes
     */
    @Query("SELECT COUNT(cm) FROM CommunityMessage cm " +
            "WHERE cm.learningCommunity.id = :communityId")
    long countMessagesByCommunityId(@Param("communityId") Long communityId);
}