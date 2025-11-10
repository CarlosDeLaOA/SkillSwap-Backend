package com.project.skillswap.logic.entity.Knowledgearea;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeAreaRepository extends JpaRepository<KnowledgeArea, Long> {
    Optional<KnowledgeArea> findByNameIgnoreCase(String name);
    public interface KnowledgeAreaSummary {
        Long getId();
        String getName();
        Boolean getActive();
    }

    // NUEVO: query por activas en orden
    List<KnowledgeAreaSummary> findAllByActiveTrueOrderByNameAsc();
}
