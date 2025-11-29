package com.project.skillswap.logic.entity.Credential;

import com.project.skillswap.logic.entity.Notification.CredentialAlertDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    /**
     * Obtiene todas las credenciales de los miembros de una comunidad específica
     * con datos completos del learner, person, skill y knowledge area
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


    /**
     * Encuentra learners que tienen 8 o 9 credenciales de un mismo skill
     * y que NO han recibido alerta previa (verificando en notifications)
     */
    /**
     * Query para TESTING - Sin validar notificaciones previas
     */
    @Query("""
    SELECT new com.project.skillswap.logic.entity.Notification.CredentialAlertDTO(
        l.id,
        p.fullName,
        p.email,
        s.id,
        s.name,
        COUNT(c.id)
    )
    FROM Credential c
    JOIN c.learner l
    JOIN l.person p
    JOIN c.skill s
    WHERE p.active = true
    GROUP BY l.id, p.fullName, p.email, s.id, s.name
    HAVING COUNT(c.id) >= 8 AND COUNT(c.id) < 10
    ORDER BY p.fullName, s.name
""")
    List<CredentialAlertDTO> findLearnersCloseToAchievingCertificate();
}