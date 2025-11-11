package com.project.skillswap.logic.entity.Knowledgearea;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeAreaService {

    @Autowired
    private KnowledgeAreaRepository knowledgeAreaRepository;

    //<region desc="Public Methods">
    /**
     * Obtiene todas las áreas de conocimiento activas ordenadas por nombre
     */
    @Transactional(readOnly = true)
    public List<KnowledgeArea> getAllActiveKnowledgeAreas() {
        return knowledgeAreaRepository.findAllActiveOrderByName();
    }
    //</region>
}