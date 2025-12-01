
package com.project.skillswap.logic.entity.Skill;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Skill business logic
 */
@Service
public class SkillService {

    //<editor-fold desc="Dependencies">
    @Autowired
    private SkillRepository skillRepository;
    //</editor-fold>

    //<editor-fold desc="Public Methods">
    /**
     * Gets all active skills by knowledge area ID
     *
     * @param knowledgeAreaId the knowledge area ID
     * @return List of active skills
     */
    @Transactional(readOnly = true)
    public List<Skill> getActiveSkillsByKnowledgeAreaId(Long knowledgeAreaId) {
        return skillRepository.findActiveSkillsByKnowledgeAreaId(knowledgeAreaId);
    }

    /**
     * Gets all active skills
     *
     * @return List of all active skills
     */
    @Transactional(readOnly = true)
    public List<Skill> getAllActiveSkills() {
        return skillRepository.findAllActiveSkills();
    }

    /**
     * Gets a skill by ID
     *
     * @param id the skill ID
     * @return Optional containing the skill if found
     */
    @Transactional(readOnly = true)
    public Optional<Skill> getSkillById(Long id) {
        return skillRepository.findById(id);
    }

    /**
     * Gets skills by IDs
     *
     * @param ids List of skill IDs
     * @return List of skills
     */
    @Transactional(readOnly = true)
    public List<Skill> getSkillsByIds(List<Long> ids) {
        return skillRepository.findAllByIdIn(ids);
    }

    /**
     * Saves a skill
     *
     * @param skill the skill to save
     * @return the saved skill
     */
    @Transactional
    public Skill saveSkill(Skill skill) {
        return skillRepository.save(skill);
    }

    /**
     * Deletes a skill by ID
     *
     * @param id the skill ID
     */
    @Transactional
    public void deleteSkill(Long id) {
        skillRepository.deleteById(id);
    }
    //</editor-fold>
}