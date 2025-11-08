package com.project.skillswap.logic.entity.Skill;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByNameAndKnowledgeArea(String name, KnowledgeArea knowledgeArea);
}