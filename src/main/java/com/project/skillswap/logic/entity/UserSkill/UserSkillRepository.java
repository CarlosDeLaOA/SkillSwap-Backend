package com.project.skillswap.logic.entity.UserSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Skill.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserSkill entity operations
 */
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    /**
     * Finds all user skills for a specific person
     *
     * @param person the person
     * @return List of user skills
     */
    List<UserSkill> findByPerson(Person person);

    /**
     * Finds all active user skills for a specific person
     *
     * @param personId the person ID
     * @return List of active user skills
     */
    @Query("SELECT us FROM UserSkill us WHERE us.person.id = :personId AND us.active = true")
    List<UserSkill> findActiveUserSkillsByPersonId(@Param("personId") Long personId);

    /**
     * Finds a user skill by person and skill
     *
     * @param person the person
     * @param skillId the skill ID
     * @return Optional containing the user skill if found
     */
    @Query("SELECT us FROM UserSkill us WHERE us.person = :person AND us.skill.id = :skillId")
    Optional<UserSkill> findByPersonAndSkillId(@Param("person") Person person, @Param("skillId") Long skillId);

    /**
     * Deletes all user skills for a specific person
     *
     * @param person the person
     */
    void deleteByPerson(Person person);

    Optional<UserSkill> findByPersonAndSkill(Person person, Skill skill);
}
