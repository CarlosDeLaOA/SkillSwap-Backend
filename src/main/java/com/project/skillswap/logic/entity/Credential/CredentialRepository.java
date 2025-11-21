package com.project.skillswap.logic.entity.Credential;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    /**
     * Obtiene todas las credenciales de los miembros de una comunidad específica
     *
     * @param communityId ID de la comunidad
     * @return Lista de credenciales obtenidas por miembros de la comunidad
     */
    @Query("SELECT c FROM Credential c " +
            "JOIN c.learner l " +
            "JOIN CommunityMember cm ON cm.learner.id = l.id " +
            "WHERE cm.learningCommunity.id = :communityId " +
            "ORDER BY c.obtainedDate DESC")
    List<Credential> findCredentialsByCommunityId(@Param("communityId") Long communityId);
}