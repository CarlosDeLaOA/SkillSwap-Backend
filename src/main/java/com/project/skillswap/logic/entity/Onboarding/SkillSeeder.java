package com.project.skillswap.logic.entity.Onboarding;

import com.project.skillswap.logic.entity.KnowledgeArea.KnowledgeArea;
import com.project.skillswap.logic.entity.KnowledgeArea.KnowledgeAreaRepository;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@Component
public class SkillSeeder implements CommandLineRunner {

    //#region Deps
    private final KnowledgeAreaRepository areaRepo;
    private final SkillRepository skillRepo;
    //#endregion

    //#region Ctor
    public SkillSeeder(KnowledgeAreaRepository areaRepo, SkillRepository skillRepo) {
        this.areaRepo = areaRepo;
        this.skillRepo = skillRepo;
    }
    //#endregion

    @Override
    public void run(String... args) {
        Map<String, List<String>> data = Map.ofEntries(
                entry("Tecnología", List.of("Java", "Spring Boot", "Angular", "Bases de Datos")),
                entry("Arte", List.of("Dibujo a lápiz", "Acuarela", "Ilustración digital")),
                entry("Deportes", List.of("Fútbol", "Calistenia", "Natación")),
                entry("Idiomas", List.of("Inglés", "Japonés", "Italiano")),
                entry("Música", List.of("Guitarra", "Piano", "Canto")),
                entry("Cocina", List.of("Repostería", "Panadería", "Cocina Keto")),
                entry("Ciencias", List.of("Física básica", "Química orgánica", "Biología")),
                entry("Negocios", List.of("Emprendimiento", "Finanzas personales", "Marketing")),
                entry("Salud & Fitness", List.of("Hipertrofia", "Nutrición básica", "Movilidad")),
                entry("Fotografía", List.of("Fotografía móvil", "Edición Lightroom", "Composición")),
                entry("Historia", List.of("Historia universal", "Historia de Costa Rica")),
                entry("Diseño", List.of("UX/UI", "Figma", "Tipografía"))
        );

        data.forEach((areaName, skills) -> {
            KnowledgeArea area = areaRepo.findByNameIgnoreCase(areaName)
                    .orElseThrow(() -> new IllegalStateException("Falta KnowledgeArea seed: " + areaName));

            skills.forEach(skillName -> upsertSkill(area, skillName));
        });
    }

    //#region Helpers
    private void upsertSkill(KnowledgeArea area, String skillName) {
        skillRepo.findByNameIgnoreCaseAndKnowledgeAreaId(skillName, area.getId())
                .orElseGet(() -> {
                    Skill s = new Skill();
                    s.setName(skillName);
                    s.setDescription(null);
                    s.setActive(true);
                    s.setKnowledgeArea(area);
                    return skillRepo.save(s);
                });
    }
    //#endregion
}
