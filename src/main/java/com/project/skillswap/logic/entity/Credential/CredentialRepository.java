package com.project.skillswap.logic.entity.Credential;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    /**
     * Obtiene todas las credenciales de los miembros de una comunidad específica
     * con datos completos del learner, person, skill y knowledge area
     *
     * @param communityId ID de la comunidad
     * @return Lista de credenciales con datos cargados
     */
    @Query("SELECT DISTINCT c FROM Credential c " +
            "JOIN FETCH c.learner l " +
            "JOIN FETCH l.person p " +
            "JOIN FETCH c.skill s " +
            "JOIN FETCH s.knowledgeArea ka " +
            "JOIN CommunityMember cm ON cm.learner.id = l.id " +
            "WHERE cm.learningCommunity.id = :communityId " +
            "ORDER BY c.obtainedDate DESC")
    List<Credential> findCredentialsByCommunityId(@Param("communityId") Long communityId);
}