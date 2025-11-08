package com.project.skillswap.rest.onboarding;

import com.project.skillswap.logic.entity.KnowledgeArea.KnowledgeArea;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Onboarding.OnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/onboarding")
public class OnboardingController {
    //#region Deps
    private final OnboardingService service;
    //#endregion

    //#region Ctor
    public OnboardingController(OnboardingService service) {
        this.service = service;
    }
    //#endregion

    //#region GET
    @GetMapping("/categories")
    public List<KnowledgeArea> categories() {
        return service.listCategories();
    }

    @GetMapping("/skills")
    public List<Skill> skillsByCategory(@RequestParam("categoryId") Long categoryId) {
        return service.listSkillsByCategory(categoryId);
    }

    @GetMapping("/person-skills")
    public List<Skill> personSkills(@RequestParam("personId") Long personId) {
        return service.listPersonSkills(personId);
    }
    //#endregion

    //#region POST
    @PostMapping("/selection")
    public ResponseEntity<Integer> saveSelection(
            @RequestParam("personId") Long personId,
            @RequestBody List<Long> skillIds
    ) {
        int inserted = service.saveSelection(personId, skillIds);
        return ResponseEntity.ok(inserted);
    }
    //#endregion

}
