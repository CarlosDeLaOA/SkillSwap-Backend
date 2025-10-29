package com.project.skillswap.logic.entity.WeeklyReport;

import com.project.skillswap.logic.entity.Person.Person;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Table(name = "weekly_report", indexes = {
        @Index(name = "idx_weekly_report_person", columnList = "person_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_weekly_report_person_week", columnNames = {"person_id", "week_start"})
})
@Entity
public class WeeklyReport {

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
    private Person person;

    @Column(name = "week_start", nullable = false)
    private Date weekStart;

    @Column(name = "week_end", nullable = false)
    private Date weekEnd;

    @Column(name = "total_sessions")
    private Integer totalSessions = 0;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @CreationTimestamp
    @Column(updatable = false, name = "generation_date")
    private Date generationDate;

    @Column(name = "sent")
    private Boolean sent = false;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public WeeklyReport() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
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

    public Date getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(Date weekStart) {
        this.weekStart = weekStart;
    }

    public Date getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(Date weekEnd) {
        this.weekEnd = weekEnd;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public Date getGenerationDate() {
        return generationDate;
    }

    public void setGenerationDate(Date generationDate) {
        this.generationDate = generationDate;
    }

    public Boolean getSent() {
        return sent;
    }

    public void setSent(Boolean sent) {
        this.sent = sent;
    }
    //</editor-fold>
}