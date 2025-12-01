package com.project.skillswap.logic.entity.dashboard;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
public class MonthlyAttendanceResponse {

    private String month;
    private Integer presentes;
    private Integer registrados;

    public MonthlyAttendanceResponse() {
    }

    public MonthlyAttendanceResponse(String month, Integer presentes, Integer registrados) {
        this.month = month;
        this.presentes = presentes;
        this.registrados = registrados;
    }

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
}