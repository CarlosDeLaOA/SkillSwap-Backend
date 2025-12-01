package com.project.skillswap.logic.entity.Learner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearnerRepository extends JpaRepository<Learner, Long> {
    Optional<Learner> findByPersonId(Long personId);
}