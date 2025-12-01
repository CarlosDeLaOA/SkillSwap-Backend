package com.project.skillswap.logic.entity.dashboard;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
public class SkillSessionStatsResponse {
    private static final Logger logger = LoggerFactory.getLogger(SkillSessionStatsResponse.class);

    private String skillName;
    private Integer completed;
    private Integer pending;

    public SkillSessionStatsResponse() {
    }

    public SkillSessionStatsResponse(String skillName, Integer completed, Integer pending) {
        this.skillName = skillName;
        this.completed = completed;
        this.pending = pending;
    }

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
}