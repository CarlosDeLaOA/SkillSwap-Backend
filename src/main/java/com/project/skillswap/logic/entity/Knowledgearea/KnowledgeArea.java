package com.project.skillswap.logic.entity.Knowledgearea;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.skillswap.logic.entity.Skill.Skill;
import jakarta.persistence.*;

import java.util.List;

@Table(name = "knowledge_area", indexes = {
        @Index(name = "idx_knowledge_area_name", columnList = "name")
})
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class KnowledgeArea {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeArea.class);

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "active")
    private Boolean active = true;

    @OneToMany(mappedBy = "knowledgeArea", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"knowledgeArea", "learningSessions"})
    private List<Skill> skills;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public KnowledgeArea() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }
    //</editor-fold>
}