package com.project.skillswap.logic.entity.Knowledgearea;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeAreaRepository extends JpaRepository<KnowledgeArea, Long> {

    List<KnowledgeArea> findByActiveTrue();

    @Query("SELECT ka FROM KnowledgeArea ka WHERE ka.active = true ORDER BY ka.name ASC")
    List<KnowledgeArea> findAllActiveOrderByName();
}