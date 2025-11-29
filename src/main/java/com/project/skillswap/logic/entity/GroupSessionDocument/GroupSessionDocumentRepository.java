package com.project.skillswap.logic.entity.GroupSessionDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para gestionar documentos de sesiones grupales.
 * Incluye queries para organizar por fecha/sesión en orden descendente.
 */
@Repository
public interface GroupSessionDocumentRepository extends JpaRepository<GroupSessionDocument, Long> {

    //#region Queries por Comunidad

    /**
     * Obtiene todos los documentos activos de una comunidad ordenados por fecha de sesión descendente.
     *
     * @param communityId ID de la comunidad
     * @return lista de documentos ordenados por fecha descendente
     */
    @Query("SELECT gsd FROM GroupSessionDocument gsd " +
            "WHERE gsd.learningCommunity.id = :communityId " +
            "AND gsd.active = true " +
            "ORDER BY gsd.sessionDate DESC, gsd.uploadDate DESC")
    List<GroupSessionDocument> findByCommunityIdOrderBySessionDateDesc(@Param("communityId") Long communityId);

    /**
     * Obtiene documentos de una comunidad filtrados por rango de fechas.
     *
     * @param communityId ID de la comunidad
     * @param startDate fecha inicio
     * @param endDate fecha fin
     * @return lista de documentos en el rango
     */
    @Query("SELECT gsd FROM GroupSessionDocument gsd " +
            "WHERE gsd.learningCommunity.id = :communityId " +
            "AND gsd.active = true " +
            "AND gsd.sessionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY gsd.sessionDate DESC, gsd.uploadDate DESC")
    List<GroupSessionDocument> findByCommunityIdAndDateRange(
            @Param("communityId") Long communityId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    //#endregion

    //#region Queries por Sesión

    /**
     * Obtiene todos los documentos activos de una sesión específica.
     *
     * @param sessionId ID de la sesión
     * @return lista de documentos de la sesión
     */
    @Query("SELECT gsd FROM GroupSessionDocument gsd " +
            "WHERE gsd.learningSession.id = :sessionId " +
            "AND gsd.active = true " +
            "ORDER BY gsd.uploadDate DESC")
    List<GroupSessionDocument> findBySessionIdOrderByUploadDateDesc(@Param("sessionId") Long sessionId);

    /**
     * Obtiene documentos por comunidad y sesión.
     *
     * @param communityId ID de la comunidad
     * @param sessionId ID de la sesión
     * @return lista de documentos
     */
    @Query("SELECT gsd FROM GroupSessionDocument gsd " +
            "WHERE gsd.learningCommunity.id = :communityId " +
            "AND gsd.learningSession.id = :sessionId " +
            "AND gsd.active = true " +
            "ORDER BY gsd.uploadDate DESC")
    List<GroupSessionDocument> findByCommunityIdAndSessionId(
            @Param("communityId") Long communityId,
            @Param("sessionId") Long sessionId);

    //#endregion

    //#region Queries de Almacenamiento

    /**
     * Calcula el tamaño total de almacenamiento usado por una comunidad.
     *
     * @param communityId ID de la comunidad
     * @return tamaño total en bytes
     */
    @Query("SELECT COALESCE(SUM(gsd.fileSize), 0) FROM GroupSessionDocument gsd " +
            "WHERE gsd.learningCommunity.id = :communityId " +
            "AND gsd.active = true")
    Long getTotalStorageByCommunityId(@Param("communityId") Long communityId);

    /**
     * Cuenta el número de documentos activos de una comunidad.
     *
     * @param communityId ID de la comunidad
     * @return número de documentos
     */
    @Query("SELECT COUNT(gsd) FROM GroupSessionDocument gsd " +
            "WHERE gsd.learningCommunity.id = :communityId " +
            "AND gsd.active = true")
    Long countDocumentsByCommunityId(@Param("communityId") Long communityId);

    //#endregion

    //#region Queries de Validación

    /**
     * Verifica si existe un documento con el mismo nombre en la misma sesión.
     *
     * @param sessionId ID de la sesión
     * @param originalFileName nombre original del archivo
     * @return true si existe
     */
    @Query("SELECT CASE WHEN COUNT(gsd) > 0 THEN true ELSE false END " +
            "FROM GroupSessionDocument gsd " +
            "WHERE gsd.learningSession.id = :sessionId " +
            "AND gsd.originalFileName = :originalFileName " +
            "AND gsd.active = true")
    boolean existsBySessionIdAndOriginalFileName(
            @Param("sessionId") Long sessionId,
            @Param("originalFileName") String originalFileName);

    //#endregion

    //#region Queries Agrupadas

    /**
     * Obtiene lista de sesiones con documentos para una comunidad (para organizar por sesión).
     *
     * @param communityId ID de la comunidad
     * @return lista de IDs de sesiones únicas
     */
    @Query("SELECT DISTINCT gsd.learningSession.id FROM GroupSessionDocument gsd " +
            "WHERE gsd.learningCommunity.id = :communityId " +
            "AND gsd.active = true " +
            "ORDER BY gsd.learningSession.id DESC")
    List<Long> findDistinctSessionIdsByCommunityId(@Param("communityId") Long communityId);

    /**
     * Obtiene lista de fechas únicas de sesiones para una comunidad.
     *
     * @param communityId ID de la comunidad
     * @return lista de fechas únicas
     */
    @Query("SELECT DISTINCT CAST(gsd.sessionDate AS date) FROM GroupSessionDocument gsd " +
            "WHERE gsd.learningCommunity.id = :communityId " +
            "AND gsd.active = true " +
            "ORDER BY CAST(gsd.sessionDate AS date) DESC")
    List<java.sql.Date> findDistinctSessionDatesByCommunityId(@Param("communityId") Long communityId);

    //  Query para obtener solo material de apoyo (sin sesión)
    @Query("SELECT d FROM GroupSessionDocument d WHERE d.learningCommunity.id = :communityId AND d.learningSession IS NULL AND d.active = true ORDER BY d.uploadDate DESC")
    List<GroupSessionDocument> findSupportMaterialsByCommunityId(@Param("communityId") Long communityId);

    //  Query para documentos borrados
    @Query("SELECT d FROM GroupSessionDocument d WHERE d.learningCommunity.id = :communityId AND d.active = false ORDER BY d.deletedAt DESC")
    List<GroupSessionDocument> findDeletedByCommunityId(@Param("communityId") Long communityId);

    //#endregion
}