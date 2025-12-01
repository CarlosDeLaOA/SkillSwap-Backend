package com.project.skillswap.logic.entity.WeeklyReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.Optional;

/**
 * Repositorio para operaciones de WeeklyReport.
 */
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    /**
     * Busca un reporte semanal por persona y rango de fechas.
     *
     * @param person persona
     * @param weekStartDate fecha de inicio de semana
     * @param weekEndDate fecha de fin de semana
     * @return Optional con el reporte si existe
     */
    @Query("SELECT wr FROM WeeklyReport wr WHERE wr.person = :person AND wr.weekStartDate = :weekStartDate AND wr.weekEndDate = :weekEndDate")
    Optional<WeeklyReport> findByPersonAndWeekDates(
            @Param("person") Person person,
            @Param("weekStartDate") Date weekStartDate,
            @Param("weekEndDate") Date weekEndDate
    );

    /**
     * Elimina reportes antiguos anteriores a una fecha específica.
     *
     * @param date fecha límite
     */
    void deleteByReportDateBefore(Date date);
}