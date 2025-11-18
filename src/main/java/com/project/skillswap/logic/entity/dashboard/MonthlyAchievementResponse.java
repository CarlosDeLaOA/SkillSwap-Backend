package com.project.skillswap.logic.entity.dashboard;

public class MonthlyAchievementResponse {

    private String month;
    private Integer credentials;
    private Integer certificates;

    public MonthlyAchievementResponse() {
    }

    public MonthlyAchievementResponse(String month, Integer credentials, Integer certificates) {
        this.month = month;
        this.credentials = credentials;
        this.certificates = certificates;
    }

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
}