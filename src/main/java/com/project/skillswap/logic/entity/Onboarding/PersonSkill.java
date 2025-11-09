package com.project.skillswap.logic.entity.Onboarding;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Skill.Skill;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "person_skill",
        uniqueConstraints = @UniqueConstraint(name = "uq_person_skill", columnNames = {"person_id", "skill_id"})
)
public class PersonSkill {
    //#region Fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    //#endregion

    //#region Constructors
    public PersonSkill() { }

    public PersonSkill(Person person, Skill skill) {
        this.person = person;
        this.skill = skill;
    }
    //#endregion

    //#region Getters/Setters
    public Long getId() { return id; }
    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public Instant getCreatedAt() { return createdAt; }
    //#endregion
}
