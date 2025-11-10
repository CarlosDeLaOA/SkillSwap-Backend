package com.project.skillswap.logic.entity.PersonRoleSkill;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PersonRoleSkillRepository extends JpaRepository<PersonRoleSkill, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM PersonRoleSkill prs WHERE prs.person.id = :personId AND prs.roleCode = :roleCode")
    void deleteByPersonAndRoleCode(@Param("personId") Long personId, @Param("roleCode") String roleCode);

    List<PersonRoleSkill> findByPersonIdAndRoleCode(Long personId, String roleCode);
}
