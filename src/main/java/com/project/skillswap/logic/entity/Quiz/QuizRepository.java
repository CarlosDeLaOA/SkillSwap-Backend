package com.project.skillswap.logic.entity.Quiz;

import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Skill.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión de cuestionarios
 */
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * Busca un cuestionario por learner y skill
     *
     * @param learner el aprendiz
     * @param skill la habilidad
     * @return Optional con el cuestionario si existe
     */
    Optional<Quiz> findFirstByLearnerAndSkill(Learner learner, Skill skill);

    /**
     * Busca todos los cuestionarios de un learner para una sesión específica
     *
     * @param learner el aprendiz
     * @param learningSession la sesión de aprendizaje
     * @return lista de cuestionarios
     */
    List<Quiz> findByLearnerAndLearningSession(Learner learner, LearningSession learningSession);

    /**
     * Busca el cuestionario más reciente de un learner para una sesión
     *
     * @param learner el aprendiz
     * @param learningSession la sesión de aprendizaje
     * @return Optional con el cuestionario más reciente
     */
    Optional<Quiz> findFirstByLearnerAndLearningSessionOrderByAttemptNumberDesc(
            Learner learner,
            LearningSession learningSession
    );

    /**
     * Cuenta el número de intentos de un learner para una sesión
     *
     * @param learnerId ID del aprendiz
     * @param learningSessionId ID de la sesión
     * @return número de intentos
     */
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.learner.id = :learnerId " +
            "AND q.learningSession.id = :learningSessionId")
    Long countAttemptsByLearnerAndSession(
            @Param("learnerId") Long learnerId,
            @Param("learningSessionId") Long learningSessionId
    );

    /**
     * Verifica si existe un cuestionario en progreso para un learner en una sesión
     *
     * @param learnerId ID del aprendiz
     * @param learningSessionId ID de la sesión
     * @return true si existe un cuestionario en progreso
     */
    @Query("SELECT CASE WHEN COUNT(q) > 0 THEN true ELSE false END FROM Quiz q " +
            "WHERE q.learner.id = :learnerId " +
            "AND q.learningSession.id = :learningSessionId " +
            "AND q.status = 'IN_PROGRESS'")
    boolean existsInProgressQuiz(
            @Param("learnerId") Long learnerId,
            @Param("learningSessionId") Long learningSessionId
    );
}