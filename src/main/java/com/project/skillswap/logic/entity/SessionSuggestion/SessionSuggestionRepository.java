package com.project.skillswap.logic.entity.SessionSuggestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar sugerencias de sesiones
 */
@Repository
public interface SessionSuggestionRepository extends JpaRepository<SessionSuggestion, Long> {

    //#region Query Methods

    /**
     * Obtiene las mejores sugerencias no vistas para una persona
     * ordenadas por score descendente (máximo 5)
     *
     * @param personId ID de la persona
     * @return Lista de sugerencias no vistas
     */
    @Query("SELECT ss FROM SessionSuggestion ss " +
            "WHERE ss.person.id = :personId " +
            "AND ss.viewed = false " +
            "ORDER BY ss.matchScore DESC")
    List<SessionSuggestion> findTop5UnviewedSuggestions(@Param("personId") Long personId);

    /**
     * Obtiene todas las sugerencias de una persona
     *
     * @param personId ID de la persona
     * @return Lista de todas las sugerencias
     */
    List<SessionSuggestion> findByPersonId(Long personId);

    /**
     * Verifica si ya existe una sugerencia para una persona y sesión
     *
     * @param personId ID de la persona
     * @param sessionId ID de la sesión
     * @return Optional con la sugerencia si existe
     */
    Optional<SessionSuggestion> findByPersonIdAndLearningSessionId(
            @Param("personId") Long personId,
            @Param("sessionId") Long sessionId);

    /**
     * Cuenta sugerencias vistas/no vistas de una persona
     *
     * @param personId ID de la persona
     * @param viewed Estado de visualización
     * @return Cantidad de sugerencias
     */
    long countByPersonIdAndViewed(Long personId, Boolean viewed);

    //#endregion
}