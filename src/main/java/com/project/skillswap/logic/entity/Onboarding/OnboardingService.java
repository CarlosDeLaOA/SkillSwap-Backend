package com.project.skillswap.logic.entity.Onboarding;

import com.project.skillswap.logic.entity.KnowledgeArea.KnowledgeArea;
import com.project.skillswap.logic.entity.KnowledgeArea.KnowledgeAreaRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Service
public class OnboardingService {

    //#region Dependencies
    private final PersonSkillRepository personSkillRepo;
    private final PersonRepository personRepo;
    private final SkillRepository skillRepo;
    private final KnowledgeAreaRepository knowledgeAreaRepo;
    //#endregion

    //#region Constructor
    public OnboardingService(
            PersonSkillRepository personSkillRepo,
            PersonRepository personRepo,
            SkillRepository skillRepo,
            KnowledgeAreaRepository knowledgeAreaRepo
    ) {
        this.personSkillRepo = personSkillRepo;
        this.personRepo = personRepo;
        this.skillRepo = skillRepo;
        this.knowledgeAreaRepo = knowledgeAreaRepo;
    }
    //#endregion

    //#region Queries
    public List<KnowledgeArea> listCategories() {
        return knowledgeAreaRepo.findAll()
                .stream()
                .filter(ka -> Boolean.TRUE.equals(ka.getActive()))
                .toList();
    }

    public List<Skill> listSkillsByCategory(Long knowledgeAreaId) {
        return skillRepo.findByKnowledgeAreaIdAndActiveTrue(knowledgeAreaId);
    }

    public List<Skill> listPersonSkills(Long personId) {
        return personSkillRepo.findByPersonId(personId)
                .stream()
                .map(PersonSkill::getSkill)
                .toList();
    }
    //#endregion

    //#region Commands
    @Transactional
    public int saveSelection(Long personId, Collection<Long> skillIds) {
        Person person = personRepo.findById(personId).orElseThrow();

        int inserted = 0;
        for (Long skillId : new HashSet<>(skillIds)) { // evita duplicados en la misma request
            if (!personSkillRepo.existsByPersonIdAndSkillId(personId, skillId)) {
                Skill skill = skillRepo.findById(skillId).orElseThrow();
                personSkillRepo.save(new PersonSkill(person, skill));
                inserted++;
            }
        }
        return inserted;
    }
    //#endregion
}
