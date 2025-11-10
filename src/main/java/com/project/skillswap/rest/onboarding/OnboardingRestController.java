package com.project.skillswap.rest.onboarding;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaRepository;
// NUEVO
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaRepository.KnowledgeAreaSummary;
import com.project.skillswap.logic.entity.PersonRoleSkill.PersonRoleSkill;
import com.project.skillswap.logic.entity.PersonRoleSkill.PersonRoleSkillRepository;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Onboarding.OnboardingService;

@RestController
@RequestMapping("/onboarding")
public class OnboardingRestController {

    private final OnboardingService onboardingService;
    private final PersonRepository personRepository;
    private final PersonRoleSkillRepository personRoleSkillRepository;

    public OnboardingRestController(OnboardingService onboardingService,
                                    PersonRepository personRepository,
                                    PersonRoleSkillRepository personRoleSkillRepository) {
        this.onboardingService = onboardingService;
        this.personRepository = personRepository;
        this.personRoleSkillRepository = personRoleSkillRepository;
    }

    // NUEVO: endpoint plano que evita recursión y devuelve (id, name, active)
    @GetMapping("/categories")
    public ResponseEntity<List<KnowledgeAreaSummary>> categories() {
        return ResponseEntity.ok(onboardingService.listActiveCategories());
    }

    // --------------------------------------------------------------------------------------
    // Endpoints originales (mantén todo lo que ya tenías)
    // --------------------------------------------------------------------------------------

    @GetMapping("/all")
    public List<KnowledgeArea> getAllKnowledgeAreas() {
        return onboardingService.getAllKnowledgeAreas();
    }

    @GetMapping("/active")
    public List<KnowledgeArea> getActiveKnowledgeAreas() {
        return onboardingService.getActiveKnowledgeAreas();
    }

    @PostMapping("/save-skill")
    public ResponseEntity<PersonRoleSkill> savePersonRoleSkill(@RequestBody PersonRoleSkill skill) {
        PersonRoleSkill savedSkill = onboardingService.save(skill);
        return ResponseEntity.ok(savedSkill);
    }

    @GetMapping("/skills")
    public List<PersonRoleSkill> getAllPersonRoleSkills() {
        return onboardingService.getAllPersonRoleSkills();
    }

    @GetMapping("/person/{id}")
    public ResponseEntity<Person> getPersonById(@PathVariable Long id) {
        return onboardingService.getPersonById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
