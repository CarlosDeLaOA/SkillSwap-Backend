
package com.project.skillswap.logic.entity.WeeklyReport;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * Entidad que representa un reporte semanal enviado a un usuario.
 */
@Table(name = "weekly_report", indexes = {
        @Index(name = "idx_weekly_report_person", columnList = "person_id"),
        @Index(name = "idx_weekly_report_date", columnList = "report_date"),
        @Index(name = "idx_weekly_report_week", columnList = "week_start_date, week_end_date")
})
@Entity
public class WeeklyReport {
    private static final Logger logger = LoggerFactory.getLogger(WeeklyReport.class);

    //#region Fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
    private Person person;

    @Column(name = "week_start_date", nullable = false)
    private Date weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private Date weekEndDate;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    @Column(name = "credentials_obtained")
    private Integer credentialsObtained;

    @Column(name = "sessions_attended")
    private Integer sessionsAttended;

    @Column(name = "skillcoins_invested")
    private Integer skillcoinsInvested;

    @Column(name = "sessions_taught")
    private Integer sessionsTaught;

    @Column(name = "total_hours_taught")
    private Integer totalHoursTaught;

    @Column(name = "skillcoins_earned")
    private Integer skillcoinsEarned;

    @Column(name = "average_rating_week")
    private Double averageRatingWeek;

    @Column(name = "total_reviews_received")
    private Integer totalReviewsReceived;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @CreationTimestamp
    @Column(updatable = false, name = "report_date")
    private Date reportDate;
    //#endregion

    //#region Constructors
    public WeeklyReport() {}
    //#endregion

    //#region Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Date getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(Date weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public Date getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(Date weekEndDate) {
        this.weekEndDate = weekEndDate;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Integer getCredentialsObtained() {
        return credentialsObtained;
    }

    public void setCredentialsObtained(Integer credentialsObtained) {
        this.credentialsObtained = credentialsObtained;
    }

    public Integer getSessionsAttended() {
        return sessionsAttended;
    }

    public void setSessionsAttended(Integer sessionsAttended) {
        this.sessionsAttended = sessionsAttended;
    }

    public Integer getSkillcoinsInvested() {
        return skillcoinsInvested;
    }

    public void setSkillcoinsInvested(Integer skillcoinsInvested) {
        this.skillcoinsInvested = skillcoinsInvested;
    }

    public Integer getSessionsTaught() {
        return sessionsTaught;
    }

    public void setSessionsTaught(Integer sessionsTaught) {
        this.sessionsTaught = sessionsTaught;
    }

    public Integer getTotalHoursTaught() {
        return totalHoursTaught;
    }

    public void setTotalHoursTaught(Integer totalHoursTaught) {
        this.totalHoursTaught = totalHoursTaught;
    }

    public Integer getSkillcoinsEarned() {
        return skillcoinsEarned;
    }

    public void setSkillcoinsEarned(Integer skillcoinsEarned) {
        this.skillcoinsEarned = skillcoinsEarned;
    }

    public Double getAverageRatingWeek() {
        return averageRatingWeek;
    }

    public void setAverageRatingWeek(Double averageRatingWeek) {
        this.averageRatingWeek = averageRatingWeek;
    }

    public Integer getTotalReviewsReceived() {
        return totalReviewsReceived;
    }

    public void setTotalReviewsReceived(Integer totalReviewsReceived) {
        this.totalReviewsReceived = totalReviewsReceived;
    }

    public Boolean getEmailSent() {
        return emailSent;
    }

    public void setEmailSent(Boolean emailSent) {
        this.emailSent = emailSent;
    }

    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
    }
    //#endregion
}