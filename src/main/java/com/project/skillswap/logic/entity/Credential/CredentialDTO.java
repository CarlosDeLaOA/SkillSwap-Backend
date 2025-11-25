package com.project.skillswap.logic.entity.Credential;

import java.math.BigDecimal;
import java.util.Date;

public class CredentialDTO {
    private Long id;
    private LearnerDTO learner;
    private SkillDTO skill;
    private BigDecimal percentageAchieved;
    private String badgeUrl;
    private Date obtainedDate;

    // Constructors
    public CredentialDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LearnerDTO getLearner() { return learner; }
    public void setLearner(LearnerDTO learner) { this.learner = learner; }

    public SkillDTO getSkill() { return skill; }
    public void setSkill(SkillDTO skill) { this.skill = skill; }

    public BigDecimal getPercentageAchieved() { return percentageAchieved; }
    public void setPercentageAchieved(BigDecimal percentageAchieved) { this.percentageAchieved = percentageAchieved; }

    public String getBadgeUrl() { return badgeUrl; }
    public void setBadgeUrl(String badgeUrl) { this.badgeUrl = badgeUrl; }

    public Date getObtainedDate() { return obtainedDate; }
    public void setObtainedDate(Date obtainedDate) { this.obtainedDate = obtainedDate; }

    // Inner DTOs
    public static class LearnerDTO {
        private Long id;
        private PersonDTO person;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public PersonDTO getPerson() { return person; }
        public void setPerson(PersonDTO person) { this.person = person; }
    }

    public static class PersonDTO {
        private Long id;
        private String fullName;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }

    public static class SkillDTO {
        private Long id;
        private String name;
        private String description;
        private KnowledgeAreaDTO knowledgeArea;
        private Boolean active;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public KnowledgeAreaDTO getKnowledgeArea() { return knowledgeArea; }
        public void setKnowledgeArea(KnowledgeAreaDTO knowledgeArea) { this.knowledgeArea = knowledgeArea; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class KnowledgeAreaDTO {
        private Long id;
        private String name;
        private String description;
        private String iconUrl;
        private Boolean active;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getIconUrl() { return iconUrl; }
        public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }
}