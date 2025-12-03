package com.project.skillswap.logic.entity.Feedback;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Learner.Learner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {

    /**
     * Busca feedback por objetos completos
     */
    Optional<Feedback> findByLearningSessionAndLearner(LearningSession learningSession, Learner learner);

    /**
     * Busca todos los feedbacks de una sesión por ID
     */
    List<Feedback> findByLearningSessionId(Long sessionId);

    /**
     * Obtiene feedbacks paginados para un instructor específico
     * Los feedbacks se obtienen de las sesiones que el instructor ha creado
     * @param instructorId ID del instructor
     * @param pageable Paginación y ordenamiento
     * @return Página de feedbacks ordenados
     */
    @Query(value = "SELECT f FROM Feedback f " +
            "JOIN f.learningSession ls " +
            "WHERE ls.instructor.id = :instructorId " +
            "ORDER BY f.creationDate DESC",
            countQuery = "SELECT COUNT(f) FROM Feedback f " +
                    "JOIN f.learningSession ls " +
                    "WHERE ls.instructor.id = :instructorId")
    Page<Feedback> findFeedbacksByInstructorId(
            @Param("instructorId") Long instructorId,
            Pageable pageable);

    /**
     * Obtiene los feedbacks más recientes de un instructor (sin paginación)
     * @param instructorId ID del instructor
     * @param limit Cantidad máxima de feedbacks a retornar
     * @return Lista de feedbacks recientes
     */
    @Query(value = "SELECT f FROM Feedback f " +
            "JOIN f.learningSession ls " +
            "WHERE ls.instructor.id = :instructorId " +
            "ORDER BY f.creationDate DESC " +
            "LIMIT :limit")
    List<Feedback> findRecentFeedbacksByInstructorId(
            @Param("instructorId") Long instructorId,
            @Param("limit") int limit);

    /**
     * Cuenta el total de feedbacks de un instructor
     * @param instructorId ID del instructor
     * @return Total de feedbacks
     */
    @Query("SELECT COUNT(f) FROM Feedback f " +
            "JOIN f.learningSession ls " +
            "WHERE ls.instructor.id = :instructorId")
    Long countFeedbacksByInstructorId(@Param("instructorId") Long instructorId);

    /**
     * Obtiene el promedio de calificación de un instructor
     * @param instructorId ID del instructor
     * @return Promedio de calificación (puede ser null si no hay feedbacks)
     */
    @Query("SELECT AVG(f.rating) FROM Feedback f " +
            "JOIN f.learningSession ls " +
            "WHERE ls.instructor.id = :instructorId")
    Double getAverageRatingByInstructorId(@Param("instructorId") Long instructorId);

    /**
     * Cuenta feedbacks por rating específico de un instructor
     * @param instructorId ID del instructor
     * @param rating Calificación a contar (1-5)
     * @return Total de feedbacks con esa calificación
     */
    @Query("SELECT COUNT(f) FROM Feedback f " +
            "JOIN f.learningSession ls " +
            "WHERE ls.instructor.id = :instructorId " +
            "AND f.rating = :rating")
    Long countFeedbacksByRatingAndInstructor(
            @Param("instructorId") Long instructorId,
            @Param("rating") Integer rating);

}