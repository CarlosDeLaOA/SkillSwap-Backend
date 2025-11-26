package com.project.skillswap.logic.entity.videocall;

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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TranscriptionService {

    @Autowired
    private LearningSessionRepository sessionRepository;

    @Autowired
    private SessionEmailService sessionEmailService;

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

    public TranscriptionService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * ️ Transcribe audio de sesión de forma asíncrona
     *
     * @param sessionId ID de la sesión
     * @return CompletableFuture con resultado de transcripción
     */
    @Async
    @Transactional
    public CompletableFuture<TranscriptionResult> transcribeSessionAudio(Long sessionId) {
        System.out.println("========================================");
        System.out.println(" INICIANDO TRANSCRIPCIÓN CON GROQ");
        System.out.println("   Session ID: " + sessionId);
        System.out.println("   Modelo: " + groqModel);
        System.out.println("========================================");

        try {

            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getAudioRecordingUrl() == null || session.getAudioRecordingUrl().isEmpty()) {
                throw new RuntimeException("No hay grabación de audio para esta sesión");
            }


            Person instructor = session.getInstructor().getPerson();
            String instructorEmail = instructor.getEmail();
            String instructorFullName = instructor.getFullName();

            System.out.println("👤 Instructor cargado: " + instructorFullName + " (" + instructorEmail + ")");


            String audioFileName = session.getAudioRecordingUrl();
            File audioFile = new File(RECORDINGS_DIR + audioFileName);

            System.out.println(" Buscando archivo de audio...");
            System.out.println("   Nombre en BD: " + audioFileName);
            System.out.println("   Ruta completa: " + audioFile.getAbsolutePath());
            System.out.println("   Existe: " + audioFile.exists());

            if (!audioFile.exists()) {
                throw new RuntimeException("Archivo de audio no encontrado: " + audioFile.getAbsolutePath());
            }

            System.out.println(" Archivo de audio encontrado");
            System.out.println("   Ruta: " + audioFile.getAbsolutePath());
            System.out.println("   Tamaño: " + formatFileSize(audioFile.length()));


            if (!audioFile.getName().toLowerCase().endsWith(".mp3")) {
                throw new RuntimeException("Solo se aceptan archivos MP3");
            }


            session.setProcessingDate(LocalDateTime.now());
            sessionRepository.save(session);


            String transcription = transcribeWithRetry(audioFile);


            int durationSeconds = estimateAudioDuration(audioFile);


            session.setFullText(transcription);
            session.setDurationSeconds(durationSeconds);
            sessionRepository.save(session);

            System.out.println("========================================");
            System.out.println(" TRANSCRIPCIÓN COMPLETADA");
            System.out.println("   Duración estimada: " + durationSeconds + " segundos");
            System.out.println("   Longitud de texto: " + transcription.length() + " caracteres");
            System.out.println("   Guardado en BD: ✓");
            System.out.println("========================================");

            //  GUARDAR TRANSCRIPCIÓN EN JSON
            try {
                saveTranscriptionAsJson(session);
            } catch (Exception jsonError) {
                System.err.println(" Error guardando JSON: " + jsonError.getMessage());
                // No lanzar excepción, continuar con el proceso
            }

            //  GUARDAR TRANSCRIPCIÓN EN JSON
            try {
                saveTranscriptionAsJson(session);
                System.out.println(" Transcripción guardada en JSON");
            } catch (Exception jsonError) {
                System.err.println(" Error guardando JSON: " + jsonError.getMessage());
                // No lanzar excepción, continuar con el proceso
            }

            //  ENVIAR EMAIL AL INSTRUCTOR
            try {
                //  Ya tenemos el instructor cargado al inicio del método
                if (instructor != null && instructorEmail != null) {
                    System.out.println(" Enviando email de notificación...");

                    boolean emailSent = sessionEmailService.sendTranscriptionReadyEmail(session, instructor);

                    if (emailSent) {
                        System.out.println(" Email enviado a: " + instructorEmail);
                    } else {
                        System.out.println(" Email no enviado (validación fallida)");
                    }
                } else {
                    System.out.println(" No se envió email: instructor sin email configurado");
                }
            } catch (Exception emailError) {
                System.err.println(" Error enviando email: " + emailError.getMessage());
                emailError.printStackTrace();
                // No lanzar excepción, la transcripción ya está guardada exitosamente
            }

            return CompletableFuture.completedFuture(
                    new TranscriptionResult(true, transcription, durationSeconds, null)
            );

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println(" ERROR EN TRANSCRIPCIÓN");
            System.err.println("   Session ID: " + sessionId);
            System.err.println("   Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();

            return CompletableFuture.completedFuture(
                    new TranscriptionResult(false, null, 0, e.getMessage())
            );
        }
    }

    /**
     *  Envía audio a Groq con sistema de reintentos
     */
    private String transcribeWithRetry(File audioFile) throws IOException {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            attempt++;
            System.out.println(" Intento " + attempt + "/" + maxRetries);

            try {
                return sendToGroq(audioFile);
            } catch (IOException e) {
                lastException = e;
                System.err.println(" Error en intento " + attempt + ": " + e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // Esperar antes de reintentar (backoff exponencial)
                        int waitSeconds = (int) Math.pow(2, attempt);
                        System.out.println(" Esperando " + waitSeconds + " segundos antes de reintentar...");
                        Thread.sleep(waitSeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Proceso interrumpido durante reintento");
                    }
                }
            }
        }

        throw new IOException("Falló después de " + maxRetries + " intentos: " +
                (lastException != null ? lastException.getMessage() : "Error desconocido"));
    }

    /**
     *  Envía archivo a la API de Groq
     */
    private String sendToGroq(File audioFile) throws IOException {
        System.out.println(" Enviando audio a Groq...");
        System.out.println("   API: " + groqApiUrl);
        System.out.println("   Modelo: " + groqModel);

        // Crear multipart request (Groq usa formato OpenAI)
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, MediaType.parse("audio/mpeg")))
                .addFormDataPart("model", groqModel)
                .addFormDataPart("response_format", "json")
                .addFormDataPart("language", "es") // Cambia a "en" si necesitas inglés
                .build();

        Request request = new Request.Builder()
                .url(groqApiUrl)
                .header("Authorization", "Bearer " + groqApiKey)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                System.err.println(" Error HTTP " + response.code());
                System.err.println("   Respuesta: " + errorBody);
                throw new IOException("Error HTTP " + response.code() + ": " + errorBody);
            }

            String responseBody = response.body().string();
            System.out.println(" Respuesta recibida de Groq");

            // Parsear respuesta JSON
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            // Groq (formato OpenAI) devuelve la transcripción en el campo "text"
            if (jsonResponse.has("text")) {
                String transcription = jsonResponse.get("text").getAsString();
                System.out.println(" Transcripción exitosa");
                System.out.println("   Primeros 100 caracteres: " +
                        transcription.substring(0, Math.min(100, transcription.length())) + "...");
                return transcription;
            } else {
                throw new IOException("Respuesta inesperada de Groq: " + responseBody);
            }
        }
    }

    /**
     *  Obtiene duración REAL del audio usando FFmpeg
     */
    private int estimateAudioDuration(File audioFile) {
        try {
            System.out.println(" Obteniendo duración real del audio con FFmpeg...");

            // Ejecutar FFmpeg para obtener duración
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    audioFile.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Leer output
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
                // Parsear duración (viene en segundos con decimales: "15.234567")
                double durationSeconds = Double.parseDouble(output.toString().trim());
                int duration = (int) Math.ceil(durationSeconds);

                System.out.println(" Duración real obtenida: " + duration + " segundos (" +
                        (duration / 60) + "m " + (duration % 60) + "s)");

                return duration;
            } else {
                System.err.println(" FFprobe falló, usando estimación por tamaño de archivo");
                return estimateByFileSize(audioFile);
            }

        } catch (Exception e) {
            System.err.println(" Error obteniendo duración real: " + e.getMessage());
            return estimateByFileSize(audioFile);
        }
    }

    /**
     *  Estimación de duración por tamaño de archivo (fallback)
     */
    private int estimateByFileSize(File audioFile) {
        // Estimación aproximada: 1 minuto de audio MP3 (128kbps) ≈ 1MB
        long fileSizeBytes = audioFile.length();
        long fileSizeMB = fileSizeBytes / (1024 * 1024);

        // Para archivos pequeños, usar cálculo más preciso
        if (fileSizeMB == 0) {
            // 128 kbps = 16 KB/segundo
            int estimatedSeconds = (int) (fileSizeBytes / 16000);
            System.out.println("️ Duración estimada (archivo pequeño): " + estimatedSeconds + " segundos");
            return Math.max(estimatedSeconds, 1); // Mínimo 1 segundo
        }

        // Estimación conservadora para archivos grandes
        int estimatedSeconds = (int) (fileSizeMB * 60);
        System.out.println("️ Duración estimada: " + estimatedSeconds + " segundos (" +
                (estimatedSeconds / 60) + " minutos)");

        return estimatedSeconds;
    }

    /**
     *  Formatea tamaño de archivo
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     *  Guarda la transcripción en formato JSON
     */
    private void saveTranscriptionAsJson(LearningSession session) throws IOException {
        // Crear directorio si no existe
        File transcriptionDir = new File(TRANSCRIPTIONS_DIR);
        if (!transcriptionDir.exists()) {
            transcriptionDir.mkdirs();
            System.out.println(" Directorio creado: " + TRANSCRIPTIONS_DIR);
        }

        // Crear objeto JSON con metadatos completos
        JsonObject jsonTranscription = new JsonObject();

        // Información de la sesión
        jsonTranscription.addProperty("sessionId", session.getId());
        jsonTranscription.addProperty("sessionTitle", session.getTitle());
        jsonTranscription.addProperty("sessionDescription", session.getDescription());

        // Información del instructor
        if (session.getInstructor() != null && session.getInstructor().getPerson() != null) {
            Person instructor = session.getInstructor().getPerson();
            jsonTranscription.addProperty("instructorName", instructor.getFullName());
            jsonTranscription.addProperty("instructorEmail", instructor.getEmail());
        }

        // Información de la habilidad
        if (session.getSkill() != null) {
            jsonTranscription.addProperty("skillName", session.getSkill().getName());
            if (session.getSkill().getKnowledgeArea() != null) {
                jsonTranscription.addProperty("knowledgeArea", session.getSkill().getKnowledgeArea().getName());
            }
        }

        // Transcripción y metadatos
        jsonTranscription.addProperty("fullText", session.getFullText());
        jsonTranscription.addProperty("durationSeconds", session.getDurationSeconds());
        jsonTranscription.addProperty("durationMinutes", session.getDurationSeconds() != null ? session.getDurationSeconds() / 60 : 0);

        // Estadísticas
        String fullText = session.getFullText();
        jsonTranscription.addProperty("wordCount", fullText != null ? fullText.split("\\s+").length : 0);
        jsonTranscription.addProperty("characterCount", fullText != null ? fullText.length() : 0);

        // Fechas
        jsonTranscription.addProperty("processingDate", session.getProcessingDate() != null ? session.getProcessingDate().toString() : null);
        jsonTranscription.addProperty("scheduledDate", session.getScheduledDatetime() != null ? session.getScheduledDatetime().toString() : null);
        jsonTranscription.addProperty("audioRecordingUrl", session.getAudioRecordingUrl());

        // Nombre del archivo: session_[ID]_[TIMESTAMP].json
        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        String filename = String.format("session_%d_%s.json", session.getId(), timestamp);
        File jsonFile = new File(TRANSCRIPTIONS_DIR + filename);

        // Escribir JSON a archivo
        try (java.io.FileWriter writer = new java.io.FileWriter(jsonFile)) {
            gson.toJson(jsonTranscription, writer);
        }

        System.out.println(" JSON guardado: " + jsonFile.getAbsolutePath());
        System.out.println("   Tamaño: " + formatFileSize(jsonFile.length()));
    }

    /**
     *  Clase de resultado de transcripción
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
}