package com.project.skillswap.logic.entity.Skill;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import jakarta.persistence.*;

@Table(name = "skill", indexes = {
        @Index(name = "idx_skill_knowledge_area", columnList = "knowledge_area_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_skill_area_name", columnNames = {"knowledge_area_id", "name"})
})
@Entity
public class Skill {

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_area_id", referencedColumnName = "id", nullable = false)
    private KnowledgeArea knowledgeArea;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "active")
    private Boolean active = true;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Skill() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KnowledgeArea getKnowledgeArea() {
        return knowledgeArea;
    }

    public void setKnowledgeArea(KnowledgeArea knowledgeArea) {
        this.knowledgeArea = knowledgeArea;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
    //</editor-fold>
}