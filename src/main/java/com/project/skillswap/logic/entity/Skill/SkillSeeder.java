package com.project.skillswap.logic.entity.Skill;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial skills in the database
 */
@Order(3)
@Component
public class SkillSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final SkillRepository skillRepository;
    private final KnowledgeAreaRepository knowledgeAreaRepository;
    //#endregion

    //#region Constructor
    /**
     * Creates a new SkillSeeder instance
     *
     * @param skillRepository the skill repository
     * @param knowledgeAreaRepository the knowledge area repository
     */
    public SkillSeeder(SkillRepository skillRepository, KnowledgeAreaRepository knowledgeAreaRepository) {
        this.skillRepository = skillRepository;
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
        this.seedSkills();
    }
    //#endregion

    //#region Seeding Logic
    /**
     * Seeds skills into the database
     */
    private void seedSkills() {
        List<SkillData> skillsToCreate = createSkillDataList();

        for (SkillData skillData : skillsToCreate) {
            Optional<KnowledgeArea> knowledgeArea = knowledgeAreaRepository.findByName(skillData.knowledgeAreaName);

            if (knowledgeArea.isEmpty()) {
                continue;
            }

            Optional<Skill> existingSkill = skillRepository.findByNameAndKnowledgeArea(
                    skillData.name,
                    knowledgeArea.get()
            );

            if (existingSkill.isPresent()) {
                continue;
            }

            Skill skill = createSkill(skillData, knowledgeArea.get());
            skillRepository.save(skill);
        }

        System.out.println(" Skills seeded successfully");
    }

    /**
     * Creates a Skill entity from SkillData
     *
     * @param data the skill data
     * @param knowledgeArea the associated knowledge area
     * @return the created Skill entity
     */
    private Skill createSkill(SkillData data, KnowledgeArea knowledgeArea) {
        Skill skill = new Skill();
        skill.setKnowledgeArea(knowledgeArea);
        skill.setName(data.name);
        skill.setDescription(data.description);
        skill.setActive(data.active);
        return skill;
    }

    /**
     * Creates the list of skill data to be seeded
     *
     * @return list of SkillData objects
     */
    private List<SkillData> createSkillDataList() {
        List<SkillData> skills = new ArrayList<>();

        skills.add(new SkillData("Programming", "Java Programming",
                "Object-oriented programming with Java", true));
        skills.add(new SkillData("Programming", "Python Programming",
                "Python fundamentals and advanced concepts", true));
        skills.add(new SkillData("Programming", "JavaScript",
                "Modern JavaScript and ES6+", true));
        skills.add(new SkillData("Programming", "React Programming",
                "Building web applications with React", true));
        skills.add(new SkillData("Programming", "Angular",
                "Full-stack development with Angular", true));

        skills.add(new SkillData("Design", "UI/UX Design",
                "User interface and experience design principles", true));
        skills.add(new SkillData("Design", "Graphic Design",
                "Visual design and branding", true));
        skills.add(new SkillData("Design", "Adobe Photoshop",
                "Photo editing and digital art", true));

        skills.add(new SkillData("Languages", "English",
                "English language learning", true));
        skills.add(new SkillData("Languages", "Spanish",
                "Spanish language and culture", true));
        skills.add(new SkillData("Languages", "French",
                "French language fundamentals", true));
        skills.add(new SkillData("Languages", "Mandarin Chinese",
                "Mandarin speaking and writing", true));

        skills.add(new SkillData("Business", "Digital Marketing",
                "Online marketing strategies and SEO", true));
        skills.add(new SkillData("Business", "Project Management",
                "Agile and traditional project management", true));
        skills.add(new SkillData("Business", "Entrepreneurship",
                "Starting and growing a business", true));

        skills.add(new SkillData("Arts", "Guitar",
                "Playing guitar and music theory", true));
        skills.add(new SkillData("Arts", "Photography",
                "Digital photography techniques", true));
        skills.add(new SkillData("Arts", "Drawing",
                "Sketching and illustration", true));

        skills.add(new SkillData("Science", "Mathematics",
                "Algebra, calculus, and statistics", true));
        skills.add(new SkillData("Science", "Physics",
                "Classical and modern physics", true));
        skills.add(new SkillData("Science", "Data Science",
                "Data analysis and machine learning", true));

        skills.add(new SkillData("Health & Fitness", "Yoga",
                "Yoga practice and meditation", true));
        skills.add(new SkillData("Health & Fitness", "Personal Training",
                "Fitness training and exercise", true));

        skills.add(new SkillData("Cooking", "Italian Cuisine",
                "Traditional Italian cooking", true));
        skills.add(new SkillData("Cooking", "Baking",
                "Bread and pastry making", true));

        return skills;
    }
    //#endregion

    //#region Inner Class
    /**
     * Data class holding information for creating skills
     */
    private static class SkillData {
        String knowledgeAreaName;
        String name;
        String description;
        Boolean active;

        SkillData(String knowledgeAreaName, String name, String description, Boolean active) {
            this.knowledgeAreaName = knowledgeAreaName;
            this.name = name;
            this.description = description;
            this.active = active;
        }
    }
    //#endregion
}