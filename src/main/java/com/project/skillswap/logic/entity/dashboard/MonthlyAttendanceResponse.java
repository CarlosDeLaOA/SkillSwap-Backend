package com.project.skillswap.logic.entity.dashboard;

/**
 * Response containing monthly attendance information for instructors
 */
public class MonthlyAttendanceResponse {

    //#region Fields
    private String month;
    private Integer presentes;  // Asistentes que llegaron
    private Integer registrados; // Total de registrados
    //#endregion

    //#region Constructors
    public MonthlyAttendanceResponse() {}

    public MonthlyAttendanceResponse(String month, Integer presentes, Integer registrados) {
        this.month = month;
        this.presentes = presentes;
        this.registrados = registrados;
    }
    //#endregion

    //#region Getters and Setters
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Integer getPresentes() {
        return presentes;
    }

    public void setPresentes(Integer presentes) {
        this.presentes = presentes;
    }

    public Integer getRegistrados() {
        return registrados;
    }

    public void setRegistrados(Integer registrados) {
        this.registrados = registrados;
    }
    //#endregion
}