package com.project.skillswap.logic.entity.Knowledgearea;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaRepository;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial knowledge areas in the database
 */
@Order(2)
@Component
public class KnowledgeAreaSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final KnowledgeAreaRepository knowledgeAreaRepository;
    //#endregion

    //#region Constructor
    /**
     * Creates a new KnowledgeAreaSeeder instance
     *
     * @param knowledgeAreaRepository the knowledge area repository
     */
    public KnowledgeAreaSeeder(KnowledgeAreaRepository knowledgeAreaRepository) {
        this.knowledgeAreaRepository = knowledgeAreaRepository;
    }
    //#endregion

    //#region Event Handling
    /**
     * Handles the application context refreshed event to seed initial data
     *
     * @param event the context refreshed event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedKnowledgeAreas();
    }
    //#endregion

    //#region Seeding Logic
    /**
     * Seeds knowledge areas into the database
     */
    private void seedKnowledgeAreas() {
        List<KnowledgeAreaData> areasToCreate = createKnowledgeAreaDataList();

        for (KnowledgeAreaData areaData : areasToCreate) {
            Optional<KnowledgeArea> existingArea = knowledgeAreaRepository.findByNameIgnoreCase(areaData.name);

            if (existingArea.isPresent()) {
                continue;
            }

            KnowledgeArea area = createKnowledgeArea(areaData);
            knowledgeAreaRepository.save(area);
        }

        System.out.println(" Knowledge areas seeded successfully");
    }

    /**
     * Creates a KnowledgeArea entity from KnowledgeAreaData
     *
     * @param data the knowledge area data
     * @return the created KnowledgeArea entity
     */
    private KnowledgeArea createKnowledgeArea(KnowledgeAreaData data) {
        KnowledgeArea area = new KnowledgeArea();
        area.setName(data.name);
        //area.setDescription(data.description);
        //area.setIconUrl(data.iconUrl);
        area.setActive(data.active);
        return area;
    }

    /**
     * Creates the list of knowledge area data to be seeded
     *
     * @return list of KnowledgeAreaData objects
     */
    private List<KnowledgeAreaData> createKnowledgeAreaDataList() {
        List<KnowledgeAreaData> areas = new ArrayList<>();

        areas.add(new KnowledgeAreaData(
                "Programming",
                "Software development and programming languages",
                "https://img.icons8.com/color/96/code.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Design",
                "Graphic design, UI/UX, and visual arts",
                "https://img.icons8.com/color/96/design.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Languages",
                "Foreign language learning and linguistics",
                "https://img.icons8.com/color/96/language.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Business",
                "Business management, marketing, and entrepreneurship",
                "https://img.icons8.com/color/96/business.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Arts",
                "Music, painting, photography, and creative arts",
                "https://img.icons8.com/color/96/art.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Science",
                "Mathematics, physics, chemistry, and natural sciences",
                "https://img.icons8.com/color/96/science.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Health & Fitness",
                "Physical fitness, nutrition, and wellness",
                "https://img.icons8.com/color/96/health.png",
                true
        ));

        areas.add(new KnowledgeAreaData(
                "Cooking",
                "Culinary arts and food preparation",
                "https://img.icons8.com/color/96/chef-hat.png",
                true
        ));

        return areas;
    }
    //#endregion

    //#region Inner Class
    /**
     * Data class holding information for creating knowledge areas
     */
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
    //#endregion
}