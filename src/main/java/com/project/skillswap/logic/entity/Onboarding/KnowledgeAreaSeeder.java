package com.project.skillswap.logic.entity.Onboarding;

import com.project.skillswap.logic.entity.KnowledgeArea.KnowledgeArea;
import com.project.skillswap.logic.entity.KnowledgeArea.KnowledgeAreaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeAreaSeeder implements CommandLineRunner {

    private final KnowledgeAreaRepository repo;

    public KnowledgeAreaSeeder(KnowledgeAreaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        List<String> categories = List.of(
                "Tecnología",
                "Arte",
                "Deportes",
                "Idiomas",
                "Música",
                "Cocina",
                "Ciencias",
                "Negocios",
                "Salud & Fitness",
                "Fotografía",
                "Historia",
                "Diseño"
        );

        for (String name : categories) {
            repo.findByNameIgnoreCase(name).orElseGet(() -> {
                KnowledgeArea ka = new KnowledgeArea();
                ka.setName(name);
                ka.setDescription(null);
                return repo.save(ka);
            });
        }
    }
}
