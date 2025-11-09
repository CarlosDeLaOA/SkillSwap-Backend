package com.project.skillswap.logic.entity.dashboard;

/**
 * Response containing monthly achievements information
 */
public class MonthlyAchievementsResponse {

    //#region Fields
    private String month;
    private Integer credentials;
    private Integer certificates;
    //#endregion

    //#region Constructors
    public MonthlyAchievementsResponse() {}

    public MonthlyAchievementsResponse(String month, Integer credentials, Integer certificates) {
        this.month = month;
        this.credentials = credentials;
        this.certificates = certificates;
    }
    //#endregion

    //#region Getters and Setters
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Integer getCredentials() {
        return credentials;
    }

    public void setCredentials(Integer credentials) {
        this.credentials = credentials;
    }

    public Integer getCertificates() {
        return certificates;
    }

    public void setCertificates(Integer certificates) {
        this.certificates = certificates;
    }
    //#endregion
}