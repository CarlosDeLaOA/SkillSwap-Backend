
package com.project.skillswap.logic.entity.UserSkill;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Skill.Skill;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Table(name = "user_skill", indexes = {
        @Index(name = "idx_user_skill_person", columnList = "person_id"),
        @Index(name = "idx_user_skill_skill", columnList = "skill_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_user_skill_unique", columnNames = {"person_id", "skill_id"})
})
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserSkill {
    private static final Logger logger = LoggerFactory.getLogger(UserSkill.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"userSkills", "instructor", "learner", "transactions", "notifications", "weeklyReports", "passwordHash"})
    private Person person;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Skill skill;

    @Column(name = "selected_date", nullable = false)
    private LocalDateTime selectedDate;

    @Column(name = "active")
    private Boolean active = true;

    public UserSkill() {
        this.selectedDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public LocalDateTime getSelectedDate() { return selectedDate; }
    public void setSelectedDate(LocalDateTime selectedDate) { this.selectedDate = selectedDate; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}