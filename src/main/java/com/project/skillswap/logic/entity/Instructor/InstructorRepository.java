package com.project.skillswap.logic.entity.Instructor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorRepository extends JpaRepository<Instructor, Integer> {

    boolean existsByPaypalAccountAndIdNot(String paypalAccount, Integer id);
}