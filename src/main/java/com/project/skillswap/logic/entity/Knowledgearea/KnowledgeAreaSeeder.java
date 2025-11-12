package com.project.skillswap.logic.entity.Knowledgearea;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Order(2)
@Component
public class KnowledgeAreaSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private final KnowledgeAreaRepository knowledgeAreaRepository;

    public KnowledgeAreaSeeder(KnowledgeAreaRepository knowledgeAreaRepository) {
        this.knowledgeAreaRepository = knowledgeAreaRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedKnowledgeAreas();
    }

    private void seedKnowledgeAreas() {
        List<KnowledgeAreaData> areasToCreate = createKnowledgeAreaDataList();

        for (KnowledgeAreaData areaData : areasToCreate) {
            Optional<KnowledgeArea> existingArea = knowledgeAreaRepository.findByName(areaData.name);
            if (existingArea.isPresent()) continue;

            KnowledgeArea area = new KnowledgeArea();
            area.setName(areaData.name);
            area.setDescription(areaData.description);
            area.setIconUrl(areaData.iconUrl);
            area.setActive(areaData.active);
            knowledgeAreaRepository.save(area);
        }
    }

    private List<KnowledgeAreaData> createKnowledgeAreaDataList() {
        List<KnowledgeAreaData> areas = new ArrayList<>();

        areas.add(new KnowledgeAreaData(
                "Cocina",
                "Aprende y perfecciona tus habilidades culinarias",
                "https://img.icons8.com/color/96/chef-hat.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Idiomas",
                "Desarrolla tus conocimientos en diferentes idiomas y culturas",
                "https://img.icons8.com/color/96/language.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Programación",
                "Desarrollo de software y lenguajes de programación",
                "https://img.icons8.com/color/96/code.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Deportes",
                "Actividad física, entrenamiento y vida saludable",
                "https://img.icons8.com/?size=100&id=MyDyhaNB2oZy&format=png&color=000000",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Arte",
                "Explora tu creatividad a través del arte y la expresión visual",
                "https://img.icons8.com/?size=100&id=rufzEhS2OFRa&format=png&color=000000",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Power Skills",
                "Desarrolla habilidades blandas y de liderazgo para el éxito personal y profesional",
                "https://img.icons8.com/?size=100&id=5RO7zCaSnyhS&format=png&color=000000",
                true
        ));

        return areas;
    }

    private static class KnowledgeAreaData {
        String name;
        String description;
        String iconUrl;
        Boolean active;

        KnowledgeAreaData(String name, String description, String iconUrl, Boolean active) {
            this.name = name;
            this.description = description;
            this.iconUrl = iconUrl;
            this.active = active;
        }
    }
}
