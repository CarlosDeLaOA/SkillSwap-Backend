package com.project.skillswap.logic.entity.Skill;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    //búsqueda por nombre (ignorando mayúsculas) y área (por ID)
    Optional<Skill> findByNameIgnoreCaseAndKnowledgeAreaId(String name, Long knowledgeAreaId);

  //lista todas las skills activas por área
    List<Skill> findByKnowledgeAreaIdAndActiveTrue(Long knowledgeAreaId);
}
