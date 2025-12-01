package com.project.skillswap.logic.entity.Instructor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    boolean existsByPaypalAccountAndIdNot(String paypalAccount, Long id);

    default Optional<Instructor> findById(int id) {
        return findById((long) id);
    }


    default boolean existsById(int id) {
        return existsById((long) id);
    }
}