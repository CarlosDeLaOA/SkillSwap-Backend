package com.project.skillswap.logic.entity.Knowledgearea;

import com.project.skillswap.logic.entity.Skill.Skill;
import jakarta.persistence.*;

// NUEVO
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Table(name = "knowledge_area", indexes = {
        @Index(name = "idx_knowledge_area_name", columnList = "name")
})
@Entity
public class KnowledgeArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "knowledgeArea",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonIgnore // NUEVO: evita LazyInitializationException/recursión al serializar
    private List<Skill> skills;

    // ===== Getters/Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<Skill> getSkills() { return skills; }
    public void setSkills(List<Skill> skills) { this.skills = skills; }
}
