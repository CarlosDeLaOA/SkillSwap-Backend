package com.project.skillswap.logic.entity.dashboard;

/**
 * Response containing session statistics by skill
 */
public class SkillSessionStatsResponse {

    //#region Fields
    private String skillName;
    private Integer completed;
    private Integer pending;
    //#endregion

    //#region Constructors
    public SkillSessionStatsResponse() {}

    public SkillSessionStatsResponse(String skillName, Integer completed, Integer pending) {
        this.skillName = skillName;
        this.completed = completed;
        this.pending = pending;
    }
    //#endregion

    //#region Getters and Setters
    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public Integer getCompleted() {
        return completed;
    }

    public void setCompleted(Integer completed) {
        this.completed = completed;
    }

    public Integer getPending() {
        return pending;
    }

    public void setPending(Integer pending) {
        this.pending = pending;
    }
    //#endregion
}