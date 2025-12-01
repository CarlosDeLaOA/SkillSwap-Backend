package com.project.skillswap.logic.entity.Feedback;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial feedbacks in the database
 */
@Order(8)
@Component
public class FeedbackSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final FeedbackRepository feedbackRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final LearnerRepository learnerRepository;
    //#endregion

    //#region Constructor
    public FeedbackSeeder(
            FeedbackRepository feedbackRepository,
            LearningSessionRepository learningSessionRepository,
            LearnerRepository learnerRepository) {
        this.feedbackRepository = feedbackRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.learnerRepository = learnerRepository;
    }
    //#endregion

    //#region Event Handling
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedFeedbacks();
    }
    //#endregion

    //#region Seeding Logic
    private void seedFeedbacks() {
        List<FeedbackData> feedbacksToCreate = createFeedbackDataList();

        for (FeedbackData feedbackData : feedbacksToCreate) {
            Optional<LearningSession> session = learningSessionRepository.findById(feedbackData.learningSessionId);
            Optional<Learner> learner = learnerRepository.findById(feedbackData.learnerId);

            if (session.isEmpty() || learner.isEmpty()) {
                continue;
            }

            Optional<Feedback> existingFeedback = feedbackRepository.findByLearningSessionAndLearner(
                    session.get(), learner.get()
            );

            if (existingFeedback.isPresent()) {
                continue;
            }

            Feedback feedback = createFeedback(feedbackData, session.get(), learner.get());
            feedbackRepository.save(feedback);
        }

    }

    private Feedback createFeedback(FeedbackData data, LearningSession session, Learner learner) {
        Feedback feedback = new Feedback();
        feedback.setLearningSession(session);
        feedback.setLearner(learner);
        feedback.setRating(data.rating);
        feedback.setComment(data.comment);
        return feedback;
    }

    /**
     * Creates the list of feedback data to be seeded
     */
    private List<FeedbackData> createFeedbackDataList() {
        List<FeedbackData> feedbacks = new ArrayList<>();

        feedbacks.add(new FeedbackData(1L, 2L, 5, "¡Excelente clase! Ella explica todo con mucha claridad y ejemplos reales."));
        feedbacks.add(new FeedbackData(2L, 2L, 5, "Ella es una gran mentora. Los ejercicios fueron muy útiles para aplicar lo aprendido."));
        feedbacks.add(new FeedbackData(6L, 2L, 4, "Buen contenido, pero el ritmo fue un poco rápido en algunos temas."));
        feedbacks.add(new FeedbackData(8L, 5L, 5, " Es increíble. Aprendí mucho sobre cultura japonesa además de caligrafía."));
        feedbacks.add(new FeedbackData(5L, 5L, 5, "¡El hace que aprender guitarra sea divertido! Recomendado 100%."));
        feedbacks.add(new FeedbackData(20L, 2L, 5, "El es un crack en ML. El proyecto final fue todo un desafío pero valió la pena."));
        feedbacks.add(new FeedbackData(30L, 2L, 5, "El instructor hace que aprender español sea súper práctico para viajar."));
        feedbacks.add(new FeedbackData(31L, 2L, 5, "La profesora domina JavaScript. Los ejemplos fueron perfectos."));
        feedbacks.add(new FeedbackData(32L, 5L, 5, "El curso de origami es arte puro. Aprendí mucho."));
        feedbacks.add(new FeedbackData(33L, 2L, 4, "Pandas es complejo, pero el instructor lo explicó bien. Falta más práctica."));
        feedbacks.add(new FeedbackData(34L, 8L, 5, "Aprendí a tocar mi primer solo. ¡Increíble experiencia!"));
        feedbacks.add(new FeedbackData(35L, 13L, 5, "El profesor optimizó mis consultas en 5 minutos. ¡Un genio!"));
        feedbacks.add(new FeedbackData(36L, 2L, 5, "La profesora hace que las matemáticas sean interesantes. ¡Gran clase!"));
        feedbacks.add(new FeedbackData(7L, 15L, 4, "Pinyin es difícil, pero el instructor fue paciente. Necesito más práctica."));
        feedbacks.add(new FeedbackData(38L, 17L, 5, "El curso de Figma fue excelente. ¡Proyecto final increíble!"));
        feedbacks.add(new FeedbackData(39L, 20L, 5, "El instructor transmite paz en sus clases de yoga. ¡Recomendado!"));
        feedbacks.add(new FeedbackData(40L, 5L, 5, "Español de negocios: ¡prepárate para reuniones reales!"));
        feedbacks.add(new FeedbackData(41L, 8L, 4, "DOM avanzado fue intenso, pero valió la pena."));
        feedbacks.add(new FeedbackData(14L, 2L, 5, "El curso de Ikebana es meditación en movimiento."));
        feedbacks.add(new FeedbackData(43L, 13L, 5, "Seaborn nunca fue tan claro. ¡Excelente explicación!"));
        feedbacks.add(new FeedbackData(44L, 15L, 5, "Fingerstyle: ¡mi nueva pasión!"));
        feedbacks.add(new FeedbackData(12L, 17L, 5, "SQL para BI: ¡reportes profesionales en una clase!"));
        feedbacks.add(new FeedbackData(46L, 20L, 5, "Cálculo: ¡desafiante pero claro!"));
        feedbacks.add(new FeedbackData(47L, 2L, 4, "Mandarín conversacional es duro, pero el profesor motiva mucho."));
        feedbacks.add(new FeedbackData(23L, 5L, 5, "Branding: ¡mi marca tiene identidad ahora!"));
        feedbacks.add(new FeedbackData(49L, 8L, 5, "Vinyasa: ¡fluyo mejor que nunca!"));
        feedbacks.add(new FeedbackData(50L, 2L, 5, "La sesión fue muy práctica y útil para el día a día."));
        feedbacks.add(new FeedbackData(51L, 5L, 5, "El instructor explicó conceptos complejos con gran claridad."));
        feedbacks.add(new FeedbackData(20L, 8L, 5, "¡Aprendí a hacer pasta como en Italia! Excelente ritmo."));
        feedbacks.add(new FeedbackData(53L, 13L, 4, "Buen contenido, pero el tema fue un poco avanzado para principiantes."));
        feedbacks.add(new FeedbackData(54L, 15L, 5, "Las frases prácticas son justo lo que necesitaba para viajar."));
        feedbacks.add(new FeedbackData(55L, 17L, 5, "El método GTD cambió mi forma de organizarme. ¡Recomendado!"));
        feedbacks.add(new FeedbackData(20L, 20L, 4, "Muy técnico, ideal para programadores con experiencia."));
        feedbacks.add(new FeedbackData(57L, 2L, 5, "Los ejemplos de automatización fueron perfectos para aplicar en el trabajo."));
        feedbacks.add(new FeedbackData(58L, 5L, 5, "La explicación sobre jerarquía visual fue clara y visualmente atractiva."));
        feedbacks.add(new FeedbackData(59L, 9L, 5, "¡Sesión relajante y restauradora! Salí renovado."));
        feedbacks.add(new FeedbackData(60L, 9L, 5, "La técnica de larga exposición fue explicada paso a paso."));
        feedbacks.add(new FeedbackData(61L, 15L, 5, "El enfoque en capas y testing fue muy profesional."));
        feedbacks.add(new FeedbackData(62L, 9L, 5, "¡La pizza quedó perfecta! El horno hace la diferencia."));
        feedbacks.add(new FeedbackData(63L, 20L, 4, "Buenas estrategias, pero faltó más práctica en vivo."));
        feedbacks.add(new FeedbackData(64L, 2L, 5, "El vocabulario comercial es justo lo que necesito para reuniones."));
        feedbacks.add(new FeedbackData(65L, 5L, 5, "¡Perdí el miedo a hablar en público! Gran metodología."));
        feedbacks.add(new FeedbackData(66L, 8L, 5, "Los pipelines automáticos ahorran horas de trabajo."));
        feedbacks.add(new FeedbackData(67L, 13L, 5, "Análisis financiero con Python: ¡nivel profesional!"));
        feedbacks.add(new FeedbackData(68L, 15L, 5, "Las animaciones quedaron espectaculares. ¡Gran clase!"));
        feedbacks.add(new FeedbackData(69L, 17L, 5, "¡Estiramientos en silla que salvan el día! Ideal para oficina."));
        return feedbacks;
    }
    //#endregion

    //#region Inner Class
    private static class FeedbackData {
        Long learningSessionId;
        Long learnerId;
        Integer rating;
        String comment;

        FeedbackData(Long learningSessionId, Long learnerId, Integer rating, String comment) {
            this.learningSessionId = learningSessionId;
            this.learnerId = learnerId;
            this.rating = rating;
            this.comment = comment;
        }
    }
    //#endregion
}
