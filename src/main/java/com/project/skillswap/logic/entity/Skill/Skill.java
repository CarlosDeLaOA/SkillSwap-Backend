package com.project.skillswap.logic.entity.Skill;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.skillswap.logic.entity.KnowledgeArea.KnowledgeArea;
import jakarta.persistence.*;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(
        name = "skill",
        indexes = {
                @Index(name = "idx_skill_name", columnList = "name"),
                @Index(name = "idx_skill_knowledge_area", columnList = "knowledge_area_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uq_skill_area_name", columnNames = {"knowledge_area_id", "name"})
)
@Entity
public class Skill {

    //#region Fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @JsonIgnore // evita recursión
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_area_id", nullable = false)
    private KnowledgeArea knowledgeArea;
    //#endregion

    //#region Constructors
    public Skill() { }
    //#endregion

    //#region Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public KnowledgeArea getKnowledgeArea() { return knowledgeArea; }
    public void setKnowledgeArea(KnowledgeArea knowledgeArea) { this.knowledgeArea = knowledgeArea; }
    //#endregion
}
