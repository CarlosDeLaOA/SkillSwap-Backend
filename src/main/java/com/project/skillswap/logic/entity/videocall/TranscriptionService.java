package com.project.skillswap.logic.entity.videocall;

import com.google.gson.Gson;
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

/**
 * Servicio de transcripción usando Groq API
 */
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

    private final OkHttpClient httpClient;
    private final Gson gson;

    public TranscriptionService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     *  Transcribe audio de sesión de forma asíncrona
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
            //  Obtener sesión y validar
            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getAudioRecordingUrl() == null || session.getAudioRecordingUrl().isEmpty()) {
                throw new RuntimeException("No hay grabación de audio para esta sesión");
            }

            //  IMPORTANTE: Cargar Person del instructor EXPLÍCITAMENTE para evitar LazyInitializationException
            Person instructor = session.getInstructor().getPerson();
            String instructorEmail = instructor.getEmail();
            String instructorFullName = instructor.getFullName();

            System.out.println("👤 Instructor cargado: " + instructorFullName + " (" + instructorEmail + ")");

            //  CORRECCIÓN: Construir ruta completa del archivo
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

            //  Validar formato MP3
            if (!audioFile.getName().toLowerCase().endsWith(".mp3")) {
                throw new RuntimeException("Solo se aceptan archivos MP3");
            }

            //  Actualizar estado: procesando
            session.setProcessingDate(LocalDateTime.now());
            sessionRepository.save(session);

            //  Enviar a Groq con reintentos
            String transcription = transcribeWithRetry(audioFile);

            //  Calcular duración estimada
            int durationSeconds = estimateAudioDuration(audioFile);

            // 7⃣ Guardar transcripción en base de datos
            session.setFullText(transcription);
            session.setDurationSeconds(durationSeconds);
            sessionRepository.save(session);

            System.out.println("========================================");
            System.out.println(" TRANSCRIPCIÓN COMPLETADA");
            System.out.println("   Duración estimada: " + durationSeconds + " segundos");
            System.out.println("   Longitud de texto: " + transcription.length() + " caracteres");
            System.out.println("   Guardado en BD: ✓");
            System.out.println("========================================");

            //  ENVIAR EMAIL AL INSTRUCTOR
            try {
                // ⭐ Ya tenemos el instructor cargado al inicio del método
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
                System.err.println("⚠️ Error en intento " + attempt + ": " + e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // Esperar antes de reintentar (backoff exponencial)
                        int waitSeconds = (int) Math.pow(2, attempt);
                        System.out.println("⏳ Esperando " + waitSeconds + " segundos antes de reintentar...");
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
     * 📤 Envía archivo a la API de Groq (formato OpenAI compatible)
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
            System.out.println("📥 Respuesta recibida de Groq");

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
     * ️ Estima duración del audio (simplificado)
     */
    private int estimateAudioDuration(File audioFile) {
        // Estimación aproximada: 1 minuto de audio MP3 (128kbps) ≈ 1MB
        long fileSizeBytes = audioFile.length();
        long fileSizeMB = fileSizeBytes / (1024 * 1024);

        // Estimación conservadora
        int estimatedSeconds = (int) (fileSizeMB * 60);

        System.out.println("⏱ Duración estimada: " + estimatedSeconds + " segundos (" +
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