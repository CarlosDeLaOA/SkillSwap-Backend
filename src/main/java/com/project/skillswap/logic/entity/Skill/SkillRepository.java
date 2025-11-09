package com.project.skillswap.logic.entity.Skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByKnowledgeAreaIdAndActiveTrue(Long knowledgeAreaId);
    Optional<Skill> findByNameIgnoreCaseAndKnowledgeAreaId(String name, Long knowledgeAreaId);
}
