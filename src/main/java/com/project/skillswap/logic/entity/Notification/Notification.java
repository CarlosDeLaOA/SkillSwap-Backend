
package com.project.skillswap.logic.entity.Notification;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_person", columnList = "person_id"),
        @Index(name = "idx_notification_person_read", columnList = "person_id, is_read"),
        @Index(name = "idx_notification_type", columnList = "type")
})
@Entity
public class Notification {
    private static final Logger logger = LoggerFactory.getLogger(Notification.class);

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read")
    private Boolean read = false;

    @CreationTimestamp
    @Column(updatable = false, name = "send_date")
    private Date sendDate;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Notification() {}
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

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Date getSendDate() {
        return sendDate;
    }

    public void setSendDate(Date sendDate) {
        this.sendDate = sendDate;
    }
    //</editor-fold>

    //<editor-fold desc="Helper Methods">
    /**
     * Obtiene la metadata del mensaje como Map (si el mensaje es JSON)
     */
    @Transient
    public Map<String, Object> getMetadata() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(this.message, Map.class);
        } catch (Exception e) {
            // Si no es JSON, retornar un Map con el mensaje como contenido
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("content", this.message);
            return fallback;
        }
    }

    /**
     * Establece la metadata del mensaje desde un Map
     */
    public void setMetadata(Map<String, Object> metadata) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.message = mapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            this.message = metadata.getOrDefault("content", "").toString();
        }
    }

    /**
     * Obtiene el contenido legible del mensaje
     */
    @Transient
    public String getReadableContent() {
        try {
            Map<String, Object> metadata = getMetadata();
            return metadata.getOrDefault("content", this.message).toString();
        } catch (Exception e) {
            return this.message;
        }
    }

    /**
     * Obtiene el tipo de evento (si está en metadata)
     */
    @Transient
    public String getEventType() {
        try {
            Map<String, Object> metadata = getMetadata();
            return metadata.getOrDefault("eventType", "GENERAL").toString();
        } catch (Exception e) {
            return "GENERAL";
        }
    }
    //</editor-fold>
}