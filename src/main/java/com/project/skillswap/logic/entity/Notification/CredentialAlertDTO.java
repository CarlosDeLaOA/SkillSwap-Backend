package com.project.skillswap.logic.entity.Notification;

/**
 * DTO para alertas de credenciales cercanas a certificado (8 o 9 de 10)
 */
public class CredentialAlertDTO {
    private Long learnerId;
    private String learnerName;
    private String learnerEmail;
    private Long skillId;
    private String skillName;
    private Long credentialCount;
    private Long remainingCredentials;

    public CredentialAlertDTO() {}

    public CredentialAlertDTO(Long learnerId, String learnerName, String learnerEmail,
                              Long skillId, String skillName, Long credentialCount) {
        this.learnerId = learnerId;
        this.learnerName = learnerName;
        this.learnerEmail = learnerEmail;
        this.skillId = skillId;
        this.skillName = skillName;
        this.credentialCount = credentialCount;
        this.remainingCredentials = 10 - credentialCount;
    }

    // Getters and Setters
    public Long getLearnerId() { return learnerId; }
    public void setLearnerId(Long learnerId) { this.learnerId = learnerId; }

    public String getLearnerName() { return learnerName; }
    public void setLearnerName(String learnerName) { this.learnerName = learnerName; }

    public String getLearnerEmail() { return learnerEmail; }
    public void setLearnerEmail(String learnerEmail) { this.learnerEmail = learnerEmail; }

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public Long getCredentialCount() { return credentialCount; }
    public void setCredentialCount(Long credentialCount) { this.credentialCount = credentialCount; }

    public Long getRemainingCredentials() { return remainingCredentials; }
    public void setRemainingCredentials(Long remainingCredentials) { this.remainingCredentials = remainingCredentials; }
}