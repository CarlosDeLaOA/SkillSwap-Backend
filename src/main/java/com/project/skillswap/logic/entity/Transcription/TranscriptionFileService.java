package com.project.skillswap.logic.entity.Transcription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Servicio para gestión de archivos de transcripción
 */
@Service
public class TranscriptionFileService {

    //#region Properties
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionFileService.class);

    @Value("${transcription.storage.path:recordings/Transcription}")
    private String transcriptionPath;

    private final ObjectMapper objectMapper;
    //#endregion

    //#region Constructor
    public TranscriptionFileService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    //#endregion

    //#region Public Methods
    /**
     * Lee el archivo de transcripción para una sesión específica
     *
     * @param sessionId ID de la sesión
     * @return el texto completo de la transcripción
     * @throws IOException si hay error al leer el archivo
     */
    public String getTranscriptionText(Long sessionId) throws IOException {
        logger.info("Buscando transcripción para session: {}", sessionId);

        Optional<File> transcriptionFile = findTranscriptionFile(sessionId);

        if (transcriptionFile.isEmpty()) {
            logger.warn("No se encontró archivo de transcripción para session: {}", sessionId);
            throw new IOException("No se encontró archivo de transcripción para la sesión " + sessionId);
        }

        File file = transcriptionFile.get();
        logger.info("Transcripción encontrada: {}", file.getName());

        String content = Files.readString(file.toPath());

        JsonNode jsonNode = objectMapper.readTree(content);
        String fullText = jsonNode.path("fullText").asText();

        if (fullText == null || fullText.trim().isEmpty()) {
            logger.error("Transcripción vacía para session: {}", sessionId);
            throw new IOException("La transcripción está vacía");
        }

        logger.info("Transcripción leída exitosamente - {} caracteres", fullText.length());
        return fullText;
    }

    /**
     * Verifica si existe una transcripción para una sesión
     *
     * @param sessionId ID de la sesión
     * @return true si existe la transcripción
     */
    public boolean hasTranscription(Long sessionId) {
        Optional<File> file = findTranscriptionFile(sessionId);
        boolean exists = file.isPresent();

        logger.debug("Transcripción para session {}: {}", sessionId, exists ? "existe" : "no existe");
        return exists;
    }

    /**
     * Obtiene información completa de la transcripción
     *
     * @param sessionId ID de la sesión
     * @return objeto con toda la información de la transcripción
     * @throws IOException si hay error al leer el archivo
     */
    public TranscriptionInfo getTranscriptionInfo(Long sessionId) throws IOException {
        logger.info("Obteniendo info completa de transcripción para session: {}", sessionId);

        Optional<File> transcriptionFile = findTranscriptionFile(sessionId);

        if (transcriptionFile.isEmpty()) {
            throw new IOException("No se encontró archivo de transcripción para la sesión " + sessionId);
        }

        File file = transcriptionFile.get();
        String content = Files.readString(file.toPath());

        JsonNode jsonNode = objectMapper.readTree(content);

        TranscriptionInfo info = new TranscriptionInfo();
        info.setSessionId(jsonNode.path("sessionId").asLong());
        info.setSessionTitle(jsonNode.path("sessionTitle").asText());
        info.setSessionDescription(jsonNode.path("sessionDescription").asText());
        info.setInstructorName(jsonNode.path("instructorName").asText());
        info.setInstructorEmail(jsonNode.path("instructorEmail").asText());
        info.setSkillName(jsonNode.path("skillName").asText());
        info.setKnowledgeArea(jsonNode.path("knowledgeArea").asText());
        info.setFullText(jsonNode.path("fullText").asText());
        info.setDurationSeconds(jsonNode.path("durationSeconds").asInt());
        info.setDurationMinutes(jsonNode.path("durationMinutes").asInt());
        info.setWordCount(jsonNode.path("wordCount").asInt());
        info.setCharacterCount(jsonNode.path("characterCount").asInt());
        info.setProcessingDate(jsonNode.path("processingDate").asText());
        info.setScheduledDate(jsonNode.path("scheduledDate").asText());
        info.setAudioRecordingUrl(jsonNode.path("audioRecordingUrl").asText());

        logger.info("Info de transcripción obtenida - {} palabras, {} caracteres",
                info.getWordCount(), info.getCharacterCount());

        return info;
    }
    //#endregion

    //#region Private Methods
    /**
     * Busca el archivo de transcripción para una sesión
     *
     * @param sessionId ID de la sesión
     * @return archivo si existe
     */
    private Optional<File> findTranscriptionFile(Long sessionId) {
        try {
            Path dirPath = Paths.get(transcriptionPath);

            if (!Files.exists(dirPath)) {
                logger.warn("Directorio de transcripciones no existe: {}", transcriptionPath);
                return Optional.empty();
            }

            File directory = dirPath.toFile();
            File[] files = directory.listFiles((dir, name) ->
                    name.startsWith("session_" + sessionId + "_") && name.endsWith(".json")
            );

            if (files != null && files.length > 0) {
                logger.debug("Archivo de transcripción encontrado: {}", files[0].getName());
                return Optional.of(files[0]);
            }

            logger.debug("No se encontró archivo de transcripción para session: {}", sessionId);
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error al buscar archivo de transcripción: {}", e.getMessage());
            return Optional.empty();
        }
    }
    //#endregion

    //#region Inner Class
    /**
     * Clase que contiene la información completa de una transcripción
     */
    public static class TranscriptionInfo {
        private Long sessionId;
        private String sessionTitle;
        private String sessionDescription;
        private String instructorName;
        private String instructorEmail;
        private String skillName;
        private String knowledgeArea;
        private String fullText;
        private Integer durationSeconds;
        private Integer durationMinutes;
        private Integer wordCount;
        private Integer characterCount;
        private String processingDate;
        private String scheduledDate;
        private String audioRecordingUrl;

        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

        public String getSessionTitle() { return sessionTitle; }
        public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }

        public String getSessionDescription() { return sessionDescription; }
        public void setSessionDescription(String sessionDescription) { this.sessionDescription = sessionDescription; }

        public String getInstructorName() { return instructorName; }
        public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

        public String getInstructorEmail() { return instructorEmail; }
        public void setInstructorEmail(String instructorEmail) { this.instructorEmail = instructorEmail; }

        public String getSkillName() { return skillName; }
        public void setSkillName(String skillName) { this.skillName = skillName; }

        public String getKnowledgeArea() { return knowledgeArea; }
        public void setKnowledgeArea(String knowledgeArea) { this.knowledgeArea = knowledgeArea; }

        public String getFullText() { return fullText; }
        public void setFullText(String fullText) { this.fullText = fullText; }

        public Integer getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

        public Integer getWordCount() { return wordCount; }
        public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

        public Integer getCharacterCount() { return characterCount; }
        public void setCharacterCount(Integer characterCount) { this.characterCount = characterCount; }

        public String getProcessingDate() { return processingDate; }
        public void setProcessingDate(String processingDate) { this.processingDate = processingDate; }

        public String getScheduledDate() { return scheduledDate; }
        public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

        public String getAudioRecordingUrl() { return audioRecordingUrl; }
        public void setAudioRecordingUrl(String audioRecordingUrl) { this.audioRecordingUrl = audioRecordingUrl; }
    }
    //#endregion
}