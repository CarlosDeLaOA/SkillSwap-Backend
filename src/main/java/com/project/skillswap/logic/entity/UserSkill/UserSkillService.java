package com.project.skillswap.logic.entity.UserSkill;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for UserSkill business logic
 */
@Service
public class UserSkillService {

    //<editor-fold desc="Dependencies">
    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private SkillService skillService;
    //</editor-fold>

    //<editor-fold desc="Public Methods">
    /**
     * Saves user skills for a person
     *
     * @param person the person
     * @param skillIds list of skill IDs to save
     * @return List of saved user skills
     * @throws IllegalArgumentException if any skill ID is invalid
     */
    @Transactional
    public List<UserSkill> saveUserSkills(Person person, List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            throw new IllegalArgumentException("Skill IDs cannot be empty");
        }

        List<Skill> skills = skillService.getSkillsByIds(skillIds);

        if (skills.size() != skillIds.size()) {
            throw new IllegalArgumentException("One or more skill IDs are invalid");
        }

        for (Skill skill : skills) {
            Optional<UserSkill> existingUserSkill = userSkillRepository.findByPersonAndSkillId(person, skill.getId());

            if (existingUserSkill.isEmpty()) {
                UserSkill userSkill = new UserSkill();
                userSkill.setPerson(person);
                userSkill.setSkill(skill);
                userSkill.setSelectedDate(LocalDateTime.now());
                userSkill.setActive(true);
                userSkillRepository.save(userSkill);
            } else {
                UserSkill existingSkill = existingUserSkill.get();
                existingSkill.setActive(true);
                existingSkill.setSelectedDate(LocalDateTime.now());
                userSkillRepository.save(existingSkill);
            }
        }

        return userSkillRepository.findActiveUserSkillsByPersonId(person.getId());
    }

    /**
     * Gets all active user skills for a person
     *
     * @param personId the person ID
     * @return List of active user skills
     */
    @Transactional(readOnly = true)
    public List<UserSkill> getActiveUserSkillsByPersonId(Long personId) {
        return userSkillRepository.findActiveUserSkillsByPersonId(personId);
    }

    /**
     * Gets all user skills for a person
     *
     * @param person the person
     * @return List of user skills
     */
    @Transactional(readOnly = true)
    public List<UserSkill> getUserSkillsByPerson(Person person) {
        return userSkillRepository.findByPerson(person);
    }

    /**
     * Deletes a user skill
     *
     * @param id the user skill ID
     */
    @Transactional
    public void deleteUserSkill(Long id) {
        userSkillRepository.deleteById(id);
    }

    /**
     * Deactivates a user skill
     *
     * @param id the user skill ID
     * @throws IllegalArgumentException if user skill not found
     */
    @Transactional
    public void deactivateUserSkill(Long id) {
        Optional<UserSkill> userSkill = userSkillRepository.findById(id);
        if (userSkill.isEmpty()) {
            throw new IllegalArgumentException("User skill not found");
        }

        UserSkill skill = userSkill.get();
        skill.setActive(false);
        userSkillRepository.save(skill);
    }
    //</editor-fold>
}