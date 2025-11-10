package com.project.skillswap.logic.entity.Onboarding;

import org.springframework.stereotype.Service;
// NUEVO
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaRepository;
// NUEVO
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaRepository.KnowledgeAreaSummary;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.PersonRoleSkill.PersonRoleSkill;
import com.project.skillswap.logic.entity.PersonRoleSkill.PersonRoleSkillRepository;

@Service
public class OnboardingService {

    private final KnowledgeAreaRepository knowledgeAreaRepository;
    private final PersonRepository personRepository;
    private final PersonRoleSkillRepository personRoleSkillRepository;

    public OnboardingService(
            KnowledgeAreaRepository knowledgeAreaRepository,
            PersonRepository personRepository,
            PersonRoleSkillRepository personRoleSkillRepository
    ) {
        this.knowledgeAreaRepository = knowledgeAreaRepository;
        this.personRepository = personRepository;
        this.personRoleSkillRepository = personRoleSkillRepository;
    }

    // NUEVO: categorías activas como proyección (id, name, active) para evitar LAZY/recursión
    @Transactional(readOnly = true)
    public List<KnowledgeAreaSummary> listActiveCategories() {
        return knowledgeAreaRepository.findAllByActiveTrueOrderByNameAsc();
    }

    // --------------------------------------------------------------------------------------
    // Métodos originales (sin cambios)
    // --------------------------------------------------------------------------------------

    public List<KnowledgeArea> getAllKnowledgeAreas() {
        return knowledgeAreaRepository.findAll();
    }

    public List<KnowledgeArea> getActiveKnowledgeAreas() {
        return knowledgeAreaRepository.findAll().stream()
                .filter(KnowledgeArea::getActive)
                .toList();
    }

    public Optional<KnowledgeArea> findByName(String name) {
        return knowledgeAreaRepository.findByNameIgnoreCase(name);
    }

    public PersonRoleSkill save(PersonRoleSkill entity) {
        return personRoleSkillRepository.save(entity);
    }

    public List<PersonRoleSkill> getAllPersonRoleSkills() {
        return personRoleSkillRepository.findAll();
    }

    public Optional<Person> getPersonById(Long id) {
        return personRepository.findById(id);
    }
}
