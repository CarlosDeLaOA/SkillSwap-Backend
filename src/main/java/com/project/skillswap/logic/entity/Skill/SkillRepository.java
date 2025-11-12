package com.project.skillswap.logic.entity.Skill;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Skill entity operations
 */
public interface SkillRepository extends JpaRepository<Skill, Long> {

    /**
     * Finds a skill by name and knowledge area
     *
     * @param name the skill name
     * @param knowledgeArea the knowledge area
     * @return Optional containing the skill if found
     */
    Optional<Skill> findByNameAndKnowledgeArea(String name, KnowledgeArea knowledgeArea);

    /**
     * Finds all active skills by knowledge area
     *
     * @param knowledgeAreaId the knowledge area ID
     * @return List of active skills
     */
    @Query("SELECT s FROM Skill s WHERE s.knowledgeArea.id = :knowledgeAreaId AND s.active = true")
    List<Skill> findActiveSkillsByKnowledgeAreaId(@Param("knowledgeAreaId") Long knowledgeAreaId);

    /**
     * Finds all active skills
     *
     * @return List of all active skills
     */
    @Query("SELECT s FROM Skill s WHERE s.active = true")
    List<Skill> findAllActiveSkills();

    /**
     * Finds skills by IDs
     *
     * @param ids List of skill IDs
     * @return List of skills
     */
    @Query("SELECT s FROM Skill s WHERE s.id IN :ids AND s.active = true")
    List<Skill> findAllByIdIn(@Param("ids") List<Long> ids);
}