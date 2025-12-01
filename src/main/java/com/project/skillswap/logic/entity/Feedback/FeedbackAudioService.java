package com.project.skillswap.logic.entity.Feedback;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.videocall.TranscriptionService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para manejar grabaciones de audio y transcripciones de feedback
 * Reutiliza TranscriptionService y almacena en Cloudinary
 */
@Service
public class FeedbackAudioService {

    //#region Dependencies
    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private LearningSessionRepository learningSessionRepository;

    @Autowired
    private TranscriptionService transcriptionService;

    @Autowired
    private Cloudinary cloudinary;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model:whisper-large-v3}")
    private String groqModel;
    //#endregion

    //#region Configuration
    private static final String CLOUDINARY_PRESET = "skillswap_feedback";
    private static final long MAX_DURATION = 120;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    //#endregion

    //#region Public Methods

    /**
     * Sube archivo de audio MP3 a Cloudinary y guarda referencia en Feedback
     * @param feedbackId ID del feedback
     * @param audioFile Archivo MP3 grabado
     * @param durationSeconds Duracion en segundos
     * @return Mapa con resultado de la subida
     */
    public Map<String, Object> uploadAudioToCloudinary(Long feedbackId, MultipartFile audioFile, Integer durationSeconds) {
        try {
            System.out.println("========================================");
            System.out.println("[FeedbackAudioService] Uploading audio to Cloudinary");
            System.out.println("   Feedback ID: " + feedbackId);
            System.out.println("   File: " + audioFile.getOriginalFilename());
            System.out.println("   Size: " + formatFileSize(audioFile.getSize()));
            System.out.println("   Duration: " + durationSeconds + " seconds");
            System.out.println("========================================");

            if (audioFile.isEmpty()) {
                throw new RuntimeException("El archivo de audio esta vacio");
            }

            if (audioFile.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("El archivo excede el tamano maximo permitido (50MB)");
            }

            if (durationSeconds > MAX_DURATION) {
                throw new RuntimeException("La duracion excede el maximo permitido (120 segundos)");
            }

            Feedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new RuntimeException("Feedback no encontrado"));

            Map uploadResult = cloudinary.uploader().upload(audioFile.getBytes(),
                    ObjectUtils.asMap(
                            "upload_preset", CLOUDINARY_PRESET,
                            "folder", "skillswap/feedback/session_" + feedback.getLearningSession().getId(),
                            "resource_type", "video",
                            "type", "upload",
                            "public_id", "feedback_" + feedbackId + "_" + System.currentTimeMillis()
                    ));

            String audioUrl = uploadResult.get("secure_url").toString();
            String publicId = uploadResult.get("public_id").toString();

            System.out.println("========================================");
            System.out.println("[FeedbackAudioService] Audio uploaded successfully");
            System.out.println("   URL: " + audioUrl);
            System.out.println("   Public ID: " + publicId);
            System.out.println("========================================");

            feedback.setAudioUrl(audioUrl);
            feedback.setDurationSeconds(durationSeconds);
            feedback.setProcessingDate(LocalDateTime.now());
            feedbackRepository.save(feedback);

            startTranscriptionAsync(feedbackId, audioFile, feedback.getLearningSession().getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("audioUrl", audioUrl);
            result.put("publicId", publicId);
            result.put("durationSeconds", durationSeconds);
            result.put("message", "Audio subido exitosamente.  Transcribiendo...");

            return result;

        } catch (IOException e) {
            System.err.println("[FeedbackAudioService] Error uploading to Cloudinary: " + e.getMessage());
            throw new RuntimeException("Error al subir audio a Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Obtiene el estado de transcripcion de un feedback
     * @param feedbackId ID del feedback
     * @return Mapa con estado de transcripcion
     */
    public Map<String, Object> getTranscriptionStatus(Long feedbackId) {
        try {
            Feedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new RuntimeException("Feedback no encontrado"));

            Map<String, Object> status = new HashMap<>();
            status.put("feedbackId", feedbackId);

            if (feedback.getAudioTranscription() != null && !feedback.getAudioTranscription().isEmpty()) {
                status.put("status", "COMPLETED");
                status.put("transcription", feedback.getAudioTranscription());
                status.put("processingDate", feedback.getProcessingDate());
            } else if (feedback.getProcessingDate() != null) {
                status.put("status", "PROCESSING");
                status.put("message", "Transcripcion en progreso...");
            } else if (feedback.getAudioUrl() != null) {
                status.put("status", "READY");
                status.put("message", "Listo para transcribir");
            } else {
                status.put("status", "NO_AUDIO");
                status.put("message", "Sin audio grabado");
            }

            return status;

        } catch (Exception e) {
            throw new RuntimeException("Error al obtener estado: " + e.getMessage());
        }
    }

    /**
     * Elimina audio de Cloudinary y limpia referencia en Feedback
     * @param feedbackId ID del feedback
     */
    public Map<String, Object> deleteAudio(Long feedbackId) {
        try {
            Feedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new RuntimeException("Feedback no encontrado"));

            if (feedback.getAudioUrl() != null) {
                String publicId = extractPublicIdFromCloudinaryUrl(feedback.getAudioUrl());

                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId,
                            ObjectUtils.asMap(
                                    "resource_type", "video",
                                    "invalidate", true
                            ));

                    System.out.println("[FeedbackAudioService] Audio deleted from Cloudinary: " + publicId);
                }
            }

            feedback.setAudioUrl(null);
            feedback.setAudioTranscription(null);
            feedback.setDurationSeconds(null);
            feedback.setProcessingDate(null);
            feedbackRepository.save(feedback);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Audio eliminado exitosamente");

            return result;

        } catch (Exception e) {
            System.err.println("[FeedbackAudioService] Error deleting audio: " + e.getMessage());
            throw new RuntimeException("Error al eliminar audio: " + e.getMessage());
        }
    }

    //#endregion

    //#region Private Methods

    /**
     * Inicia la transcripcion de forma asincrona
     * @param feedbackId ID del feedback
     * @param audioFile Archivo de audio
     * @param sessionId ID de la sesion
     */
    @Async
    private void startTranscriptionAsync(Long feedbackId, MultipartFile audioFile, Long sessionId) {
        try {
            System.out.println("========================================");
            System.out.println("[FeedbackAudioService] Starting async transcription for feedback: " + feedbackId);
            System.out.println("   File: " + audioFile.getOriginalFilename());
            System.out.println("   Size: " + formatFileSize(audioFile.getSize()));
            System.out.println("========================================");

            Feedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new RuntimeException("Feedback no encontrado"));

            try {
                String transcription = transcribeAudioFile(audioFile);

                if (transcription != null && !transcription.isEmpty()) {
                    feedback.setAudioTranscription(transcription);
                    feedbackRepository.save(feedback);

                    System.out.println("========================================");
                    System.out.println("[FeedbackAudioService] ✅ Transcription completed");
                    System.out.println("   Length: " + transcription.length() + " characters");
                    System.out.println("========================================");
                } else {
                    System.err.println("[FeedbackAudioService] ❌ Empty transcription");
                    feedback.setAudioTranscription("ERROR: Transcripción vacía");
                    feedbackRepository.save(feedback);
                }
            } catch (Exception transcriptionError) {
                System.err.println("[FeedbackAudioService] ❌ Transcription error: " + transcriptionError.getMessage());
                transcriptionError.printStackTrace();
                feedback.setAudioTranscription("ERROR: " + transcriptionError.getMessage());
                feedbackRepository.save(feedback);
            }

        } catch (Exception e) {
            System.err.println("[FeedbackAudioService] Error in async transcription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Transcribe un archivo de audio usando Groq API
     * @param audioFile Archivo de audio (WEBM, MP3, etc.)
     * @return Transcripción en texto
     */
    private String transcribeAudioFile(MultipartFile audioFile) throws IOException {
        System.out.println("[FeedbackAudioService] Transcribing audio file: " + audioFile.getOriginalFilename());

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getOriginalFilename(),
                        RequestBody.create(audioFile.getBytes(),
                                MediaType.parse("audio/*")))
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
            if (! response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No details";
                throw new IOException("Groq API error " + response.code() + ": " + errorBody);
            }

            String responseBody = response.body().string();
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (jsonResponse.has("text")) {
                String transcription = jsonResponse.get("text").getAsString();
                System.out.println("[FeedbackAudioService] Transcription result: " + transcription.substring(0, Math.min(100, transcription.length())) + "...");
                return transcription;
            }

            throw new IOException("Unexpected Groq response: " + responseBody);
        }
    }

    /**
     * Extrae public_id de URL de Cloudinary
     */
    private String extractPublicIdFromCloudinaryUrl(String url) {
        try {
            if (url == null || ! url.contains("cloudinary.com")) {
                return null;
            }

            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;

            String pathAfterUpload = parts[1];
            String withoutVersion = pathAfterUpload.replaceFirst("v\\d+/", "");
            String publicId = withoutVersion.replaceFirst("\\.[^.]+$", "");

            return publicId;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Formatea tamano de archivo
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    //#endregion
}