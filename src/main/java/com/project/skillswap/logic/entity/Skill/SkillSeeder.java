package com.project.skillswap.logic.entity.Skill;

import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeArea;
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Order(3)
@Component
public class SkillSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private final SkillRepository skillRepository;
    private final KnowledgeAreaRepository knowledgeAreaRepository;

    public SkillSeeder(SkillRepository skillRepository, KnowledgeAreaRepository knowledgeAreaRepository) {
        this.skillRepository = skillRepository;
        this.knowledgeAreaRepository = knowledgeAreaRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedSkills();
    }

    private void seedSkills() {
        List<SkillData> skillsToCreate = createSkillDataList();

        for (SkillData skillData : skillsToCreate) {
            Optional<KnowledgeArea> knowledgeArea = knowledgeAreaRepository.findByName(skillData.knowledgeAreaName);
            if (knowledgeArea.isEmpty()) continue;

            Optional<Skill> existingSkill = skillRepository.findByNameAndKnowledgeArea(
                    skillData.name, knowledgeArea.get());
            if (existingSkill.isPresent()) continue;

            Skill skill = new Skill();
            skill.setKnowledgeArea(knowledgeArea.get());
            skill.setName(skillData.name);
            skill.setDescription(skillData.description);
            skill.setActive(skillData.active);
            skillRepository.save(skill);
        }
    }

    private List<SkillData> createSkillDataList() {
        List<SkillData> skills = new ArrayList<>();

        skills.add(new SkillData("Cocina", "Cocina Italiana", "Preparación de pastas, pizzas y platos tradicionales italianos", true));
        skills.add(new SkillData("Cocina", "Repostería", "Elaboración de postres, pasteles y panadería", true));
        skills.add(new SkillData("Cocina", "Cocina Saludable", "Preparación de comidas nutritivas y balanceadas", true));
        skills.add(new SkillData("Cocina", "Cocina Internacional", "Recetas y técnicas de diferentes culturas", true));
        skills.add(new SkillData("Cocina", "Cocina Vegana", "Preparación de comidas sin productos animales", true));
        skills.add(new SkillData("Cocina", "Decoración de Platos", "Presentación estética y visual de comidas", true));
        skills.add(new SkillData("Cocina", "Cocina Japonesa", "Sushi, ramen y gastronomía japonesa tradicional", true));
        skills.add(new SkillData("Cocina", "Cocina Costarricense", "Platos típicos y sabores nacionales", true));
        skills.add(new SkillData("Cocina", "Cocina Mexicana", "Tacos, salsas y sabores picantes auténticos", true));
        skills.add(new SkillData("Cocina", "Barismo", "Preparación profesional de café y bebidas calientes", true));

        skills.add(new SkillData("Idiomas", "Inglés", "Comprensión, escritura y conversación en inglés", true));
        skills.add(new SkillData("Idiomas", "Español", "Dominio del idioma español y gramática avanzada", true));
        skills.add(new SkillData("Idiomas", "Francés", "Lengua francesa y cultura francófona", true));
        skills.add(new SkillData("Idiomas", "Alemán", "Aprendizaje del idioma alemán", true));
        skills.add(new SkillData("Idiomas", "Italiano", "Lengua italiana y comunicación básica", true));
        skills.add(new SkillData("Idiomas", "Portugués", "Portugués conversacional y gramatical", true));
        skills.add(new SkillData("Idiomas", "Mandarín", "Lectura y pronunciación del idioma chino mandarín", true));
        skills.add(new SkillData("Idiomas", "Lengua de Señas", "Comunicación inclusiva con lenguaje de señas", true));
        skills.add(new SkillData("Idiomas", "Japonés", "Hiragana, katakana y expresiones básicas", true));
        skills.add(new SkillData("Idiomas", "Coreano", "Gramática y escritura del idioma coreano", true));

        skills.add(new SkillData("Programación", "Java", "Programación orientada a objetos en Java", true));
        skills.add(new SkillData("Programación", "Python", "Desarrollo con Python desde lo básico hasta avanzado", true));
        skills.add(new SkillData("Programación", "JavaScript", "Desarrollo web moderno con JavaScript", true));
        skills.add(new SkillData("Programación", "HTML y CSS", "Diseño y estructura de sitios web", true));
        skills.add(new SkillData("Programación", "React", "Desarrollo de interfaces dinámicas con React.js", true));
        skills.add(new SkillData("Programación", "Angular", "Framework de desarrollo web con Angular", true));
        skills.add(new SkillData("Programación", "C#", "Desarrollo en C# y entorno .NET", true));
        skills.add(new SkillData("Programación", "SQL", "Gestión y consulta de bases de datos relacionales", true));
        skills.add(new SkillData("Programación", "Desarrollo Móvil", "Creación de apps con Android e iOS", true));
        skills.add(new SkillData("Programación", "Git y GitHub", "Control de versiones y colaboración en proyectos", true));

        skills.add(new SkillData("Deportes", "Fútbol", "Técnicas, reglas y entrenamiento en fútbol", true));
        skills.add(new SkillData("Deportes", "Natación", "Técnicas de nado y acondicionamiento físico", true));
        skills.add(new SkillData("Deportes", "Yoga", "Prácticas de respiración, equilibrio y relajación", true));
        skills.add(new SkillData("Deportes", "Ciclismo", "Entrenamiento y mantenimiento de bicicletas", true));
        skills.add(new SkillData("Deportes", "Atletismo", "Carreras, resistencia y velocidad", true));
        skills.add(new SkillData("Deportes", "Básquetbol", "Tácticas y fundamentos del baloncesto", true));
        skills.add(new SkillData("Deportes", "Voleibol", "Trabajo en equipo y técnicas de saque y bloqueo", true));
        skills.add(new SkillData("Deportes", "Gimnasia", "Equilibrio, fuerza y flexibilidad corporal", true));
        skills.add(new SkillData("Deportes", "Artes Marciales", "Defensa personal y disciplina física", true));
        skills.add(new SkillData("Deportes", "Entrenamiento Funcional", "Ejercicios para mejorar fuerza y movilidad", true));

        skills.add(new SkillData("Arte", "Pintura", "Técnicas con acrílico, óleo y acuarela", true));
        skills.add(new SkillData("Arte", "Dibujo", "Sombras, perspectiva y creatividad artística", true));
        skills.add(new SkillData("Arte", "Fotografía", "Composición, luz y retoque digital", true));
        skills.add(new SkillData("Arte", "Escultura", "Modelado con arcilla, madera y piedra", true));
        skills.add(new SkillData("Arte", "Cine", "Guion, producción y dirección audiovisual", true));
        skills.add(new SkillData("Arte", "Música", "Teoría musical e interpretación instrumental", true));
        skills.add(new SkillData("Arte", "Teatro", "Actuación, improvisación y expresión corporal", true));
        skills.add(new SkillData("Arte", "Diseño Gráfico", "Creación visual y comunicación digital", true));
        skills.add(new SkillData("Arte", "Bailes Latinos", "Salsa, bachata y ritmos caribeños", true));
        skills.add(new SkillData("Arte", "Artesanías", "Elaboración de objetos decorativos manuales", true));

        skills.add(new SkillData("Power Skills", "Comunicación Efectiva", "Expresión verbal y escucha activa", true));
        skills.add(new SkillData("Power Skills", "Liderazgo", "Gestión de equipos y toma de decisiones", true));
        skills.add(new SkillData("Power Skills", "Trabajo en Equipo", "Colaboración y sinergia entre compañeros", true));
        skills.add(new SkillData("Power Skills", "Gestión del Tiempo", "Organización y productividad personal", true));
        skills.add(new SkillData("Power Skills", "Pensamiento Crítico", "Análisis y resolución de problemas", true));
        skills.add(new SkillData("Power Skills", "Empatía", "Comprensión emocional y relaciones humanas", true));
        skills.add(new SkillData("Power Skills", "Negociación", "Técnicas de persuasión y acuerdos efectivos", true));
        skills.add(new SkillData("Power Skills", "Adaptabilidad", "Capacidad de afrontar cambios con flexibilidad", true));
        skills.add(new SkillData("Power Skills", "Gestión del Estrés", "Manejo emocional y equilibrio mental", true));
        skills.add(new SkillData("Power Skills", "Creatividad", "Innovación y pensamiento fuera de lo común", true));

        return skills;
    }

    private static class SkillData {
        String knowledgeAreaName;
        String name;
        String description;
        Boolean active;

        SkillData(String knowledgeAreaName, String name, String description, Boolean active) {
            this.knowledgeAreaName = knowledgeAreaName;
            this.name = name;
            this.description = description;
            this.active = active;
        }
    }
}
