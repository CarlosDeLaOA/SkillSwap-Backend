package com.project.skillswap.logic.entity.Onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PersonSkillRepository extends JpaRepository<PersonSkill, Long> {
    boolean existsByPersonIdAndSkillId(Long personId, Long skillId);
    List<PersonSkill> findByPersonId(Long personId);
}
