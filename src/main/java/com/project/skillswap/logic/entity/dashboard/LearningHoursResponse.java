package com.project.skillswap.logic.entity.dashboard;

/**
 * Response containing total learning hours information
 */
public class LearningHoursResponse {

    //#region Fields
    private Integer totalMinutes;
    private Integer totalHours;
    private String role;
    //#endregion

    //#region Constructors
    public LearningHoursResponse() {}

    public LearningHoursResponse(Integer totalMinutes, String role) {
        this.totalMinutes = totalMinutes;
        this.totalHours = totalMinutes / 60;
        this.role = role;
    }
    //#endregion

    //#region Getters and Setters
    public Integer getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(Integer totalMinutes) {
        this.totalMinutes = totalMinutes;
        this.totalHours = totalMinutes / 60;
    }

    public Integer getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Integer totalHours) {
        this.totalHours = totalHours;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    //#endregion
}