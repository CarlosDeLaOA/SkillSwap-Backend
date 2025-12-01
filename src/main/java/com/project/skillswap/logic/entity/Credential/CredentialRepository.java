package com.project.skillswap.logic.entity.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Notification.CredentialAlertDTO;
import com.project.skillswap.logic.entity.Skill.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio para gestión de credenciales
 */
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

    /**
     * Cuenta el número de credenciales de un learner para una habilidad específica
     *
     * @param learner el aprendiz
     * @param skill la habilidad
     * @return número de credenciales
     */
    @Query("SELECT COUNT(c) FROM Credential c WHERE c.learner = :learner AND c.skill = :skill")
    long countByLearnerAndSkill(@Param("learner") Learner learner, @Param("skill") Skill skill);

    /**
     * Verifica si ya existe una credencial para un quiz específico
     *
     * @param quiz el cuestionario
     * @return true si existe la credencial
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Credential c WHERE c.quiz = :quiz")
    boolean existsByQuiz(@Param("quiz") com.project.skillswap.logic.entity.Quiz.Quiz quiz);

    /**
     * Obtiene todas las skills únicas en las que un learner tiene credenciales
     *
     * @param learner el aprendiz
     * @return lista de skills con credenciales
     */
    @Query("SELECT DISTINCT c.skill FROM Credential c WHERE c.learner = :learner")
    List<Skill> findDistinctSkillsByLearner(@Param("learner") Learner learner);

}