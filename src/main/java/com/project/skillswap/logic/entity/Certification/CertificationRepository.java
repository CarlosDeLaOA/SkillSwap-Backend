package com.project.skillswap.logic.entity.Certification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Skill.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio para gestión de certificaciones
 */
public interface CertificationRepository extends JpaRepository<Certification, Long> {

    /**
     * Verifica si existe una certificación para un learner y skill específicos
     *
     * @param learner el aprendiz
     * @param skill la habilidad
     * @return true si existe la certificación
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Certification c " +
            "WHERE c.learner = :learner AND c.skill = :skill")
    boolean existsByLearnerAndSkill(@Param("learner") Learner learner, @Param("skill") Skill skill);
}