package com.project.skillswap.logic.entity.videocall;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
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
    //#endregion

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
     * ⭐ Procesa grabación de Jitsi: extrae audio y convierte a MP3
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