package com.project.skillswap.logic.entity.Quiz;

import com.project.skillswap.logic.entity.Quiz.Quiz;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Skill.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findFirstByLearnerAndSkill(Learner learner, Skill skill);
}
