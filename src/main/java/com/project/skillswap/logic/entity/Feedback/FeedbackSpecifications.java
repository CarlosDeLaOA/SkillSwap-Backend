package com.project.skillswap.logic.entity.Feedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

/**
 * Especificaciones de Feedback para construir queries dinámicas con JPA Criteria API
 * Permite filtrado flexible y reutilizable de feedbacks
 *
 * Uso:
 * Specification<Feedback> spec = Specification
 *     .where(FeedbackSpecifications.byInstructorId(5L))
 *     .and(FeedbackSpecifications.byRatingGreaterThanOrEqual(4));
 * Page<Feedback> feedbacks = feedbackRepository.findAll(spec, pageable);
 */
public class FeedbackSpecifications {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackSpecifications.class);

    //#region Especificaciones por Instructor

    /**
     * Filtra feedbacks por ID del instructor
     * @param instructorId ID del instructor
     * @return Specification para filtrar por instructor
     */
    public static Specification<Feedback> byInstructorId(Long instructorId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("learningSession").get("instructor").get("id"), instructorId);
    }

    //#endregion

    //#region Especificaciones por Rating

    /**
     * Filtra feedbacks por rating exacto
     * @param rating Calificación exacta (1-5)
     * @return Specification para filtrar por rating
     */
    public static Specification<Feedback> byRating(Integer rating) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("rating"), rating);
    }

    /**
     * Filtra feedbacks con rating mayor o igual a un valor
     * @param minRating Calificación mínima
     * @return Specification para filtrar por rating mínimo
     */
    public static Specification<Feedback> byRatingGreaterThanOrEqual(Integer minRating) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    /**
     * Filtra feedbacks con rating menor o igual a un valor
     * @param maxRating Calificación máxima
     * @return Specification para filtrar por rating máximo
     */
    public static Specification<Feedback> byRatingLessThanOrEqual(Integer maxRating) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("rating"), maxRating);
    }

    /**
     * Filtra feedbacks con rating en un rango
     * @param minRating Calificación mínima
     * @param maxRating Calificación máxima
     * @return Specification para filtrar por rango de rating
     */
    public static Specification<Feedback> byRatingRange(Integer minRating, Integer maxRating) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("rating"), minRating, maxRating);
    }

    //#endregion

    //#region Especificaciones por Fecha

    /**
     * Filtra feedbacks creados después de una fecha específica
     * @param date Fecha de inicio
     * @return Specification para filtrar por fecha de creación posterior
     */
    public static Specification<Feedback> createdAfter(Date date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("creationDate"), date);
    }

    /**
     * Filtra feedbacks creados antes de una fecha específica
     * @param date Fecha de fin
     * @return Specification para filtrar por fecha de creación anterior
     */
    public static Specification<Feedback> createdBefore(Date date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("creationDate"), date);
    }

    /**
     * Filtra feedbacks creados en un rango de fechas
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Specification para filtrar por rango de fechas
     */
    public static Specification<Feedback> createdBetween(Date startDate, Date endDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("creationDate"), startDate, endDate);
    }

    //#endregion

    //#region Especificaciones por Contenido

    /**
     * Filtra feedbacks que tengan comentario (no nulo ni vacío)
     * @return Specification para feedbacks con comentario
     */
    public static Specification<Feedback> hasComment() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("comment")),
                        criteriaBuilder.notEqual(root.get("comment"), "")
                );
    }

    /**
     * Filtra feedbacks sin comentario (nulo o vacío)
     * @return Specification para feedbacks sin comentario
     */
    public static Specification<Feedback> withoutComment() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("comment")),
                        criteriaBuilder.equal(root.get("comment"), "")
                );
    }

    /**
     * Filtra feedbacks que contengan un texto específico en el comentario
     * @param searchText Texto a buscar
     * @return Specification para búsqueda por texto
     */
    public static Specification<Feedback> commentContains(String searchText) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("comment")),
                        "%" + searchText.toLowerCase() + "%"
                );
    }

    //#endregion

    //#region Especificaciones por Audio

    /**
     * Filtra feedbacks que tengan grabación de audio
     * @return Specification para feedbacks con audio
     */
    public static Specification<Feedback> hasAudio() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("audioUrl")),
                        criteriaBuilder.notEqual(root.get("audioUrl"), "")
                );
    }

    /**
     * Filtra feedbacks sin grabación de audio
     * @return Specification para feedbacks sin audio
     */
    public static Specification<Feedback> withoutAudio() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("audioUrl")),
                        criteriaBuilder.equal(root.get("audioUrl"), "")
                );
    }

    /**
     * Filtra feedbacks que tengan transcripción de audio
     * @return Specification para feedbacks con transcripción
     */
    public static Specification<Feedback> hasTranscription() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("audioTranscription")),
                        criteriaBuilder.notEqual(root.get("audioTranscription"), "")
                );
    }

    //#endregion

    //#region Especificaciones por Sesión

    /**
     * Filtra feedbacks de una sesión específica
     * @param sessionId ID de la sesión
     * @return Specification para filtrar por sesión
     */
    public static Specification<Feedback> bySessionId(Long sessionId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("learningSession").get("id"), sessionId);
    }

    /**
     * Filtra feedbacks de un alumno específico
     * @param learnerId ID del alumno
     * @return Specification para filtrar por alumno
     */
    public static Specification<Feedback> byLearnerId(Long learnerId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("learner").get("id"), learnerId);
    }

    //#endregion

    //#region Especificaciones de Ordenamiento

    /**
     * Ordena feedbacks por fecha de creación descendente (más recientes primero)
     * @return Specification para ordenamiento
     */
    public static Specification<Feedback> orderByCreationDateDesc() {
        return (root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.desc(root.get("creationDate")));
            return criteriaBuilder.conjunction();
        };
    }

    /**
     * Ordena feedbacks por fecha de creación ascendente (más antiguos primero)
     * @return Specification para ordenamiento
     */
    public static Specification<Feedback> orderByCreationDateAsc() {
        return (root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get("creationDate")));
            return criteriaBuilder.conjunction();
        };
    }

    /**
     * Ordena feedbacks por rating descendente (mejores primero)
     * @return Specification para ordenamiento
     */
    public static Specification<Feedback> orderByRatingDesc() {
        return (root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.desc(root.get("rating")));
            return criteriaBuilder.conjunction();
        };
    }

    /**
     * Ordena feedbacks por rating ascendente (peores primero)
     * @return Specification para ordenamiento
     */
    public static Specification<Feedback> orderByRatingAsc() {
        return (root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get("rating")));
            return criteriaBuilder.conjunction();
        };
    }

    //#endregion
}