package com.project.skillswap.logic.entity.PersonRoleSkill;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Skill.Skill;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "person_role_skill",
        uniqueConstraints = @UniqueConstraint(name = "uq_prs", columnNames = {"person_id","role_code","skill_id"})
)
public class PersonRoleSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode; // "INSTRUCTOR" | "LEARNER"

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // --- Getters/Setters ---
    public Long getId() { return id; }
    public Person getPerson() { return person; }
    public Skill getSkill() { return skill; }
    public String getRoleCode() { return roleCode; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setPerson(Person person) { this.person = person; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
