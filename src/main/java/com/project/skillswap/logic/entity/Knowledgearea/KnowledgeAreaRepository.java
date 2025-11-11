package com.project.skillswap.logic.entity.Knowledgearea;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeAreaRepository extends JpaRepository<KnowledgeArea, Long> {
    Optional<KnowledgeArea> findByName(String name);
}