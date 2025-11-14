package com.project.skillswap.logic.entity.UserSkill;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial user-skill relationships in the database
 */
@Order(4)
@Component
public class UserSkillSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final UserSkillRepository userSkillRepository;
    private final PersonRepository personRepository;
    private final SkillRepository skillRepository;
    //#endregion

    //#region Constructor
    /**
     * Creates a new UserSkillSeeder instance
     *
     * @param userSkillRepository the user skill repository
     * @param personRepository the person repository
     * @param skillRepository the skill repository
     */
    public UserSkillSeeder(
            UserSkillRepository userSkillRepository,
            PersonRepository personRepository,
            SkillRepository skillRepository) {
        this.userSkillRepository = userSkillRepository;
        this.personRepository = personRepository;
        this.skillRepository = skillRepository;
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
        this.seedUserSkills();
    }
    //#endregion

    //#region Seeding Logic
    /**
     * Seeds user skills into the database
     */
    private void seedUserSkills() {
        List<UserSkillData> userSkillsToCreate = createUserSkillDataList();

        for (UserSkillData userSkillData : userSkillsToCreate) {
            Optional<Person> person = personRepository.findById(userSkillData.personId);
            Optional<Skill> skill = skillRepository.findById(userSkillData.skillId);

            if (person.isEmpty() || skill.isEmpty()) {
                continue;
            }

            // Check if the relationship already exists
            Optional<UserSkill> existingUserSkill = userSkillRepository.findByPersonAndSkill(
                    person.get(), skill.get()
            );

            if (existingUserSkill.isPresent()) {
                continue;
            }

            UserSkill userSkill = createUserSkill(userSkillData, person.get(), skill.get());
            userSkillRepository.save(userSkill);
        }
    }

    /**
     * Creates a UserSkill entity from UserSkillData
     *
     * @param data the user skill data
     * @param person the person
     * @param skill the skill
     * @return the created UserSkill entity
     */
    private UserSkill createUserSkill(UserSkillData data, Person person, Skill skill) {
        UserSkill userSkill = new UserSkill();
        userSkill.setPerson(person);
        userSkill.setSkill(skill);
        userSkill.setSelectedDate(data.selectedDate);
        userSkill.setActive(data.active);
        return userSkill;
    }

    /**
     * Creates the list of user skill data to be seeded
     *
     * @return list of UserSkillData objects
     */
    private List<UserSkillData> createUserSkillDataList() {
        List<UserSkillData> userSkills = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // María Rodríguez (Person 1) - Instructora de español
        userSkills.add(new UserSkillData(1L, 12L, now.minusDays(200), true)); // Español
        userSkills.add(new UserSkillData(1L, 13L, now.minusDays(180), true)); // Francés
        userSkills.add(new UserSkillData(1L, 1L, now.minusDays(150), true));  // Cocina Italiana
        userSkills.add(new UserSkillData(1L, 4L, now.minusDays(100), true));  // Power Skills - Comunicación

        // John Smith (Person 2) - MUCHOS REGISTROS (Learner principal)
        userSkills.add(new UserSkillData(2L, 2L, now.minusDays(300), true));  // Python
        userSkills.add(new UserSkillData(2L, 3L, now.minusDays(280), true));  // JavaScript
        userSkills.add(new UserSkillData(2L, 5L, now.minusDays(250), true));  // React
        userSkills.add(new UserSkillData(2L, 8L, now.minusDays(240), true));  // SQL
        userSkills.add(new UserSkillData(2L, 21L, now.minusDays(220), true)); // Machine Learning
        userSkills.add(new UserSkillData(2L, 12L, now.minusDays(200), true)); // Español
        userSkills.add(new UserSkillData(2L, 18L, now.minusDays(180), true)); // Caligrafía Japonesa
        userSkills.add(new UserSkillData(2L, 7L, now.minusDays(160), true));  // Matemáticas
        userSkills.add(new UserSkillData(2L, 10L, now.minusDays(140), true)); // Git y GitHub
        userSkills.add(new UserSkillData(2L, 16L, now.minusDays(120), true)); // Guitarra
        userSkills.add(new UserSkillData(2L, 3L, now.minusDays(100), true));  // Yoga
        userSkills.add(new UserSkillData(2L, 17L, now.minusDays(80), true));  // Mandarín
        userSkills.add(new UserSkillData(2L, 4L, now.minusDays(60), true));   // Gestión del Tiempo
        userSkills.add(new UserSkillData(2L, 1L, now.minusDays(50), true));   // Cocina Italiana
        userSkills.add(new UserSkillData(2L, 13L, now.minusDays(40), true));  // Francés
        userSkills.add(new UserSkillData(2L, 48L, now.minusDays(30), true));  // Fotografía
        userSkills.add(new UserSkillData(2L, 8L, now.minusDays(20), true));   // Diseño Gráfico
        userSkills.add(new UserSkillData(2L, 2L, now.minusDays(10), true));   // Liderazgo
        userSkills.add(new UserSkillData(2L, 19L, now.minusDays(5), true));   // Japonés

        // Sophie Chen (Person 3) - Instructora de programación
        userSkills.add(new UserSkillData(3L, 2L, now.minusDays(400), true));  // Python
        userSkills.add(new UserSkillData(3L, 3L, now.minusDays(380), true));  // JavaScript
        userSkills.add(new UserSkillData(3L, 5L, now.minusDays(350), true));  // React
        userSkills.add(new UserSkillData(3L, 6L, now.minusDays(320), true));  // Angular
        userSkills.add(new UserSkillData(3L, 8L, now.minusDays(300), true));  // SQL
        userSkills.add(new UserSkillData(3L, 21L, now.minusDays(250), true)); // Machine Learning
        userSkills.add(new UserSkillData(3L, 4L, now.minusDays(200), true));  // HTML y CSS

        // Lucas Santos (Person 4) - Instructor de guitarra
        userSkills.add(new UserSkillData(4L, 16L, now.minusDays(500), true)); // Guitarra
        userSkills.add(new UserSkillData(4L, 46L, now.minusDays(450), true)); // Música
        userSkills.add(new UserSkillData(4L, 16L, now.minusDays(400), true)); // Portugués
        userSkills.add(new UserSkillData(4L, 12L, now.minusDays(300), true)); // Español

        // Emma Wilson (Person 5) - Learner
        userSkills.add(new UserSkillData(5L, 11L, now.minusDays(180), true)); // Inglés
        userSkills.add(new UserSkillData(5L, 3L, now.minusDays(150), true));  // JavaScript
        userSkills.add(new UserSkillData(5L, 4L, now.minusDays(120), true));  // Gestión del Tiempo
        userSkills.add(new UserSkillData(5L, 48L, now.minusDays(100), true)); // Fotografía

        // Michael Johnson (Person 6) - Instructor de SQL
        userSkills.add(new UserSkillData(6L, 8L, now.minusDays(350), true));  // SQL
        userSkills.add(new UserSkillData(6L, 1L, now.minusDays(300), true));  // Java
        userSkills.add(new UserSkillData(6L, 7L, now.minusDays(250), true));  // C#
        userSkills.add(new UserSkillData(6L, 2L, now.minusDays(200), true));  // Python

        // Ana García (Person 7) - Instructora de fotografía
        userSkills.add(new UserSkillData(7L, 48L, now.minusDays(400), true)); // Fotografía
        userSkills.add(new UserSkillData(7L, 8L, now.minusDays(350), true));  // Diseño Gráfico
        userSkills.add(new UserSkillData(7L, 47L, now.minusDays(300), true)); // Pintura
        userSkills.add(new UserSkillData(7L, 12L, now.minusDays(250), true)); // Español

        // Oliver Brown (Person 8) - Instructor de Python
        userSkills.add(new UserSkillData(8L, 2L, now.minusDays(450), true));  // Python
        userSkills.add(new UserSkillData(8L, 8L, now.minusDays(400), true));  // SQL
        userSkills.add(new UserSkillData(8L, 10L, now.minusDays(350), true)); // Git y GitHub
        userSkills.add(new UserSkillData(8L, 21L, now.minusDays(300), true)); // Machine Learning
        userSkills.add(new UserSkillData(8L, 3L, now.minusDays(250), true));  // JavaScript

        // Yuki Tanaka (Person 9) - Instructora de arte japonés
        userSkills.add(new UserSkillData(9L, 18L, now.minusDays(600), true)); // Caligrafía Japonesa
        userSkills.add(new UserSkillData(9L, 19L, now.minusDays(550), true)); // Japonés
        userSkills.add(new UserSkillData(9L, 47L, now.minusDays(500), true)); // Pintura
        userSkills.add(new UserSkillData(9L, 48L, now.minusDays(450), true)); // Fotografía
        userSkills.add(new UserSkillData(9L, 60L, now.minusDays(400), true)); // Artesanías

        // Isabella Rossi (Person 10) - Instructora de cocina italiana
        userSkills.add(new UserSkillData(10L, 1L, now.minusDays(500), true)); // Cocina Italiana
        userSkills.add(new UserSkillData(10L, 2L, now.minusDays(450), true)); // Repostería
        userSkills.add(new UserSkillData(10L, 15L, now.minusDays(400), true)); // Italiano
        userSkills.add(new UserSkillData(10L, 6L, now.minusDays(350), true)); // Decoración de Platos

        // David Kim (Person 11) - Instructor de arquitectura de software
        userSkills.add(new UserSkillData(11L, 1L, now.minusDays(450), true));  // Java
        userSkills.add(new UserSkillData(11L, 2L, now.minusDays(400), true));  // Python
        userSkills.add(new UserSkillData(11L, 7L, now.minusDays(350), true));  // C#
        userSkills.add(new UserSkillData(11L, 8L, now.minusDays(300), true));  // SQL
        userSkills.add(new UserSkillData(11L, 10L, now.minusDays(250), true)); // Git y GitHub

        // Carlos Delao (Person 12) - MUCHOS REGISTROS (Instructor principal)
        userSkills.add(new UserSkillData(12L, 1L, now.minusDays(500), true));  // Java
        userSkills.add(new UserSkillData(12L, 2L, now.minusDays(480), true));  // Python
        userSkills.add(new UserSkillData(12L, 3L, now.minusDays(460), true));  // JavaScript
        userSkills.add(new UserSkillData(12L, 4L, now.minusDays(440), true));  // HTML y CSS
        userSkills.add(new UserSkillData(12L, 5L, now.minusDays(420), true));  // React
        userSkills.add(new UserSkillData(12L, 6L, now.minusDays(400), true));  // Angular
        userSkills.add(new UserSkillData(12L, 7L, now.minusDays(380), true));  // C#
        userSkills.add(new UserSkillData(12L, 8L, now.minusDays(360), true));  // SQL
        userSkills.add(new UserSkillData(12L, 9L, now.minusDays(340), true));  // Desarrollo Móvil
        userSkills.add(new UserSkillData(12L, 10L, now.minusDays(320), true)); // Git y GitHub
        userSkills.add(new UserSkillData(12L, 21L, now.minusDays(300), true)); // Machine Learning
        userSkills.add(new UserSkillData(12L, 12L, now.minusDays(280), true)); // Español
        userSkills.add(new UserSkillData(12L, 11L, now.minusDays(260), true)); // Inglés
        userSkills.add(new UserSkillData(12L, 48L, now.minusDays(240), true)); // Fotografía
        userSkills.add(new UserSkillData(12L, 8L, now.minusDays(220), true));  // Diseño Gráfico
        userSkills.add(new UserSkillData(12L, 2L, now.minusDays(200), true));  // Liderazgo
        userSkills.add(new UserSkillData(12L, 4L, now.minusDays(180), true));  // Gestión del Tiempo
        userSkills.add(new UserSkillData(12L, 1L, now.minusDays(160), true));  // Comunicación Efectiva
        userSkills.add(new UserSkillData(12L, 3L, now.minusDays(140), true));  // Trabajo en Equipo
        userSkills.add(new UserSkillData(12L, 5L, now.minusDays(120), true));  // Pensamiento Crítico

        // Natalie Blanc (Person 13) - Learner
        userSkills.add(new UserSkillData(13L, 13L, now.minusDays(250), true)); // Francés
        userSkills.add(new UserSkillData(13L, 11L, now.minusDays(200), true)); // Inglés
        userSkills.add(new UserSkillData(13L, 4L, now.minusDays(150), true));  // Gestión del Tiempo
        userSkills.add(new UserSkillData(13L, 1L, now.minusDays(100), true));  // Cocina Italiana

        // Marco Silva (Person 14) - Instructor de yoga
        userSkills.add(new UserSkillData(14L, 3L, now.minusDays(400), true));  // Yoga
        userSkills.add(new UserSkillData(14L, 9L, now.minusDays(350), true));  // Artes Marciales
        userSkills.add(new UserSkillData(14L, 10L, now.minusDays(300), true)); // Entrenamiento Funcional
        userSkills.add(new UserSkillData(14L, 16L, now.minusDays(250), true)); // Portugués

        // Sara Anderson (Person 15) - Learner
        userSkills.add(new UserSkillData(15L, 4L, now.minusDays(120), true));  // Gestión del Tiempo
        userSkills.add(new UserSkillData(15L, 1L, now.minusDays(100), true));  // Comunicación Efectiva
        userSkills.add(new UserSkillData(15L, 11L, now.minusDays(80), true));  // Inglés

        // Miguel López (Person 16) - Instructor de marketing digital
        userSkills.add(new UserSkillData(16L, 10L, now.minusDays(350), true)); // Marketing Digital (SEO)
        userSkills.add(new UserSkillData(16L, 8L, now.minusDays(300), true));  // Diseño Gráfico
        userSkills.add(new UserSkillData(16L, 12L, now.minusDays(250), true)); // Español
        userSkills.add(new UserSkillData(16L, 3L, now.minusDays(200), true));  // JavaScript

        // Elena Popov (Person 17) - Instructora de matemáticas
        userSkills.add(new UserSkillData(17L, 7L, now.minusDays(400), true));  // Matemáticas (Álgebra)
        userSkills.add(new UserSkillData(17L, 2L, now.minusDays(350), true));  // Python
        userSkills.add(new UserSkillData(17L, 8L, now.minusDays(300), true));  // SQL
        userSkills.add(new UserSkillData(17L, 11L, now.minusDays(250), true)); // Inglés

        // James Taylor (Person 18) - Learner
        userSkills.add(new UserSkillData(18L, 11L, now.minusDays(150), true)); // Inglés
        userSkills.add(new UserSkillData(18L, 2L, now.minusDays(120), true));  // Python
        userSkills.add(new UserSkillData(18L, 4L, now.minusDays(100), true));  // Gestión del Tiempo

        // Li Wei (Person 19) - Instructor de mandarín
        userSkills.add(new UserSkillData(19L, 17L, now.minusDays(500), true)); // Mandarín
        userSkills.add(new UserSkillData(19L, 11L, now.minusDays(450), true)); // Inglés
        userSkills.add(new UserSkillData(19L, 12L, now.minusDays(400), true)); // Español
        userSkills.add(new UserSkillData(19L, 18L, now.minusDays(350), true)); // Caligrafía

        // Clara Müller (Person 20) - Instructora de diseño gráfico
        userSkills.add(new UserSkillData(20L, 8L, now.minusDays(400), true));  // Diseño Gráfico
        userSkills.add(new UserSkillData(20L, 4L, now.minusDays(350), true));  // HTML y CSS
        userSkills.add(new UserSkillData(20L, 48L, now.minusDays(300), true)); // Fotografía
        userSkills.add(new UserSkillData(20L, 14L, now.minusDays(250), true)); // Alemán

        // Thomas Martin (Person 21) - Learner
        userSkills.add(new UserSkillData(21L, 10L, now.minusDays(150), true)); // Git y GitHub
        userSkills.add(new UserSkillData(21L, 2L, now.minusDays(120), true));  // Python
        userSkills.add(new UserSkillData(21L, 3L, now.minusDays(100), true));  // JavaScript

        return userSkills;
    }
    //#endregion

    //#region Inner Class
    /**
     * Data class holding information for creating user skills
     */
    private static class UserSkillData {
        Long personId;
        Long skillId;
        LocalDateTime selectedDate;
        Boolean active;

        UserSkillData(Long personId, Long skillId, LocalDateTime selectedDate, Boolean active) {
            this.personId = personId;
            this.skillId = skillId;
            this.selectedDate = selectedDate;
            this.active = active;
        }
    }
    //#endregion
}