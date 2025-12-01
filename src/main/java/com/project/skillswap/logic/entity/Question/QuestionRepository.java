package com.project.skillswap.logic.entity.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio para gestión de preguntas de cuestionarios
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Busca todas las preguntas de un cuestionario ordenadas por número
     *
     * @param quizId ID del cuestionario
     * @return lista de preguntas ordenadas
     */
    @Query("SELECT q FROM Question q WHERE q.quiz.id = :quizId ORDER BY q.number ASC")
    List<Question> findByQuizIdOrderByNumber(@Param("quizId") Long quizId);

    /**
     * Cuenta las respuestas correctas de un cuestionario
     *
     * @param quizId ID del cuestionario
     * @return número de respuestas correctas
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.quiz.id = :quizId AND q.isCorrect = true")
    Long countCorrectAnswersByQuizId(@Param("quizId") Long quizId);
}