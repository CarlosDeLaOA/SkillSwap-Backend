package com.project.skillswap.logic.entity.Knowledgearea;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeAreaRepository extends JpaRepository<KnowledgeArea, Long> {

    //<editor-fold desc="Active Knowledge Areas Queries">
    /**
     * Obtiene todas las áreas de conocimiento activas
     */
    List<KnowledgeArea> findByActiveTrue();

    /**
     * Obtiene áreas de conocimiento activas ordenadas por nombre
     */
    @Query("SELECT ka FROM KnowledgeArea ka WHERE ka.active = true ORDER BY ka.name ASC")
    List<KnowledgeArea> findAllActiveOrderByName();
    //</editor-fold>

    //<editor-fold desc="Find By Name">
    /**
     * Busca un área de conocimiento por nombre
     */
    Optional<KnowledgeArea> findByName(String name);
    //</editor-fold>
}