package com.project.skillswap.logic.entity.videocall;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionEmailService;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;

/**
 * Controlador REST para operaciones de videollamadas
 */
@RestController
@RequestMapping("/videocall")
@CrossOrigin(origins = "http://localhost:4200")
public class VideoCallRestController {

    //#region Dependencies

    @Autowired
    private VideoCallService videoCallService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private LearningSessionRepository sessionRepository;

    @Autowired
    private RecordingService recordingService;

    @Autowired
    private SessionDocumentService documentService;

    @Autowired
    private TranscriptionService transcriptionService;

    @Autowired
    private SessionEmailService sessionEmailService;

    @Autowired
    private SessionSummaryService summaryService;

    @Autowired
    private SessionSummaryPdfService summaryPdfService;

    @Autowired
    private SessionSummaryEmailService summaryEmailService;

    //#endregion


//  TRANSCRIPTION ENDPOINTS

    /**
     *  ENDPOINT DE PRUEBA: Enviar email de transcripción manualmente
     * Usar: GET http://localhost:8080/videocall/test-transcription-email/1464
     *
     *  ELIMINAR DESPUÉS DE CONFIRMAR QUE FUNCIONA
     */
    @GetMapping("/test-transcription-email/{sessionId}")
    public ResponseEntity<Map<String, Object>> testTranscriptionEmail(@PathVariable Long sessionId) {
        System.out.println("========================================");
        System.out.println(" TEST EMAIL - Iniciando prueba manual");
        System.out.println("   Session ID: " + sessionId);
        System.out.println("========================================");

        try {
            // Obtener sesión
            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            // Obtener instructor
            Person instructor = session.getInstructor().getPerson();

            System.out.println(" Instructor: " + instructor.getEmail());
            System.out.println(" Tiene transcripción: " + (session.getFullText() != null));

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "La sesión no tiene transcripción",
                        "sessionId", sessionId
                ));
            }

            // Intentar enviar email
            boolean emailSent = sessionEmailService.sendTranscriptionReadyEmail(session, instructor);

            Map<String, Object> response = new HashMap<>();
            response.put("success", emailSent);
            response.put("message", emailSent ? "Email enviado exitosamente" : "Error al enviar email");
            response.put("sessionId", sessionId);
            response.put("instructorEmail", instructor.getEmail());
            response.put("transcriptionLength", session.getFullText().length());

            System.out.println("========================================");
            System.out.println(emailSent ? " TEST EXITOSO" : " TEST FALLIDO");
            System.out.println("========================================");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println(" ERROR EN TEST DE EMAIL");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage(),
                    "sessionId", sessionId
            ));
        }
    }

    /**
     *  Inicia transcripción de audio de sesión
     * Se ejecuta automáticamente después de detener la grabación
     */
    @PostMapping("/transcription/start/{sessionId}")
    public ResponseEntity<Map<String, Object>> startTranscription(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede iniciar la transcripción"));
            }

            System.out.println("========================================");
            System.out.println(" INICIANDO PROCESO DE TRANSCRIPCIÓN");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("   Solicitado por: " + person.getFullName());
            System.out.println("========================================");

            // Iniciar transcripción asíncrona
            transcriptionService.transcribeSessionAudio(sessionId)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            System.out.println(" Transcripción completada para sesión " + sessionId);
                        } else {
                            System.err.println(" Transcripción falló para sesión " + sessionId + ": " + result.getErrorMessage());
                        }
                    });

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transcripción iniciada en segundo plano");
            response.put("sessionId", sessionId);
            response.put("status", "PROCESSING");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" Error al iniciar transcripción: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al iniciar transcripción: " + e.getMessage()
                    ));
        }
    }

    /**
     *  Obtiene estado y resultado de transcripción
     */
    @GetMapping("/transcription/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getTranscriptionStatus(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            Map<String, Object> status = new HashMap<>();
            status.put("sessionId", sessionId);

            if (session.getFullText() != null && !session.getFullText().isEmpty()) {
                // Transcripción completada
                status.put("status", "COMPLETED");
                status.put("hasTranscription", true);
                status.put("transcription", session.getFullText());
                status.put("durationSeconds", session.getDurationSeconds());
                status.put("processingDate", session.getProcessingDate());
                status.put("wordCount", countWords(session.getFullText()));

            } else if (session.getProcessingDate() != null) {
                // En proceso
                status.put("status", "PROCESSING");
                status.put("hasTranscription", false);
                status.put("message", "Transcripción en proceso");

            } else if (session.getAudioRecordingUrl() != null) {
                // Tiene audio pero no transcripción
                status.put("status", "READY_TO_TRANSCRIBE");
                status.put("hasTranscription", false);
                status.put("message", "Audio disponible, listo para transcribir");

            } else {
                // Sin audio
                status.put("status", "NO_AUDIO");
                status.put("hasTranscription", false);
                status.put("message", "No hay grabación de audio");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Estado obtenido exitosamente");
            response.put("data", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener estado: " + e.getMessage()));
        }
    }

    @DeleteMapping("/transcription/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteTranscription(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede eliminar la transcripción"));
            }

            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            session.setFullText(null);
            session.setDurationSeconds(null);
            session.setProcessingDate(null);
            sessionRepository.save(session);

            System.out.println(" Transcripción eliminada para sesión " + sessionId);

            return ResponseEntity.ok(Map.of(
                    "message", "Transcripción eliminada exitosamente",
                    "sessionId", sessionId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al eliminar transcripción: " + e.getMessage()));
        }
    }

    /**
     *  Cuenta palabras en texto
     */
    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    //#region Video Call Endpoints
    /**
     * Genera token y datos para unirse a una videollamada
     */
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinVideoCall(@RequestBody Map<String, Object> request) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (request.get("sessionId") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "sessionId es requerido"));
            }

            Long sessionId = Long.valueOf(request.get("sessionId").toString());

            String joinLink = request.get("joinLink") != null
                    ? request.get("joinLink").toString()
                    : null;

            if (joinLink != null && !joinLink.isEmpty() &&
                    !videoCallService.validateVideoCallLink(sessionId, joinLink)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Enlace no válido o sesión no disponible"));
            }

            boolean isModerator = isSessionInstructor(person, sessionId);

            System.out.println("========================================");
            System.out.println(" DETERMINANDO ROL DE USUARIO");
            System.out.println("   Usuario: " + person.getFullName());
            System.out.println("   Email: " + person.getEmail());
            System.out.println("   Session ID: " + sessionId);
            System.out.println("   Es instructor de ESTA sesión: " + isModerator);
            System.out.println("========================================");

            Map<String, Object> videoCallData = videoCallService.generateVideoCallToken(sessionId, person, isModerator);

            videoCallData.put("cameraEnabled", request.getOrDefault("cameraEnabled", true));
            videoCallData.put("microphoneEnabled", request.getOrDefault("microphoneEnabled", true));

            videoCallService.registerParticipantJoin(sessionId, person.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Unión exitosa a videollamada");
            response.put("data", videoCallData);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "sessionId debe ser un número válido"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al unirse a videollamada: " + e.getMessage()));
        }
    }

    @GetMapping("/info/{sessionId}")
    public ResponseEntity<Map<String, Object>> getVideoCallInfo(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            Map<String, Object> videoCallInfo = videoCallService.getVideoCallInfo(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Información obtenida exitosamente");
            response.put("data", videoCallInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener información: " + e.getMessage()));
        }
    }

    @PostMapping("/validate-screen-share")
    public ResponseEntity<Map<String, Object>> validateScreenShare(@RequestBody Map<String, Object> request) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            Long sessionId = Long.valueOf(request.get("sessionId").toString());
            boolean canShare = isSessionInstructor(person, sessionId);

            if (!canShare) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "message", "No posee los permisos necesarios para compartir pantalla",
                                "canShareScreen", false
                        ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Permiso concedido para compartir pantalla");
            response.put("canShareScreen", true);
            response.put("personId", person.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al validar permisos: " + e.getMessage()));
        }
    }

    @PostMapping("/end/{sessionId}")
    public ResponseEntity<Map<String, Object>> endVideoCall(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede finalizar la sesión"));
            }

            recordingService.stopRecording(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Videollamada finalizada exitosamente");
            response.put("sessionId", sessionId);
            response.put("endedAt", new java.util.Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al finalizar videollamada: " + e.getMessage()));
        }
    }
    //#endregion

    //#region Recording Endpoints
    @PostMapping("/recording/start/{sessionId}")
    public ResponseEntity<Map<String, Object>> startRecording(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede iniciar la grabación"));
            }

            Map<String, Object> result = recordingService.startRecording(sessionId, person.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Grabación iniciada exitosamente");
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al iniciar grabación: " + e.getMessage()));
        }
    }

    @PostMapping("/recording/stop/{sessionId}")
    public ResponseEntity<Map<String, Object>> stopRecording(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede detener la grabación"));
            }

            Map<String, Object> result = recordingService.stopRecording(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Grabación detenida exitosamente");
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al detener grabación: " + e.getMessage()));
        }
    }

    @GetMapping("/recording/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getRecordingStatus(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            Map<String, Object> status = recordingService.getRecordingStatus(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Estado obtenido exitosamente");
            response.put("data", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener estado: " + e.getMessage()));
        }
    }

    @PostMapping("/recording/upload/{sessionId}")
    public ResponseEntity<Map<String, Object>> uploadRecording(
            @PathVariable Long sessionId,
            @RequestParam("audio") MultipartFile audioFile) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede subir grabaciones"));
            }

            System.out.println(" Recibiendo archivo de audio...");
            System.out.println("   Nombre: " + audioFile.getOriginalFilename());
            System.out.println("   Tamaño: " + audioFile.getSize() + " bytes");
            System.out.println("   Tipo: " + audioFile.getContentType());

            Map<String, Object> result = recordingService.saveRecordingFile(sessionId, audioFile);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grabación subida y convertida a MP3 exitosamente",
                    "data", result
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error: " + e.getMessage()
                    ));
        }
    }

    /**
     *  Usuario sube grabación descargada de Jitsi
     */
    @PostMapping("/recording/upload-jitsi-file/{sessionId}")
    public ResponseEntity<?> uploadJitsiFile(
            @PathVariable Long sessionId,
            @RequestParam("file") MultipartFile file) {

        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Solo el instructor puede subir grabaciones"));
            }

            System.out.println("========================================");
            System.out.println(" RECIBIENDO ARCHIVO DE JITSI");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("   Archivo: " + file.getOriginalFilename());
            System.out.println("   Tamaño: " + formatFileSize(file.getSize()));
            System.out.println("========================================");

            // Validar que es un archivo de video
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("video/") && !contentType.equals("application/octet-stream"))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El archivo debe ser un video"));
            }

            // Crear directorio temporal
            String tempDir = "recordings/temp/";
            Path tempPath = Paths.get(tempDir);
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
            }

            // Guardar video temporal
            String videoFileName = "temp_jitsi_" + sessionId + "_" + System.currentTimeMillis() + ".webm";
            Path videoPath = Paths.get(tempDir + videoFileName);

            Files.copy(file.getInputStream(), videoPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println(" Video temporal guardado");
            System.out.println("   Ruta: " + videoPath.toAbsolutePath());

            // Procesar en segundo plano
            CompletableFuture.runAsync(() -> {
                processJitsiRecording(sessionId, videoPath.toString());
            });

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grabación subida y procesándose en segundo plano",
                    "fileName", file.getOriginalFilename()
            ));

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     *  Procesa grabación de Jitsi: extrae audio y convierte a MP3
     */
    private void processJitsiRecording(Long sessionId, String videoPathStr) {
        try {
            System.out.println("========================================");
            System.out.println(" PROCESANDO GRABACIÓN DE JITSI");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("========================================");

            Path videoPath = Paths.get(videoPathStr);
            String recordingsDir = "recordings/audio/";
            Path recordingsPath = Paths.get(recordingsDir);
            if (!Files.exists(recordingsPath)) {
                Files.createDirectories(recordingsPath);
            }

            // Nombre del archivo de audio final
            String audioFileName = "session_" + sessionId + "_jitsi_" + System.currentTimeMillis() + ".mp3";
            Path audioPath = Paths.get(recordingsDir + audioFileName);

            System.out.println(" Extrayendo audio del video con FFmpeg...");
            System.out.println("   Video origen: " + videoPath.getFileName());
            System.out.println("   Audio destino: " + audioFileName);

            // Ejecutar FFmpeg
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", videoPath.toString(),
                    "-vn",  // Sin video
                    "-acodec", "libmp3lame",
                    "-ab", "128k",
                    "-ar", "44100",
                    "-y",  // Sobrescribir si existe
                    audioPath.toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Leer output de FFmpeg (para debugging)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Duration") || line.contains("time=") || line.contains("error")) {
                        System.out.println("   FFmpeg: " + line);
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && Files.exists(audioPath)) {
                long audioSize = Files.size(audioPath);

                System.out.println("========================================");
                System.out.println(" AUDIO EXTRAÍDO EXITOSAMENTE");
                System.out.println("   Archivo: " + audioFileName);
                System.out.println("   Tamaño: " + formatFileSize(audioSize));
                System.out.println("   Ruta: " + audioPath.toAbsolutePath());
                System.out.println("========================================");

                // Guardar URL del audio
                recordingService.saveRecordingUrl(sessionId, audioFileName);

                // Eliminar video temporal
                Files.deleteIfExists(videoPath);
                System.out.println(" Video temporal eliminado");

            } else {
                System.err.println(" Error en FFmpeg (exit code: " + exitCode + ")");
                System.err.println("   ¿El archivo existe? " + Files.exists(audioPath));
            }

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println(" ERROR PROCESANDO GRABACIÓN");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
        }
    }

    @DeleteMapping("/recording/clear/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearRecording(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede limpiar grabaciones"));
            }

            recordingService.forceStopRecording(sessionId);

            return ResponseEntity.ok(Map.of(
                    "message", "Grabación limpiada exitosamente",
                    "sessionId", sessionId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @GetMapping("/recording/{sessionId}/url")
    public ResponseEntity<Map<String, Object>> getRecordingUrl(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            Map<String, Object> recordingInfo = recordingService.getRecordingUrl(sessionId);

            return ResponseEntity.ok(Map.of(
                    "message", "URL de grabación obtenida",
                    "data", recordingInfo
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener URL: " + e.getMessage()));
        }
    }

    /**
     *  Obtiene la transcripción de una sesión (solo instructores)
     */
    @GetMapping("/transcription/{sessionId}")
    public ResponseEntity<?> getTranscription(@PathVariable Long sessionId) {
        try {
            System.out.println("========================================");
            System.out.println(" SOLICITUD DE TRANSCRIPCIÓN");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("========================================");

            //  Obtener la sesión
            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            //  Validar que existe transcripción
            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                System.out.println(" No hay transcripción disponible para esta sesión");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "No hay transcripción disponible para esta sesión"
                        ));
            }

            //  Calcular estadísticas
            String fullText = session.getFullText();
            int wordCount = fullText.split("\\s+").length;
            int durationSeconds = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0;

            System.out.println(" Transcripción encontrada");
            System.out.println("   Palabras: " + wordCount);
            System.out.println("   Duración: " + durationSeconds + " segundos");
            System.out.println("   Caracteres: " + fullText.length());


            Map<String, Object> transcriptionData = new HashMap<>();
            transcriptionData.put("transcription", fullText);
            transcriptionData.put("wordCount", wordCount);
            transcriptionData.put("durationSeconds", durationSeconds);
            transcriptionData.put("processingDate", session.getProcessingDate());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Transcripción obtenida exitosamente",
                    "data", transcriptionData
            ));

        } catch (Exception e) {
            System.err.println(" Error al obtener transcripción: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener la transcripción: " + e.getMessage()
                    ));
        }
    }



    @GetMapping("/transcription/{sessionId}/download-txt")
    public ResponseEntity<?> downloadTranscriptionTxt(@PathVariable Long sessionId) {
        try {
            System.out.println("========================================");
            System.out.println(" DESCARGA DIRECTA TXT");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("========================================");

            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No hay transcripción disponible".getBytes());
            }

            String fullText = session.getFullText();
            int wordCount = fullText.split("\\s+").length;
            int durationSeconds = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0;

            StringBuilder content = new StringBuilder();
            content.append("===========================================\n");
            content.append("TRANSCRIPCIÓN DE SESIÓN - SKILLSWAP\n");
            content.append("===========================================\n");
            content.append("Sesión: #").append(session.getId()).append("\n");
            content.append("Título: ").append(session.getTitle()).append("\n");

            if (session.getInstructor() != null && session.getInstructor().getPerson() != null) {
                content.append("Instructor: ").append(session.getInstructor().getPerson().getFullName()).append("\n");
            }

            if (session.getSkill() != null) {
                content.append("Habilidad: ").append(session.getSkill().getName()).append("\n");
            }

            content.append("Palabras: ").append(wordCount).append("\n");
            content.append("Duración: ").append(durationSeconds / 60).append(" minutos ")
                    .append(durationSeconds % 60).append(" segundos\n");
            content.append("Fecha: ").append(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                    .format(new java.util.Date())).append("\n");
            content.append("===========================================\n\n");
            content.append(fullText);

            String fileName = "transcripcion_sesion_" + session.getId() + "_" +
                    new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".txt";

            byte[] contentBytes = content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.TEXT_PLAIN);
            headers.setContentLength(contentBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            headers.set("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" +
                            java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));

            System.out.println(" Descarga TXT iniciada: " + fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(contentBytes);

        } catch (Exception e) {
            System.err.println(" Error descargando TXT: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error: " + e.getMessage()).getBytes());
        }
    }


    /**
     *  Descarga directa de archivo PDF
     */
    @GetMapping("/transcription/{sessionId}/download-pdf")
    public ResponseEntity<?> downloadTranscriptionPdf(@PathVariable Long sessionId) {
        try {
            System.out.println("========================================");
            System.out.println(" DESCARGA DE TRANSCRIPCIÓN PDF");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("========================================");

            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                System.out.println(" No hay transcripción disponible");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No hay transcripción disponible para esta sesión".getBytes());
            }

            // Generar PDF usando el servicio
            TranscriptionPdfService pdfService = new TranscriptionPdfService();
            byte[] pdfBytes = pdfService.generateTranscriptionPdf(session);

            String fileName = "transcripcion_sesion_" + session.getId() + "_" +
                    new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".pdf";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            headers.set("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" +
                            java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));

            System.out.println("========================================");
            System.out.println(" PDF LISTO PARA DESCARGA");
            System.out.println("========================================");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println(" ERROR AL DESCARGAR PDF");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error al descargar PDF: " + e.getMessage())
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    //#endregion

    //#region Document Endpoints
    @GetMapping("/sessions/{sessionId}/documents")
    public ResponseEntity<Map<String, Object>> getSessionDocuments(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            List<Map<String, Object>> documents = documentService.getSessionDocuments(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Documentos obtenidos exitosamente");
            response.put("data", documents);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener documentos: " + e.getMessage()));
        }
    }

    @PostMapping("/sessions/{sessionId}/documents")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @PathVariable Long sessionId,
            @RequestParam("file") MultipartFile file) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            Map<String, Object> document = documentService.uploadDocument(sessionId, person.getId(), file);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Documento subido exitosamente");
            response.put("data", document);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al subir documento: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/{sessionId}/documents/{documentId}/download")
    public ResponseEntity<?> downloadDocument(
            @PathVariable Long sessionId,
            @PathVariable Long documentId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            return documentService.downloadDocument(documentId);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/sessions/{sessionId}/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @PathVariable Long sessionId,
            @PathVariable Long documentId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            boolean isModerator = isSessionInstructor(person, sessionId);
            documentService.deleteDocument(documentId, person.getId(), isModerator);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Documento eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al eliminar documento: " + e.getMessage()));
        }
    }
    //#endregion

    //#region Private Methods
    private Person getAuthenticatedPerson() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        return personRepository.findByEmail(email).orElse(null);
    }

    private boolean isSessionInstructor(Person person, Long sessionId) {
        try {
            if (!isInstructor(person)) {
                return false;
            }

            LearningSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null) {
                return false;
            }

            Long sessionInstructorId = session.getInstructor().getId();
            Long personInstructorId = person.getInstructor().getId();

            return sessionInstructorId.equals(personInstructorId);

        } catch (Exception e) {
            System.err.println("Error al verificar instructor: " + e.getMessage());
            return false;
        }
    }

    private boolean isInstructor(Person person) {
        return person.getInstructor() != null && person.getInstructor().getId() != null;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }


// ========================================
// SUMMARY ENDPOINTS (RESÚMENES CON IA)
// ========================================

    /**
     *  Genera resumen de sesión con IA y lo envía por email a participantes
     * POST /videocall/summary/generate/{sessionId}
     */
    @PostMapping("/summary/generate/{sessionId}")
    public ResponseEntity<Map<String, Object>> generateAndSendSummary(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede generar resúmenes"));
            }

            System.out.println("========================================");
            System.out.println(" GENERANDO RESUMEN DE SESIÓN");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("   Solicitado por: " + person.getFullName());
            System.out.println("========================================");

            // 1. Obtener sesión
            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            // 2. Validar que tenga transcripción
            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "La sesión no tiene transcripción. Genera primero la transcripción."
                        ));
            }

            // 3. Generar resumen con IA
            String summary = summaryService.generateSummary(session);

            // 4. Validar resumen
            if (!summaryService.validateSummary(summary)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "success", false,
                                "message", "El resumen generado no cumple con los requisitos de calidad"
                        ));
            }

            // 5. Generar PDF del resumen
            byte[] summaryPdf = summaryPdfService.generateSummaryPdf(session, summary);

            // 6. Enviar a todos los participantes
            boolean emailsSent = summaryEmailService.sendSummaryToParticipants(session, summaryPdf, summary);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", emailsSent
                    ? "Resumen generado y enviado a todos los participantes"
                    : "Resumen generado pero no se pudieron enviar emails");
            response.put("sessionId", sessionId);
            response.put("summaryLength", summary.length());
            response.put("emailsSent", emailsSent);

            System.out.println("========================================");
            System.out.println(" PROCESO COMPLETADO");
            System.out.println("   Resumen generado: ");
            System.out.println("   PDF creado: ");
            System.out.println("   Emails enviados: " + (emailsSent ? "" : ""));
            System.out.println("========================================");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println(" ERROR GENERANDO RESUMEN");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al generar resumen: " + e.getMessage()
                    ));
        }
    }

    /**
     * Obtiene el resumen de una sesión (texto plano)
     * GET /videocall/summary/{sessionId}
     */
    @GetMapping("/summary/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSummary(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));


            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "No hay transcripción disponible para esta sesión"
                        ));
            }

            String summary = summaryService.generateSummary(session);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("summary", summary);
            response.put("wordCount", summary.split("\\s+").length);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error obteniendo resumen: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener resumen: " + e.getMessage()
                    ));
        }
    }

    /**
     * Descarga el resumen en PDF
     * GET /videocall/summary/{sessionId}/download-pdf
     */
    @GetMapping("/summary/{sessionId}/download-pdf")
    public ResponseEntity<?> downloadSummaryPdf(@PathVariable Long sessionId) {
        try {
            System.out.println("========================================");
            System.out.println(" DESCARGA DE RESUMEN PDF");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("========================================");

            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Usuario no autenticado".getBytes());
            }

            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No hay transcripción disponible".getBytes());
            }

            // Generar resumen
            String summary = summaryService.generateSummary(session);

            // Generar PDF
            byte[] pdfBytes = summaryPdfService.generateSummaryPdf(session, summary);

            String fileName = "resumen_sesion_" + session.getId() + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            headers.set("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" +
                            java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));

            System.out.println("========================================");
            System.out.println(" PDF DE RESUMEN LISTO");
            System.out.println("   Archivo: " + fileName);
            System.out.println("========================================");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println(" ERROR DESCARGANDO PDF");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error al descargar PDF: " + e.getMessage())
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     * Reenvía el resumen por email (manual)
     * POST /videocall/summary/{sessionId}/resend-email
     */
    @PostMapping("/summary/{sessionId}/resend-email")
    public ResponseEntity<Map<String, Object>> resendSummaryEmail(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }

            if (!isSessionInstructor(person, sessionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo el instructor puede reenviar resúmenes"));
            }

            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "La sesión no tiene transcripción"
                        ));
            }

            // Generar resumen y PDF
            String summary = summaryService.generateSummary(session);
            byte[] summaryPdf = summaryPdfService.generateSummaryPdf(session, summary);

            // Reenviar emails
            boolean emailsSent = summaryEmailService.sendSummaryToParticipants(session, summaryPdf, summary);

            return ResponseEntity.ok(Map.of(
                    "success", emailsSent,
                    "message", emailsSent
                            ? "Resumen reenviado exitosamente"
                            : "No se pudieron enviar los emails",
                    "sessionId", sessionId
            ));

        } catch (Exception e) {
            System.err.println("Error reenviando resumen: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al reenviar resumen: " + e.getMessage()
                    ));
        }
    }

    //#endregion
}