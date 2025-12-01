package com.project.skillswap.logic.entity.Transcription;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Order(8)
@Component
public class TranscriptionSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionSeeder.class);

    private final TranscriptionRepository transcriptionRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final Random random = new Random();

    public TranscriptionSeeder(TranscriptionRepository transcriptionRepository,
                               LearningSessionRepository learningSessionRepository) {
        this.transcriptionRepository = transcriptionRepository;
        this.learningSessionRepository = learningSessionRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (transcriptionRepository.count() > 0) {
            logger.info("TranscriptionSeeder: Ya existen transcripciones, omitiendo seed");
            return;
        }
        this.seedTranscriptions();
    }

    private void seedTranscriptions() {
        List<LearningSession> finishedSessions = new ArrayList<>();
        for (LearningSession s : learningSessionRepository.findAll()) {
            if (s.getStatus() == SessionStatus.FINISHED) {
                finishedSessions.add(s);
            }
        }

        Collections.shuffle(finishedSessions);

        if (finishedSessions.isEmpty()) {
            logger.warn("No hay sesiones finalizadas para crear transcripciones");
            return;
        }
        int transcriptionsToCreate = (int) (finishedSessions.size() * 0.7);
        Collections.shuffle(finishedSessions);

        List<LearningSession> sessionsToTranscribe = finishedSessions.subList(0,
                Math.min(transcriptionsToCreate, finishedSessions.size()));

        for (LearningSession session : sessionsToTranscribe) {
            Transcription transcription = createTranscription(session);
            transcriptionRepository.save(transcription);
        }

        logger.info("TranscriptionSeeder: " + sessionsToTranscribe.size() + " transcripciones creadas");
    }

    private Transcription createTranscription(LearningSession session) {
        Transcription transcription = new Transcription();

        transcription.setLearningSession(session);

        // Generar texto de transcripción simulado
        String transcriptionText = generateTranscriptionText(session);
        transcription.setFullText(transcriptionText);

        // Duración en segundos (basada en la duración de la sesión)
        int durationSeconds = session.getDurationMinutes() * 60;
        transcription.setDurationSeconds(durationSeconds);

        // Fecha de procesamiento (1-3 días después de la sesión)
        Calendar processingCal = Calendar.getInstance();
        processingCal.setTime(session.getScheduledDatetime());
        processingCal.add(Calendar.DAY_OF_MONTH, 1 + random.nextInt(3));
        transcription.setProcessingDate(processingCal.getTime());

        return transcription;
    }

    private String generateTranscriptionText(LearningSession session) {
        String[] introTemplates = {
                "Bienvenidos a esta sesión de {title}. Hoy vamos a explorar los conceptos fundamentales y prácticos.",
                "Hola a todos, gracias por unirse a {title}. Comencemos con una introducción al tema.",
                "Buenas tardes, en esta sesión de {title} vamos a profundizar en aspectos importantes.",
                "Les doy la bienvenida a {title}. Prepárense para una sesión intensa y productiva."
        };

        String[] contentTemplates = {
                "Primero, es importante entender que... Como pueden ver en este ejemplo... " +
                        "Ahora vamos a practicar juntos... ¿Tienen alguna pregunta hasta aquí? " +
                        "Perfecto, continuemos con el siguiente punto... Esto es crucial porque... " +
                        "Déjenme mostrarles cómo se hace... Exactamente, así es... ",

                "Iniciemos revisando los conceptos básicos... Muy bien, ahora que tenemos claro esto... " +
                        "La clave está en... Como mencioné antes... Presten atención a este detalle... " +
                        "Vamos a hacer un ejercicio práctico... Excelente participación... " +
                        "Ahora les toca a ustedes intentarlo... ",

                "Comencemos con una demostración... Fíjense en cómo... " +
                        "Este es un error común que deben evitar... La mejor práctica es... " +
                        "¿Notan la diferencia? Exacto... Sigamos adelante... " +
                        "Aquí tienen un consejo útil... Recuerden siempre que... "
        };

        String[] conclusionTemplates = {
                "Para resumir lo que vimos hoy... Recuerden practicar en casa... " +
                        "Nos vemos en la próxima sesión. ¡Muchas gracias por participar!",

                "En conclusión, los puntos principales fueron... No olviden revisar el material... " +
                        "Si tienen dudas, pueden contactarme. ¡Excelente trabajo hoy!",

                "Hemos cubierto mucho material importante... Asegúrense de repasar... " +
                        "Espero que hayan disfrutado la sesión. ¡Hasta la próxima!"
        };

        String intro = introTemplates[random.nextInt(introTemplates.length)];
        String content = contentTemplates[random.nextInt(contentTemplates.length)];
        String conclusion = conclusionTemplates[random.nextInt(conclusionTemplates.length)];

        return intro.replace("{title}", session.getTitle()) +
                "\n\n" + content + content +
                "\n\n" + conclusion;
    }
}