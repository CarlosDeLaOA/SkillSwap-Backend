package com.project.skillswap.logic.entity.videocall;

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
        System.out.println("️ Servicio de grabación inicializado");
    }

    /**
     * ▶ Inicia sesión de grabación
     */
    public Map<String, Object> startRecording(Long sessionId, Long instructorId) {
        if (activeRecordings.containsKey(sessionId)) {
            System.out.println("️ Ya existe grabación activa para sesión " + sessionId);
            System.out.println(" Deteniendo grabación anterior...");
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

        System.out.println("========================================");
        System.out.println("▶ GRABACIÓN INICIADA");
        System.out.println("   Session ID: " + sessionId);
        System.out.println("   Recording ID: " + recording.getRecordingId());
        System.out.println("   Instructor ID: " + instructorId);
        System.out.println("========================================");

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
            System.out.println("ℹ No hay grabación activa para sesión: " + sessionId);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "No hay grabación activa");
            result.put("status", "NO_RECORDING");
            return result;
        }

        recording.stop();

        System.out.println("========================================");
        System.out.println("⏹ GRABACIÓN DETENIDA");
        System.out.println("   Session ID: " + sessionId);
        System.out.println("   Duración: " + recording.getDurationSeconds() + " segundos");
        System.out.println("========================================");

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
                System.out.println(" No se encontró sesión de grabación, creando una nueva...");
                recording = new RecordingSession(sessionId, 0L);
            }

            System.out.println("========================================");
            System.out.println(" RECIBIENDO ARCHIVO DE AUDIO");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("   Nombre original: " + audioFile.getOriginalFilename());
            System.out.println("   Content-Type: " + audioFile.getContentType());
            System.out.println("   Tamaño: " + formatFileSize(audioFile.getSize()));
            System.out.println("   isEmpty: " + audioFile.isEmpty());
            System.out.println("========================================");

            if (audioFile.isEmpty() || audioFile.getSize() == 0) {
                throw new RuntimeException(" El archivo está vacío. No se capturó audio.");
            }

            //  VALIDACIÓN 2: Tamaño mínimo
            if (audioFile.getSize() < 10000) { // 10KB
                System.out.println(" ADVERTENCIA: Archivo muy pequeño");
                System.out.println("   Tamaño: " + audioFile.getSize() + " bytes");
                System.out.println("   Esto puede indicar que no hay audio audible");
            }

            // Guardar archivo temporal WebM
            String tempFileName = "temp_" + sessionId + "_" + System.currentTimeMillis() + ".webm";
            Path tempFilePath = Paths.get(RECORDINGS_DIR + tempFileName);

            System.out.println(" Guardando archivo temporal...");
            Files.copy(audioFile.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

            long tempFileSize = Files.size(tempFilePath);
            System.out.println(" Archivo temporal guardado");
            System.out.println("   Ruta: " + tempFilePath.toAbsolutePath());
            System.out.println("   Tamaño verificado: " + formatFileSize(tempFileSize));

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

            System.out.println("========================================");
            System.out.println(" INICIANDO CONVERSIÓN A MP3");
            System.out.println("   Origen: " + tempFileName);
            System.out.println("   Destino: " + mp3FileName);
            System.out.println("========================================");

            convertToMP3(tempFilePath, mp3FilePath);

            long mp3FileSize = Files.size(mp3FilePath);

            System.out.println("========================================");
            System.out.println(" CONVERSIÓN A MP3 COMPLETADA");
            System.out.println("   Archivo MP3: " + mp3FileName);
            System.out.println("   Tamaño WebM: " + formatFileSize(tempFileSize));
            System.out.println("   Tamaño MP3: " + formatFileSize(mp3FileSize));
            System.out.println("   Ruta: " + mp3FilePath.toAbsolutePath());
            System.out.println("========================================");

            // Validar MP3
            if (mp3FileSize < 10240) {
                System.out.println("️ ADVERTENCIA: MP3 muy pequeño (" + mp3FileSize + " bytes)");
            }

            // Eliminar archivo temporal
            Files.deleteIfExists(tempFilePath);
            System.out.println("️ Archivo temporal eliminado");

            //  GUARDAR RUTA EN BASE DE DATOS
            session.setAudioRecordingUrl(mp3FileName);
            sessionRepository.save(session);

            System.out.println("========================================");
            System.out.println(" NOMBRE DE ARCHIVO GUARDADO EN BD");
            System.out.println("   Campo: audio_recording_url");
            System.out.println("   Valor: " + mp3FileName);
            System.out.println("========================================");

            // Guardar URL en memoria también
            recordingUrls.put(sessionId, RECORDINGS_DIR + mp3FileName);
            activeRecordings.remove(sessionId);

            System.out.println("========================================");
            System.out.println(" PROCESO COMPLETADO EXITOSAMENTE");
            System.out.println("   Estado: Listo para transcripción");
            System.out.println("   Guardado en BD: ");
            System.out.println("========================================");

            // ⭐⭐⭐ TRIGGER TRANSCRIPCIÓN AUTOMÁTICA (AQUÍ ES EL LUGAR CORRECTO) ⭐⭐⭐
            System.out.println("========================================");
            System.out.println(" INICIANDO TRANSCRIPCIÓN AUTOMÁTICA");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("   Archivo MP3: " + mp3FileName);
            System.out.println("========================================");

            try {
                transcriptionService.transcribeSessionAudio(sessionId)
                        .thenAccept(transcriptionResult -> {
                            if (transcriptionResult.isSuccess()) {
                                System.out.println("========================================");
                                System.out.println(" TRANSCRIPCIÓN AUTOMÁTICA COMPLETADA");
                                System.out.println("   Session ID: " + sessionId);
                                System.out.println("========================================");
                            } else {
                                System.err.println("========================================");
                                System.err.println(" ERROR EN TRANSCRIPCIÓN AUTOMÁTICA");
                                System.err.println("   Session ID: " + sessionId);
                                System.err.println("   Error: " + transcriptionResult.getErrorMessage());
                                System.err.println("========================================");
                            }
                        });

                System.out.println(" Transcripción iniciada en segundo plano");

            } catch (Exception e) {
                System.err.println("========================================");
                System.err.println(" ERROR AL INICIAR TRANSCRIPCIÓN");
                System.err.println("   Error: " + e.getMessage());
                System.err.println("========================================");
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
            System.out.println(" VERIFICANDO CONTENIDO DE AUDIO...");

            MultimediaObject source = new MultimediaObject(filePath.toFile());
            ws.schild.jave.info.MultimediaInfo info = source.getInfo();

            System.out.println(" Información del archivo:");
            System.out.println("   Duración: " + (info.getDuration() / 1000.0) + " segundos");
            System.out.println("   Formato: " + info.getFormat());

            if (info.getAudio() == null) {
                System.out.println(" NO TIENE PISTA DE AUDIO");
                return false;
            }

            System.out.println(" Tiene pista de audio:");
            System.out.println("   Codec: " + info.getAudio().getDecoder());
            System.out.println("   Bitrate: " + info.getAudio().getBitRate() + " bps");
            System.out.println("   Sample rate: " + info.getAudio().getSamplingRate() + " Hz");
            System.out.println("   Canales: " + info.getAudio().getChannels());

            //  VALIDACIÓN MEJORADA: Solo verificar que tenga sample rate válido
            if (info.getAudio().getSamplingRate() == 0) {
                System.out.println(" ADVERTENCIA: Sample rate es 0");
                return false;
            }

            //  Permitir duraciones cortas o negativas (problema común con WebM)
            // El archivo será válido si tiene codec y sample rate correctos
            System.out.println(" Archivo válido para conversión");
            return true;

        } catch (Exception e) {
            System.err.println(" No se pudo verificar el contenido: " + e.getMessage());
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

            System.out.println(" Configurando conversión...");

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            System.out.println("   Codec: libmp3lame");
            System.out.println("   Bitrate: 128 kbps");
            System.out.println("   Sample rate: 44100 Hz");
            System.out.println("   Canales: 2 (Stereo)");

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();

            System.out.println(" Convirtiendo...");
            long startTime = System.currentTimeMillis();

            encoder.encode(new MultimediaObject(source), target, attrs);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println(" Conversión exitosa en " + (duration / 1000.0) + " segundos");

        } catch (EncoderException e) {
            System.err.println("========================================");
            System.err.println(" ERROR EN CONVERSIÓN A MP3");
            System.err.println("   Mensaje: " + e.getMessage());

            if (e.getMessage().contains("Invalid data found")) {
                System.err.println("\n POSIBLE CAUSA:");
                System.err.println("   El archivo WebM no contiene audio válido");
            }

            System.err.println("========================================");
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

            System.out.println(" URL de grabación guardada");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("   Archivo: " + fileName);
            System.out.println("   Ruta BD: " + absolutePath);

        } catch (Exception e) {
            System.err.println(" Error guardando en BD: " + e.getMessage());
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
            System.err.println(" Error consultando BD: " + e.getMessage());
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
            System.err.println("Error estimando duración: " + e.getMessage());
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
                System.out.println("️ Grabación eliminada: " + filePath.getFileName());
            } catch (IOException e) {
                System.err.println(" Error al eliminar: " + e.getMessage());
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
            System.out.println(" Grabación limpiada para sesión: " + sessionId);
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
                System.err.println(" Error consultando BD: " + e.getMessage());
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