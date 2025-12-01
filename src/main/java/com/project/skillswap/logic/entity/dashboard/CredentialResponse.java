package com.project.skillswap.logic.entity.dashboard;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.Date;

/**
 * Response containing credential information for learners
 */
public class CredentialResponse {
    //#region Fields
    private Long id;
    private String skillName;
    private Double percentageAchieved;
    private String badgeUrl;
    private Date obtainedDate;
    //#endregion

    //#region Constructors
    public CredentialResponse() {}
    //#endregion

    //#region Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public Double getPercentageAchieved() {
        return percentageAchieved;
    }

    public void setPercentageAchieved(Double percentageAchieved) {
        this.percentageAchieved = percentageAchieved;
    }

    public String getBadgeUrl() {
        return badgeUrl;
    }

    public void setBadgeUrl(String badgeUrl) {
        this.badgeUrl = badgeUrl;
    }

    public Date getObtainedDate() {
        return obtainedDate;
    }

    public void setObtainedDate(Date obtainedDate) {
        this.obtainedDate = obtainedDate;
    }
    //#endregion
}