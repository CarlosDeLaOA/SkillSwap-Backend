
package com.project.skillswap.logic.entity.videocall;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para manejar grabaciones de audio
 * Graba en formato nativo del navegador y convierte a MP3
 */
@Service
public class RecordingService {
    private static final Logger logger = LoggerFactory.getLogger(RecordingService.class);

    @Autowired
    private LearningSessionRepository sessionRepository;

    @Autowired
    private TranscriptionService transcriptionService;

    private static final String RECORDINGS_DIR = "recordings/audio/";
    private static final long MAX_RECORDING_SIZE = 500 * 1024 * 1024; // 500MB

    private final Map<Long, RecordingSession> activeRecordings = new ConcurrentHashMap<>();
    private final Map<Long, String> recordingUrls = new ConcurrentHashMap<>();

    public RecordingService() {
        activeRecordings.clear();
        logger.info("️ Servicio de grabación inicializado");
    }

    /**
     * ▶ Inicia sesión de grabación
     */
    public Map<String, Object> startRecording(Long sessionId, Long instructorId) {
        if (activeRecordings.containsKey(sessionId)) {
            logger.info("️ Ya existe grabación activa para sesión " + sessionId);
            logger.info(" Deteniendo grabación anterior...");
            stopRecording(sessionId);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        createRecordingsDirectory();

        RecordingSession recording = new RecordingSession(sessionId, instructorId);
        activeRecordings.put(sessionId, recording);

        logger.info("========================================");
        logger.info("▶ GRABACIÓN INICIADA");
        logger.info("   Session ID: " + sessionId);
        logger.info("   Recording ID: " + recording.getRecordingId());
        logger.info("   Instructor ID: " + instructorId);
        logger.info("========================================");

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("recordingId", recording.getRecordingId());
        result.put("startTime", recording.getStartTime());
        result.put("status", "RECORDING");
        result.put("fileName", recording.getFileName());

        return result;
    }

    /**
     *  Detiene sesión de grabación
     */
    public Map<String, Object> stopRecording(Long sessionId) {
        RecordingSession recording = activeRecordings.get(sessionId);

        if (recording == null) {
            logger.info("ℹ No hay grabación activa para sesión: " + sessionId);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "No hay grabación activa");
            result.put("status", "NO_RECORDING");
            return result;
        }

        recording.stop();

        logger.info("========================================");
        logger.info("⏹ GRABACIÓN DETENIDA");
        logger.info("   Session ID: " + sessionId);
        logger.info("   Duración: " + recording.getDurationSeconds() + " segundos");
        logger.info("========================================");

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("recordingId", recording.getRecordingId());
        result.put("fileName", recording.getFileName());
        result.put("durationSeconds", recording.getDurationSeconds());
        result.put("status", "STOPPED");

        return result;
    }

    /**
     *  Guarda archivo de audio del navegador y lo convierte a MP3
     */
    public Map<String, Object> saveRecordingFile(Long sessionId, MultipartFile audioFile) {
        try {
            createRecordingsDirectory();

            //  Obtener la sesión de la base de datos
            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            RecordingSession recording = activeRecordings.get(sessionId);
            if (recording == null) {
                logger.info(" No se encontró sesión de grabación, creando una nueva...");
                recording = new RecordingSession(sessionId, 0L);
            }

            logger.info("========================================");
            logger.info(" RECIBIENDO ARCHIVO DE AUDIO");
            logger.info("   Session ID: " + sessionId);
            logger.info("   Nombre original: " + audioFile.getOriginalFilename());
            logger.info("   Content-Type: " + audioFile.getContentType());
            logger.info("   Tamaño: " + formatFileSize(audioFile.getSize()));
            logger.info("   isEmpty: " + audioFile.isEmpty());
            logger.info("========================================");

            if (audioFile.isEmpty() || audioFile.getSize() == 0) {
                throw new RuntimeException(" El archivo está vacío. No se capturó audio.");
            }

            //  VALIDACIÓN 2: Tamaño mínimo
            if (audioFile.getSize() < 10000) { // 10KB
                logger.info(" ADVERTENCIA: Archivo muy pequeño");
                logger.info("   Tamaño: " + audioFile.getSize() + " bytes");
                logger.info("   Esto puede indicar que no hay audio audible");
            }

            // Guardar archivo temporal WebM
            String tempFileName = "temp_" + sessionId + "_" + System.currentTimeMillis() + ".webm";
            Path tempFilePath = Paths.get(RECORDINGS_DIR + tempFileName);

            logger.info(" Guardando archivo temporal...");
            Files.copy(audioFile.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

            long tempFileSize = Files.size(tempFilePath);
            logger.info(" Archivo temporal guardado");
            logger.info("   Ruta: " + tempFilePath.toAbsolutePath());
            logger.info("   Tamaño verificado: " + formatFileSize(tempFileSize));

            boolean hasAudio = verifyAudioContent(tempFilePath);

            if (!hasAudio) {
                Files.deleteIfExists(tempFilePath);
                throw new RuntimeException(
                        " El archivo NO contiene audio válido.\n\n" +
                                "POSIBLE CAUSA:\n" +
                                "- No se capturó correctamente el audio del micrófono\n" +
                                "- El audio de Jitsi no estaba activo\n\n" +
                                "SOLUCIÓN:\n" +
                                "1. Verifica que tu micrófono esté encendido\n" +
                                "2. Habla durante la grabación\n" +
                                "3. Asegúrate de que otros participantes tengan audio"
                );
            }

            // Convertir a MP3
            String mp3FileName = recording.getFileName();
            Path mp3FilePath = Paths.get(RECORDINGS_DIR + mp3FileName);

            logger.info("========================================");
            logger.info(" INICIANDO CONVERSIÓN A MP3");
            logger.info("   Origen: " + tempFileName);
            logger.info("   Destino: " + mp3FileName);
            logger.info("========================================");

            convertToMP3(tempFilePath, mp3FilePath);

            long mp3FileSize = Files.size(mp3FilePath);

            logger.info("========================================");
            logger.info(" CONVERSIÓN A MP3 COMPLETADA");
            logger.info("   Archivo MP3: " + mp3FileName);
            logger.info("   Tamaño WebM: " + formatFileSize(tempFileSize));
            logger.info("   Tamaño MP3: " + formatFileSize(mp3FileSize));
            logger.info("   Ruta: " + mp3FilePath.toAbsolutePath());
            logger.info("========================================");

            // Validar MP3
            if (mp3FileSize < 10240) {
                logger.info("️ ADVERTENCIA: MP3 muy pequeño (" + mp3FileSize + " bytes)");
            }

            // Eliminar archivo temporal
            Files.deleteIfExists(tempFilePath);
            logger.info("️ Archivo temporal eliminado");

            //  GUARDAR RUTA EN BASE DE DATOS
            session.setAudioRecordingUrl(mp3FileName);
            sessionRepository.save(session);

            logger.info("========================================");
            logger.info(" NOMBRE DE ARCHIVO GUARDADO EN BD");
            logger.info("   Campo: audio_recording_url");
            logger.info("   Valor: " + mp3FileName);
            logger.info("========================================");

            // Guardar URL en memoria también
            recordingUrls.put(sessionId, RECORDINGS_DIR + mp3FileName);
            activeRecordings.remove(sessionId);

            logger.info("========================================");
            logger.info(" PROCESO COMPLETADO EXITOSAMENTE");
            logger.info("   Estado: Listo para transcripción");
            logger.info("   Guardado en BD: ");
            logger.info("========================================");

            // ⭐⭐⭐ TRIGGER TRANSCRIPCIÓN AUTOMÁTICA (AQUÍ ES EL LUGAR CORRECTO) ⭐⭐⭐
            logger.info("========================================");
            logger.info(" INICIANDO TRANSCRIPCIÓN AUTOMÁTICA");
            logger.info("   Session ID: " + sessionId);
            logger.info("   Archivo MP3: " + mp3FileName);
            logger.info("========================================");

            try {
                transcriptionService.transcribeSessionAudio(sessionId)
                        .thenAccept(transcriptionResult -> {
                            if (transcriptionResult.isSuccess()) {
                                logger.info("========================================");
                                logger.info(" TRANSCRIPCIÓN AUTOMÁTICA COMPLETADA");
                                logger.info("   Session ID: " + sessionId);
                                logger.info("========================================");
                            } else {
                                logger.info("========================================");
                                logger.info(" ERROR EN TRANSCRIPCIÓN AUTOMÁTICA");
                                logger.info("   Session ID: " + sessionId);
                                logger.info("   Error: " + transcriptionResult.getErrorMessage());
                                logger.info("========================================");
                            }
                        });

                logger.info(" Transcripción iniciada en segundo plano");

            } catch (Exception e) {
                logger.info("========================================");
                logger.info(" ERROR AL INICIAR TRANSCRIPCIÓN");
                logger.info("   Error: " + e.getMessage());
                logger.info("========================================");
                e.printStackTrace();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("fileName", mp3FileName);
            result.put("size", mp3FileSize);
            result.put("sizeFormatted", formatFileSize(mp3FileSize));
            result.put("path", mp3FilePath.toString());
            result.put("format", "MP3");
            result.put("hasAudio", true);
            result.put("savedToDatabase", true);

            return result;

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar grabación: " + e.getMessage());
        }
    }

    /**
     *  VALIDACIÓN MEJORADA: Verifica que el archivo contiene audio real
     */
    private boolean verifyAudioContent(Path filePath) {
        try {
            logger.info(" VERIFICANDO CONTENIDO DE AUDIO...");

            MultimediaObject source = new MultimediaObject(filePath.toFile());
            ws.schild.jave.info.MultimediaInfo info = source.getInfo();

            logger.info(" Información del archivo:");
            logger.info("   Duración: " + (info.getDuration() / 1000.0) + " segundos");
            logger.info("   Formato: " + info.getFormat());

            if (info.getAudio() == null) {
                logger.info(" NO TIENE PISTA DE AUDIO");
                return false;
            }

            logger.info(" Tiene pista de audio:");
            logger.info("   Codec: " + info.getAudio().getDecoder());
            logger.info("   Bitrate: " + info.getAudio().getBitRate() + " bps");
            logger.info("   Sample rate: " + info.getAudio().getSamplingRate() + " Hz");
            logger.info("   Canales: " + info.getAudio().getChannels());

            //  VALIDACIÓN MEJORADA: Solo verificar que tenga sample rate válido
            if (info.getAudio().getSamplingRate() == 0) {
                logger.info(" ADVERTENCIA: Sample rate es 0");
                return false;
            }

            //  Permitir duraciones cortas o negativas (problema común con WebM)
            // El archivo será válido si tiene codec y sample rate correctos
            logger.info(" Archivo válido para conversión");
            return true;

        } catch (Exception e) {
            logger.info(" No se pudo verificar el contenido: " + e.getMessage());
            //  En caso de error de análisis, permitir continuar
            return true;
        }
    }

    /**
     *  Convierte archivo de audio a MP3 usando JAVE2
     * CON CONFIGURACIÓN OPTIMIZADA
     */
    private void convertToMP3(Path inputPath, Path outputPath) {
        try {
            File source = inputPath.toFile();
            File target = outputPath.toFile();

            logger.info(" Configurando conversión...");

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            logger.info("   Codec: libmp3lame");
            logger.info("   Bitrate: 128 kbps");
            logger.info("   Sample rate: 44100 Hz");
            logger.info("   Canales: 2 (Stereo)");

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();

            logger.info(" Convirtiendo...");
            long startTime = System.currentTimeMillis();

            encoder.encode(new MultimediaObject(source), target, attrs);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logger.info(" Conversión exitosa en " + (duration / 1000.0) + " segundos");

        } catch (EncoderException e) {
            logger.info("========================================");
            logger.info(" ERROR EN CONVERSIÓN A MP3");
            logger.info("   Mensaje: " + e.getMessage());

            if (e.getMessage().contains("Invalid data found")) {
                logger.info("\n POSIBLE CAUSA:");
                logger.info("   El archivo WebM no contiene audio válido");
            }

            logger.info("========================================");
            e.printStackTrace();
            throw new RuntimeException("Error en conversión a MP3: " + e.getMessage());
        }
    }

    public void saveRecordingUrl(Long sessionId, String fileName) {
        String fullPath = RECORDINGS_DIR + fileName;
        recordingUrls.put(sessionId, fullPath);

        //  Guardar en base de datos
        try {
            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            Path absolutePath = Paths.get(fullPath).toAbsolutePath();
            session.setAudioRecordingUrl(absolutePath.toString());
            sessionRepository.save(session);

            logger.info(" URL de grabación guardada");
            logger.info("   Session ID: " + sessionId);
            logger.info("   Archivo: " + fileName);
            logger.info("   Ruta BD: " + absolutePath);

        } catch (Exception e) {
            logger.info(" Error guardando en BD: " + e.getMessage());
        }
    }

    /**
     *  Obtiene URL de grabación
     */
    public Map<String, Object> getRecordingUrl(Long sessionId) {
        Map<String, Object> result = new HashMap<>();

        //  Primero intentar obtener de la base de datos
        try {
            LearningSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session != null && session.getAudioRecordingUrl() != null) {
                File file = new File(session.getAudioRecordingUrl());
                if (file.exists()) {
                    result.put("available", true);
                    result.put("url", "/api/videocall/recording/download/" + sessionId);
                    result.put("fileName", file.getName());
                    result.put("size", file.length());
                    result.put("sizeFormatted", formatFileSize(file.length()));
                    result.put("source", "database");
                    return result;
                }
            }
        } catch (Exception e) {
            logger.info(" Error consultando BD: " + e.getMessage());
        }

        // Fallback: buscar en memoria
        String url = recordingUrls.get(sessionId);

        if (url != null) {
            File file = new File(url);
            if (file.exists()) {
                result.put("available", true);
                result.put("url", "/api/videocall/recording/download/" + sessionId);
                result.put("fileName", file.getName());
                result.put("size", file.length());
                result.put("sizeFormatted", formatFileSize(file.length()));
                result.put("source", "memory");
            } else {
                result.put("available", false);
                result.put("message", "Archivo no encontrado");
            }
        } else {
            result.put("available", false);
            result.put("message", "No hay grabación para esta sesión");
        }

        return result;
    }

    public int estimateRecordingDuration(String recordingUrl) {
        // Estimación simple basada en tamaño del archivo
        // 1 minuto de audio MP3 (128kbps) ≈ 1MB
        try {
            File file = new File(recordingUrl);
            if (file.exists()) {
                long fileSizeBytes = file.length();
                long fileSizeMB = fileSizeBytes / (1024 * 1024);
                return (int) (fileSizeMB * 60); // Convertir MB a segundos estimados
            }
        } catch (Exception e) {
            logger.info("Error estimando duración: " + e.getMessage());
        }
        return 0;
    }

    /**
     *  Elimina archivo manualmente
     */
    public void deleteRecordingFile(Long sessionId) {
        String url = recordingUrls.get(sessionId);

        if (url != null) {
            try {
                Path filePath = Paths.get(url);
                Files.deleteIfExists(filePath);
                recordingUrls.remove(sessionId);
                logger.info("️ Grabación eliminada: " + filePath.getFileName());
            } catch (IOException e) {
                logger.info(" Error al eliminar: " + e.getMessage());
            }
        }
    }

    /**
     *  Limpia grabación huérfana
     */
    public void forceStopRecording(Long sessionId) {
        RecordingSession recording = activeRecordings.remove(sessionId);

        if (recording != null) {
            recording.stop();
            logger.info(" Grabación limpiada para sesión: " + sessionId);
        }
    }

    /**
     *  Obtiene estado de grabación
     */
    public Map<String, Object> getRecordingStatus(Long sessionId) {
        RecordingSession recording = activeRecordings.get(sessionId);

        Map<String, Object> status = new HashMap<>();
        status.put("sessionId", sessionId);

        if (recording != null) {
            status.put("isRecording", true);
            status.put("recordingId", recording.getRecordingId());
            status.put("startTime", recording.getStartTime());
            status.put("durationSeconds", recording.getDurationSeconds());
        } else {
            status.put("isRecording", false);

            // Verificar si existe en BD
            try {
                LearningSession session = sessionRepository.findById(sessionId).orElse(null);
                if (session != null && session.getAudioRecordingUrl() != null) {
                    status.put("hasRecording", true);
                    status.put("status", "COMPLETED");
                    status.put("recordingUrl", session.getAudioRecordingUrl());
                    return status;
                }
            } catch (Exception e) {
                logger.info(" Error consultando BD: " + e.getMessage());
            }

            // Fallback: verificar en memoria
            String url = recordingUrls.get(sessionId);
            if (url != null) {
                status.put("hasRecording", true);
                status.put("status", "COMPLETED");
            }
        }

        return status;
    }

    /**
     *  Crea directorio de grabaciones
     */
    private void createRecordingsDirectory() {
        try {
            Path path = Paths.get(RECORDINGS_DIR);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al crear directorio: " + e.getMessage());
        }
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
     *  Clase interna para manejar sesiones de grabación activas
     */
    private static class RecordingSession {
        private final String recordingId;
        private final Long sessionId;
        private final Long instructorId;
        private final Date startTime;
        private Date endTime;

        public RecordingSession(Long sessionId, Long instructorId) {
            this.recordingId = "REC_" + sessionId + "_" + System.currentTimeMillis();
            this.sessionId = sessionId;
            this.instructorId = instructorId;
            this.startTime = new Date();
        }

        public void stop() {
            this.endTime = new Date();
        }

        public String getRecordingId() { return recordingId; }
        public Date getStartTime() { return startTime; }

        public String getFileName() {
            return "session_" + sessionId + "_" + recordingId + ".mp3";
        }

        public long getDurationSeconds() {
            if (endTime == null) {
                return (new Date().getTime() - startTime.getTime()) / 1000;
            }
            return (endTime.getTime() - startTime.getTime()) / 1000;
        }
    }
}