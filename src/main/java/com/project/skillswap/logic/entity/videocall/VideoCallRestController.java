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

    /**
     * 🗑️ Elimina transcripción (si necesitas regenerarla)
     */
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

            System.out.println("🗑️ Transcripción eliminada para sesión " + sessionId);

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
     *  Obtiene la transcripción de una sesión (solo para instructores)
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

            // 4️ Retornar datos
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


    /**
     * 📥 Popup de selección de formato (página intermedia)
     */
    @GetMapping("/transcription/{sessionId}/download")
    public ResponseEntity<String> downloadTranscriptionPage(@PathVariable Long sessionId) {
        try {
            System.out.println("========================================");
            System.out.println("📥 POPUP DE SELECCIÓN DE FORMATO");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("========================================");

            // Validar que existe la sesión y tiene transcripción
            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                String errorHtml = """
                <!DOCTYPE html>
                <html lang='es'>
                <head>
                    <meta charset='UTF-8'>
                    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                    <title>Transcripción no disponible</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                            background: #1a1a1a;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            margin: 0;
                        }
                        .container {
                            background: #2a2a2a;
                            padding: 40px;
                            border-radius: 15px;
                            box-shadow: 0 10px 40px rgba(0,0,0,0.5);
                            text-align: center;
                            max-width: 500px;
                            border: 1px solid #3a3a3a;
                        }
                        .error-icon { font-size: 64px; margin-bottom: 20px; }
                        h1 { color: #e74c3c; margin: 0 0 15px 0; }
                        p { color: #999; line-height: 1.6; }
                    </style>
                </head>
                <body>
                    <div class='container'>
                        <div class='error-icon'>⚠️</div>
                        <h1>Transcripción no disponible</h1>
                        <p>No hay transcripción disponible para esta sesión.</p>
                    </div>
                </body>
                </html>
                """;

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(org.springframework.http.MediaType.TEXT_HTML)
                        .body(errorHtml);
            }

            // Calcular estadísticas
            int wordCount = session.getFullText().split("\\s+").length;
            int durationSeconds = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0;

            // Página con popup modal
            String html = """
            <!DOCTYPE html>
            <html lang='es'>
            <head>
                <meta charset='UTF-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <title>Descargar Transcripción - SkillSwap</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background: linear-gradient(135deg, #1a1a1a 0%%, #2d2d2d 100%%);
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        padding: 20px;
                    }
                    
                    .modal {
                        background: #2a2a2a;
                        padding: 0;
                        border-radius: 20px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.5);
                        max-width: 500px;
                        width: 100%%;
                        animation: modalFadeIn 0.4s ease-in-out;
                        border: 1px solid #3a3a3a;
                        overflow: hidden;
                    }
                    
                    @keyframes modalFadeIn {
                        from {
                            opacity: 0;
                            transform: scale(0.9) translateY(-20px);
                        }
                        to {
                            opacity: 1;
                            transform: scale(1) translateY(0);
                        }
                    }
                    
                    .modal-header {
                        background: linear-gradient(135deg, #504ab7 0%%, #aae16b 100%%);
                        padding: 30px;
                        text-align: center;
                    }
                    
                    .modal-header h1 {
                        color: white;
                        font-size: 24px;
                        margin-bottom: 5px;
                        font-weight: 600;
                    }
                    
                    .modal-header p {
                        color: rgba(255,255,255,0.9);
                        font-size: 14px;
                    }
                    
                    .modal-body {
                        padding: 30px;
                    }
                    
                    .session-title {
                        text-align: center;
                        margin-bottom: 25px;
                    }
                    
                    .session-title h2 {
                        color: #aae16b;
                        font-size: 20px;
                        margin-bottom: 15px;
                    }
                    
                    .stats {
                        display: flex;
                        justify-content: space-around;
                        margin-bottom: 30px;
                    }
                    
                    .stat-item {
                        text-align: center;
                    }
                    
                    .stat-value {
                        color: #aae16b;
                        font-size: 24px;
                        font-weight: 700;
                        display: block;
                        margin-bottom: 5px;
                    }
                    
                    .stat-label {
                        color: #888;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }
                    
                    .divider {
                        height: 1px;
                        background: linear-gradient(90deg, transparent, #3a3a3a, transparent);
                        margin: 25px 0;
                    }
                    
                    .format-selector h3 {
                        color: #ccc;
                        font-size: 16px;
                        text-align: center;
                        margin-bottom: 20px;
                        font-weight: 500;
                    }
                    
                    .button-group {
                        display: flex;
                        gap: 15px;
                    }
                    
                    .download-btn {
                        flex: 1;
                        padding: 16px 20px;
                        border: none;
                        border-radius: 12px;
                        font-size: 15px;
                        font-weight: 600;
                        cursor: pointer;
                        text-decoration: none;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        gap: 8px;
                        transition: all 0.3s ease;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.3);
                    }
                    
                    .download-btn:hover {
                        transform: translateY(-3px);
                        box-shadow: 0 6px 20px rgba(0,0,0,0.4);
                    }
                    
                    .download-btn:active {
                        transform: translateY(-1px);
                    }
                    
                    .btn-txt {
                        background: linear-gradient(135deg, #aae16b 0%%, #8ec756 100%%);
                        color: #1a1a1a;
                    }
                    
                    .btn-txt:hover {
                        background: linear-gradient(135deg, #b8ed7a 0%%, #9dd665 100%%);
                    }
                    
                    .btn-pdf {
                        background: linear-gradient(135deg, #504ab7 0%%, #6b63d8 100%%);
                        color: white;
                    }
                    
                    .btn-pdf:hover {
                        background: linear-gradient(135deg, #6159c9 0%%, #7c74ea 100%%);
                    }
                    
                    .icon {
                        font-size: 32px;
                    }
                    
                    .format-label {
                        font-size: 14px;
                    }
                    
                    .format-desc {
                        font-size: 11px;
                        opacity: 0.8;
                    }
                    
                    @media (max-width: 600px) {
                        .modal {
                            margin: 20px;
                        }
                        
                        .modal-body {
                            padding: 25px 20px;
                        }
                        
                        .button-group {
                            flex-direction: column;
                        }
                    }
                </style>
            </head>
            <body>
                <div class='modal'>
                    <div class='modal-header'>
                        <h1>📥 Transcripción Disponible</h1>
                        <p>Seleccione el formato de descarga</p>
                    </div>
                    
                    <div class='modal-body'>
                        <div class='session-title'>
                            <h2>%s</h2>
                        </div>
                        
                        <div class='stats'>
                            <div class='stat-item'>
                                <span class='stat-value'>%d</span>
                                <span class='stat-label'>Minutos</span>
                            </div>
                            <div class='stat-item'>
                                <span class='stat-value'>%d</span>
                                <span class='stat-label'>Palabras</span>
                            </div>
                            <div class='stat-item'>
                                <span class='stat-value'>#%d</span>
                                <span class='stat-label'>Sesión</span>
                            </div>
                        </div>
                        
                        <div class='divider'></div>
                        
                        <div class='format-selector'>
                            <h3>Seleccione formato:</h3>
                        </div>
                        
                        <div class='button-group'>
                            <a href='/videocall/transcription/%d/download-txt' class='download-btn btn-txt'>
                                <span class='icon'>📝</span>
                                <span class='format-label'>Texto Plano</span>
                                <span class='format-desc'>Archivo .TXT</span>
                            </a>
                            
                            <a href='/videocall/transcription/%d/download-pdf' class='download-btn btn-pdf'>
                                <span class='icon'>📄</span>
                                <span class='format-label'>Documento</span>
                                <span class='format-desc'>Archivo .PDF</span>
                            </a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                    session.getTitle(),
                    durationSeconds / 60,
                    wordCount,
                    session.getId(),
                    session.getId(),
                    session.getId()
            );

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            System.err.println("❌ Error en popup de selección: " + e.getMessage());
            e.printStackTrace();

            String errorHtml = """
            <!DOCTYPE html>
            <html>
            <body style='font-family: Arial; background: #1a1a1a; color: #fff; text-align: center; padding: 50px;'>
                <h2>❌ Error</h2>
                <p>%s</p>
            </body>
            </html>
            """.formatted(e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }

    /**
     * 📝 Página de descarga TXT (idéntica a PDF pero descarga TXT)
     */
    @GetMapping("/transcription/{sessionId}/download-txt")
    public ResponseEntity<String> downloadTranscriptionTxtPage(@PathVariable Long sessionId) {
        try {
            System.out.println("========================================");
            System.out.println("📝 PÁGINA DE DESCARGA TXT");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("========================================");

            // Validar sesión
            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                String errorHtml = """
                <!DOCTYPE html>
                <html lang='es'>
                <head>
                    <meta charset='UTF-8'>
                    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                    <title>Transcripción no disponible</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            margin: 0;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 15px;
                            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                            text-align: center;
                            max-width: 500px;
                        }
                        .error-icon { font-size: 64px; margin-bottom: 20px; }
                        h1 { color: #e74c3c; margin: 0 0 15px 0; }
                        p { color: #555; line-height: 1.6; }
                    </style>
                </head>
                <body>
                    <div class='container'>
                        <div class='error-icon'>⚠️</div>
                        <h1>Transcripción no disponible</h1>
                        <p>No hay transcripción disponible para esta sesión.</p>
                    </div>
                </body>
                </html>
                """;

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(org.springframework.http.MediaType.TEXT_HTML)
                        .body(errorHtml);
            }

            // Calcular estadísticas
            int wordCount = session.getFullText().split("\\s+").length;
            int durationSeconds = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0;

            // Generar TXT en memoria
            StringBuilder content = new StringBuilder();
            content.append("===========================================\\n");
            content.append("TRANSCRIPCIÓN DE SESIÓN - SKILLSWAP\\n");
            content.append("===========================================\\n");
            content.append("Sesión: #").append(session.getId()).append("\\n");
            content.append("Título: ").append(session.getTitle()).append("\\n");

            if (session.getInstructor() != null && session.getInstructor().getPerson() != null) {
                content.append("Instructor: ").append(session.getInstructor().getPerson().getFullName()).append("\\n");
            }

            if (session.getSkill() != null) {
                content.append("Habilidad: ").append(session.getSkill().getName()).append("\\n");
            }

            content.append("Palabras: ").append(wordCount).append("\\n");
            content.append("Duración: ").append(durationSeconds / 60).append(" minutos ")
                    .append(durationSeconds % 60).append(" segundos\\n");
            content.append("Fecha: ").append(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                    .format(new java.util.Date())).append("\\n");
            content.append("===========================================\\n\\n");
            content.append(session.getFullText());

            String fileName = "transcripcion_sesion_" + session.getId() + "_" +
                    new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".txt";

            // Convertir a Base64 para JavaScript
            String base64Content = java.util.Base64.getEncoder()
                    .encodeToString(content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Página HTML idéntica al PDF
            String html = """
            <!DOCTYPE html>
            <html lang='es'>
            <head>
                <meta charset='UTF-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <title>Descargar Transcripción TXT - SkillSwap</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background: linear-gradient(135deg, #aae16b 0%%, #504ab7 100%%);
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        padding: 20px;
                    }
                    
                    .container {
                        background: white;
                        padding: 50px 40px;
                        border-radius: 20px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                        max-width: 600px;
                        width: 100%%;
                        animation: fadeIn 0.5s ease-in-out;
                    }
                    
                    @keyframes fadeIn {
                        from { opacity: 0; transform: translateY(-20px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    
                    .logo {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    
                    .logo h1 {
                        font-size: 32px;
                        background: linear-gradient(135deg, #aae16b 0%%, #504ab7 100%%);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        background-clip: text;
                        margin-bottom: 5px;
                    }
                    
                    .logo p {
                        color: #666;
                        font-size: 14px;
                    }
                    
                    .title {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    
                    .title h2 {
                        color: #333;
                        font-size: 24px;
                        margin-bottom: 10px;
                    }
                    
                    .title .format-badge {
                        display: inline-block;
                        background: linear-gradient(135deg, #aae16b 0%%, #8ec756 100%%);
                        color: #1a1a1a;
                        padding: 8px 20px;
                        border-radius: 20px;
                        font-size: 14px;
                        font-weight: 600;
                        margin-top: 10px;
                    }
                    
                    .info-box {
                        background: #f8f9fa;
                        border-left: 4px solid #aae16b;
                        padding: 20px;
                        margin-bottom: 30px;
                        border-radius: 8px;
                    }
                    
                    .info-box h3 {
                        color: #8ec756;
                        font-size: 18px;
                        margin-bottom: 15px;
                    }
                    
                    .info-row {
                        display: flex;
                        justify-content: space-between;
                        padding: 8px 0;
                        border-bottom: 1px solid #e0e0e0;
                    }
                    
                    .info-row:last-child {
                        border-bottom: none;
                    }
                    
                    .info-label {
                        color: #666;
                        font-weight: 500;
                    }
                    
                    .info-value {
                        color: #333;
                        font-weight: 600;
                    }
                    
                    .download-section {
                        text-align: center;
                        margin: 30px 0;
                    }
                    
                    .download-btn {
                        display: inline-block;
                        background: linear-gradient(135deg, #aae16b 0%%, #8ec756 100%%);
                        color: #1a1a1a;
                        padding: 18px 40px;
                        border: none;
                        border-radius: 12px;
                        font-size: 18px;
                        font-weight: 600;
                        cursor: pointer;
                        text-decoration: none;
                        box-shadow: 0 4px 15px rgba(170, 225, 107, 0.4);
                        transition: all 0.3s ease;
                    }
                    
                    .download-btn:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 6px 20px rgba(170, 225, 107, 0.6);
                        background: linear-gradient(135deg, #8ec756 0%%, #7ab847 100%%);
                    }
                    
                    .download-btn .icon {
                        font-size: 24px;
                        margin-right: 10px;
                    }
                    
                    .status {
                        text-align: center;
                        margin-top: 20px;
                        padding: 15px;
                        background: #e8f5e9;
                        border-radius: 8px;
                        color: #2e7d32;
                        font-weight: 500;
                        display: none;
                    }
                    
                    .status.show {
                        display: block;
                        animation: slideDown 0.3s ease-in-out;
                    }
                    
                    @keyframes slideDown {
                        from { opacity: 0; transform: translateY(-10px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    
                    .footer {
                        text-align: center;
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #e0e0e0;
                    }
                    
                    .footer p {
                        color: #999;
                        font-size: 12px;
                    }
                    
                    @media (max-width: 600px) {
                        .container {
                            padding: 30px 20px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class='container'>
                    <div class='logo'>
                        <h1>SkillSwap</h1>
                        <p>Plataforma de Intercambio de Conocimiento</p>
                    </div>
                    
                    <div class='title'>
                        <h2>📝 Descargar Transcripción</h2>
                        <span class='format-badge'>Formato: TXT</span>
                    </div>
                    
                    <div class='info-box'>
                        <h3>%s</h3>
                        <div class='info-row'>
                            <span class='info-label'>Sesión ID:</span>
                            <span class='info-value'>#%d</span>
                        </div>
                        <div class='info-row'>
                            <span class='info-label'>Palabras:</span>
                            <span class='info-value'>%d palabras</span>
                        </div>
                        <div class='info-row'>
                            <span class='info-label'>Duración:</span>
                            <span class='info-value'>%d minutos %d segundos</span>
                        </div>
                    </div>
                    
                    <div class='download-section'>
                        <button class='download-btn' onclick='downloadFile()'>
                            <span class='icon'>📝</span>
                            <span>Descargar Archivo TXT</span>
                        </button>
                    </div>
                    
                    <div class='status' id='status'>
                        ✅ Descarga iniciada. Revise su carpeta de descargas.
                    </div>
                    
                    <div class='footer'>
                        <p>© 2025 SkillSwap - Transcripción procesada con IA</p>
                        <p>Groq Whisper Large V3</p>
                    </div>
                </div>
                
                <script>
                    function downloadFile() {
                        // Decodificar contenido
                        const base64Content = '%s';
                        const binaryString = atob(base64Content);
                        const bytes = new Uint8Array(binaryString.length);
                        for (let i = 0; i < binaryString.length; i++) {
                            bytes[i] = binaryString.charCodeAt(i);
                        }
                        
                        // Crear blob
                        const blob = new Blob([bytes], { type: 'text/plain;charset=utf-8' });
                        const url = window.URL.createObjectURL(blob);
                        
                        // Crear link de descarga
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = '%s';
                        document.body.appendChild(a);
                        a.click();
                        
                        // Limpiar
                        window.URL.revokeObjectURL(url);
                        document.body.removeChild(a);
                        
                        // Mostrar mensaje
                        document.getElementById('status').classList.add('show');
                        
                        console.log('✅ Descarga iniciada: %s');
                    }
                    
                    // Auto-descarga después de 1 segundo
                    setTimeout(function() {
                        downloadFile();
                    }, 1000);
                </script>
            </body>
            </html>
            """.formatted(
                    session.getTitle(),
                    session.getId(),
                    wordCount,
                    durationSeconds / 60,
                    durationSeconds % 60,
                    base64Content,
                    fileName,
                    fileName
            );

            System.out.println("========================================");
            System.out.println("✅ PÁGINA TXT GENERADA");
            System.out.println("   Archivo: " + fileName);
            System.out.println("========================================");

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            System.err.println("❌ Error en página TXT: " + e.getMessage());
            e.printStackTrace();

            String errorHtml = """
            <!DOCTYPE html>
            <html>
            <body style='font-family: Arial; text-align: center; padding: 50px;'>
                <h2>❌ Error</h2>
                <p>%s</p>
            </body>
            </html>
            """.formatted(e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }

    /**
     * 📄 Descarga directa de archivo PDF
     */
    @GetMapping("/transcription/{sessionId}/download-pdf")
    public ResponseEntity<?> downloadTranscriptionPdf(@PathVariable Long sessionId) {
        try {
            System.out.println("========================================");
            System.out.println("📄 DESCARGA DE TRANSCRIPCIÓN PDF");
            System.out.println("   Session ID: " + sessionId);
            System.out.println("========================================");

            LearningSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            if (session.getFullText() == null || session.getFullText().isEmpty()) {
                System.out.println("⚠️ No hay transcripción disponible");
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
            System.out.println("✅ PDF LISTO PARA DESCARGA");
            System.out.println("========================================");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ ERROR AL DESCARGAR PDF");
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
    //#endregion
}