
package com.project.skillswap.logic.entity.videocall;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionEmailService;
import com.project.skillswap.logic.entity.Person.Person;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.skillswap.logic.entity.LearningSession.SessionCompletionService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TranscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);

    //#region Dependencies
    @Autowired
    private LearningSessionRepository sessionRepository;

    @Autowired
    private SessionEmailService sessionEmailService;

    @Autowired
    private SessionSummaryService summaryService;

    @Autowired
    private SessionSummaryPdfService summaryPdfService;

    @Autowired
    private SessionSummaryEmailService summaryEmailService;

    @Autowired
    private SessionCompletionService sessionCompletionService;
    //#endregion


    //#region Configuration
    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model:whisper-large-v3}")
    private String groqModel;

    @Value("${transcription.max-retries:3}")
    private int maxRetries;

    @Value("${transcription.timeout-seconds:300}")
    private int timeoutSeconds;

    private static final String RECORDINGS_DIR = "recordings/audio/";
    private static final String TRANSCRIPTIONS_DIR = "recordings/Transcription/";

    private final OkHttpClient httpClient;
    private final Gson gson;
    //#endregion


    //#region Constructor
    /**
     * Inicializa los componentes del servicio de transcripción.
     * Configura el cliente HTTP y el formateador JSON.
     */
    public TranscriptionService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();

        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    //#endregion


    //#region Main Transcription Flow

    /**
     * Procesa la transcripción de audio de manera asíncrona.
     *
     * @param sessionId ID de la sesión a transcribir
     * @return resultado del proceso de transcripción
     */
    @Async
    @Transactional
    public CompletableFuture<TranscriptionResult> transcribeSessionAudio(Long sessionId) {
        try {
            // 1. Validación de sesión
            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getAudioRecordingUrl() == null || session.getAudioRecordingUrl().isEmpty()) {
                throw new RuntimeException("No hay grabación de audio para esta sesión");
            }

            Person instructor = session.getInstructor().getPerson();

            // 2. Archivo de audio
            File audioFile = new File(RECORDINGS_DIR + session.getAudioRecordingUrl());

            if (!audioFile.exists()) {
                throw new RuntimeException("Archivo de audio no encontrado: " + audioFile.getAbsolutePath());
            }

            if (!audioFile.getName().toLowerCase().endsWith(".mp3")) {
                throw new RuntimeException("Solo se aceptan archivos MP3");
            }

            session.setProcessingDate(LocalDateTime.now());
            sessionRepository.save(session);

            // 3. Enviar archivo a Groq
            String transcription = transcribeWithRetry(audioFile);

            // 4. Obtener duración real
            int durationSeconds = estimateAudioDuration(audioFile);

            // 5. Guardar transcripción
            session.setFullText(transcription);
            session.setDurationSeconds(durationSeconds);
            sessionRepository.save(session);

            // 6. Generar resumen, PDF y enviar emails (no detener proceso si fallan)
            try {
                String summary = summaryService.generateSummary(session);

                if (summaryService.validateSummary(summary)) {
                    byte[] summaryPdf = summaryPdfService.generateSummaryPdf(session, summary);
                    summaryEmailService.sendSummaryToParticipants(session, summaryPdf, summary);
                }

            } catch (Exception ignored) {}

            // 7. Guardar JSON
            try {
                saveTranscriptionAsJson(session);
            } catch (Exception ignored) {}

            // 8. Enviar email al instructor
            try {
                if (instructor != null && instructor.getEmail() != null) {
                    sessionEmailService.sendTranscriptionReadyEmail(session, instructor);
                }
            } catch (Exception ignored) {}

            try {
                logger.info(" INICIANDO ENVÍO DE INVITACIONES QUIZ");
                sessionCompletionService.processSessionCompletion(session.getId());
            } catch (Exception e) {
                logger.info(" ERROR AL ENVIAR INVITACIONES QUIZ");
                e.printStackTrace();
            }
            // 9. Respuesta final
            return CompletableFuture.completedFuture(
                    new TranscriptionResult(true, transcription, durationSeconds, null)
            );

        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    new TranscriptionResult(false, null, 0, e.getMessage())
            );
        }
    }
    //#endregion



    //#region Groq API

    /**
     * Envía el audio a Groq con lógica de reintentos automáticos.
     *
     * @param audioFile archivo de audio MP3
     * @return transcripción en texto
     * @throws IOException si después de todos los intentos no responde la API
     */
    private String transcribeWithRetry(File audioFile) throws IOException {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            attempt++;

            try {
                return sendToGroq(audioFile);

            } catch (IOException e) {
                lastException = e;

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep((long) Math.pow(2, attempt) * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Proceso interrumpido durante reintento");
                    }
                }
            }
        }

        throw new IOException(
                "Falló después de " + maxRetries + " intentos: " +
                        (lastException != null ? lastException.getMessage() : "Error desconocido")
        );
    }

    /**
     * Envía un archivo de audio a la API de Groq en formato OpenAI-compatible.
     *
     * @param audioFile archivo MP3
     * @return texto de la transcripción
     * @throws IOException si Groq responde error
     */
    private String sendToGroq(File audioFile) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, MediaType.parse("audio/mpeg")))
                .addFormDataPart("model", groqModel)
                .addFormDataPart("response_format", "json")
                .addFormDataPart("language", "es")
                .build();

        Request request = new Request.Builder()
                .url(groqApiUrl)
                .header("Authorization", "Bearer " + groqApiKey)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                throw new IOException("Error HTTP " + response.code() + ": " + errorBody);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (jsonResponse.has("text")) {
                return jsonResponse.get("text").getAsString();
            }

            throw new IOException("Respuesta inesperada de Groq: " + responseBody);
        }
    }
    //#endregion



    //#region Audio Duration

    /**
     * Obtiene la duración real del archivo de audio utilizando FFprobe.
     *
     * @param audioFile archivo de audio
     * @return duración total en segundos
     */
    private int estimateAudioDuration(File audioFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    audioFile.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && output.length() > 0) {
                double durationSeconds = Double.parseDouble(output.toString().trim());
                return (int) Math.ceil(durationSeconds);
            } else {
                return estimateByFileSize(audioFile);
            }

        } catch (Exception e) {
            return estimateByFileSize(audioFile);
        }
    }

    /**
     * Estima la duración del audio basándose en el tamaño del archivo.
     *
     * @param audioFile archivo MP3
     * @return duración aproximada en segundos
     */
    private int estimateByFileSize(File audioFile) {
        long fileSizeBytes = audioFile.length();
        long fileSizeMB = fileSizeBytes / (1024 * 1024);

        if (fileSizeMB == 0) {
            int estimatedSeconds = (int) (fileSizeBytes / 16000);
            return Math.max(estimatedSeconds, 1);
        }

        return (int) (fileSizeMB * 60);
    }
    //#endregion



    //#region JSON Export

    /**
     * Guarda la transcripción completa en formato JSON dentro del directorio asignado.
     *
     * @param session sesión con información de transcripción
     * @throws IOException si no se puede escribir el archivo JSON
     */
    private void saveTranscriptionAsJson(LearningSession session) throws IOException {

        File transcriptionDir = new File(TRANSCRIPTIONS_DIR);
        if (!transcriptionDir.exists()) {
            transcriptionDir.mkdirs();
        }

        JsonObject jsonTranscription = new JsonObject();

        jsonTranscription.addProperty("sessionId", session.getId());
        jsonTranscription.addProperty("sessionTitle", session.getTitle());
        jsonTranscription.addProperty("sessionDescription", session.getDescription());

        if (session.getInstructor() != null && session.getInstructor().getPerson() != null) {
            Person instructor = session.getInstructor().getPerson();
            jsonTranscription.addProperty("instructorName", instructor.getFullName());
            jsonTranscription.addProperty("instructorEmail", instructor.getEmail());
        }

        if (session.getSkill() != null) {
            jsonTranscription.addProperty("skillName", session.getSkill().getName());
            if (session.getSkill().getKnowledgeArea() != null) {
                jsonTranscription.addProperty("knowledgeArea", session.getSkill().getKnowledgeArea().getName());
            }
        }

        jsonTranscription.addProperty("fullText", session.getFullText());
        jsonTranscription.addProperty("durationSeconds", session.getDurationSeconds());
        jsonTranscription.addProperty("durationMinutes",
                session.getDurationSeconds() != null ? session.getDurationSeconds() / 60 : 0);

        String fullText = session.getFullText();
        jsonTranscription.addProperty("wordCount", fullText != null ? fullText.split("\\s+").length : 0);
        jsonTranscription.addProperty("characterCount", fullText != null ? fullText.length() : 0);

        jsonTranscription.addProperty("processingDate",
                session.getProcessingDate() != null ? session.getProcessingDate().toString() : null);
        jsonTranscription.addProperty("scheduledDate",
                session.getScheduledDatetime() != null ? session.getScheduledDatetime().toString() : null);
        jsonTranscription.addProperty("audioRecordingUrl", session.getAudioRecordingUrl());

        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        String filename = String.format("session_%d_%s.json", session.getId(), timestamp);

        File jsonFile = new File(TRANSCRIPTIONS_DIR + filename);

        try (java.io.FileWriter writer = new java.io.FileWriter(jsonFile)) {
            gson.toJson(jsonTranscription, writer);
        }
    }
    //#endregion



    //#region DTO

    /**
     * Representa el resultado del proceso de transcripción.
     */
    public static class TranscriptionResult {
        private final boolean success;
        private final String transcription;
        private final int durationSeconds;
        private final String errorMessage;

        public TranscriptionResult(boolean success, String transcription, int durationSeconds, String errorMessage) {
            this.success = success;
            this.transcription = transcription;
            this.durationSeconds = durationSeconds;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getTranscription() { return transcription; }
        public int getDurationSeconds() { return durationSeconds; }
        public String getErrorMessage() { return errorMessage; }
    }
    //#endregion
}
