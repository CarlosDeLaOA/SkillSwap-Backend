package com.project.skillswap.logic.entity.dashboard;

import java.util.Date;

/**
 * Response containing feedback information for instructors
 */
public class FeedbackResponse {

    //#region Fields
    private Long id;
    private Integer rating;
    private String comment;
    private Date creationDate;
    private String learnerName;
    private String sessionTitle;
    //#endregion

    //#region Constructors
    public FeedbackResponse() {}
    //#endregion

    //#region Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getLearnerName() {
        return learnerName;
    }

    public void setLearnerName(String learnerName) {
        this.learnerName = learnerName;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }
    //#endregion
}