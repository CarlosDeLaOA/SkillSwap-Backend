package com.project.skillswap.logic.entity.LearningSession;

public class SessionUpdateRequest {

    private String description;
    private Integer durationMinutes;

    public SessionUpdateRequest() {}

    public SessionUpdateRequest(String description, Integer durationMinutes) {
        this.description = description;
        this.durationMinutes = durationMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}