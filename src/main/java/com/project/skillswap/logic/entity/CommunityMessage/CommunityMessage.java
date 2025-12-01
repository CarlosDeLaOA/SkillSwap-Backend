package com.project.skillswap.logic.entity.CommunityMessage;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * Entidad que representa un mensaje en el chat de una comunidad.
 */
@Table(name = "community_message", indexes = {
        @Index(name = "idx_community_message_community", columnList = "learning_community_id"),
        @Index(name = "idx_community_message_sender", columnList = "sender_id"),
        @Index(name = "idx_community_message_date", columnList = "sent_date")
})
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CommunityMessage {
    private static final Logger logger = LoggerFactory.getLogger(CommunityMessage.class);

    //#region Fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_community_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"members", "documents", "bookings", "creator", "hibernateLazyInitializer", "handler"})
    private LearningCommunity learningCommunity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "googleOauthId", "emailVerified", "instructor", "learner", "transactions", "notifications", "weeklyReports", "userSkills", "hibernateLazyInitializer", "handler"})
    private Person sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(updatable = false, name = "sent_date")
    private Date sentDate;

    @Column(name = "edited")
    private Boolean edited = false;
    //#endregion

    //#region Constructors
    public CommunityMessage() {}
    //#endregion

    //#region Getters and Setters
    /**
     * Obtiene el ID del mensaje.
     *
     * @return ID del mensaje
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el ID del mensaje.
     *
     * @param id ID del mensaje
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene la comunidad a la que pertenece el mensaje.
     *
     * @return comunidad del mensaje
     */
    public LearningCommunity getLearningCommunity() {
        return learningCommunity;
    }

    /**
     * Establece la comunidad a la que pertenece el mensaje.
     *
     * @param learningCommunity comunidad del mensaje
     */
    public void setLearningCommunity(LearningCommunity learningCommunity) {
        this.learningCommunity = learningCommunity;
    }

    /**
     * Obtiene la persona que envió el mensaje.
     *
     * @return persona que envió el mensaje
     */
    public Person getSender() {
        return sender;
    }

    /**
     * Establece la persona que envió el mensaje.
     *
     * @param sender persona que envió el mensaje
     */
    public void setSender(Person sender) {
        this.sender = sender;
    }

    /**
     * Obtiene el contenido del mensaje.
     *
     * @return contenido del mensaje
     */
    public String getContent() {
        return content;
    }

    /**
     * Establece el contenido del mensaje.
     *
     * @param content contenido del mensaje
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Obtiene la fecha de envío del mensaje.
     *
     * @return fecha de envío
     */
    public Date getSentDate() {
        return sentDate;
    }

    /**
     * Establece la fecha de envío del mensaje.
     *
     * @param sentDate fecha de envío
     */
    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    /**
     * Verifica si el mensaje fue editado.
     *
     * @return true si fue editado
     */
    public Boolean getEdited() {
        return edited;
    }

    /**
     * Establece si el mensaje fue editado.
     *
     * @param edited estado de edición
     */
    public void setEdited(Boolean edited) {
        this.edited = edited;
    }
    //#endregion
}