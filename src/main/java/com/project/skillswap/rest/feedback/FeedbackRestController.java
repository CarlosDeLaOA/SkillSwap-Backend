package com.project.skillswap.rest.feedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.Feedback.Feedback;
import com.project.skillswap.logic.entity.Feedback.FeedbackService;
import com.project.skillswap.logic.entity.Feedback.FeedbackAudioService;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/feedbacks")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FeedbackRestController {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackRestController.class);

    //#region Dependencies
    private final FeedbackService feedbackService;
    private final FeedbackAudioService feedbackAudioService;
    private final LearningSessionRepository learningSessionRepository;
    private final PersonRepository personRepository;
    //#endregion

    //#region Constructor
    public FeedbackRestController(
            FeedbackService feedbackService,
            FeedbackAudioService feedbackAudioService,
            LearningSessionRepository learningSessionRepository,
            PersonRepository personRepository) {
        this.feedbackService = feedbackService;
        this.feedbackAudioService = feedbackAudioService;
        this.learningSessionRepository = learningSessionRepository;
        this.personRepository = personRepository;
    }
    //#endregion

    //#region Audio Recording Endpoints

    /**
     * Sube archivo de audio MP3 a Cloudinary para un feedback
     * POST /feedbacks/{sessionId}/upload-audio
     * @param sessionId ID de la sesion
     * @param audioFile Archivo de audio MP3
     * @param durationSeconds Duracion en segundos
     * @return ResponseEntity con resultado de la subida
     */
    @PostMapping("/{sessionId}/upload-audio")
    public ResponseEntity<Map<String, Object>> uploadAudio(
            @PathVariable Long sessionId,
            @RequestParam(value = "file", required = false) MultipartFile audioFileFromFile,
            @RequestParam(value = "audio", required = false) MultipartFile audioFileFromAudio,
            @RequestParam(value = "duration", defaultValue = "0") Integer durationSeconds) {
        try {
            MultipartFile audioFile = audioFileFromFile != null ? audioFileFromFile : audioFileFromAudio;

            if (audioFile == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "No se recibió archivo de audio"));
            }

            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Usuario no autenticado"));
            }

            LearningSession session = learningSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesion no encontrada"));

            Learner learner = person.getLearner();
            if (learner == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Solo learners pueden dejar feedback"));
            }

            logger.info("========================================");
            logger.info("[FeedbackRestController] Subiendo audio para feedback");
            logger.info("   Session ID: " + sessionId);
            logger.info("   Learner: " + person.getFullName());
            logger.info("   Archivo: " + audioFile.getOriginalFilename());
            logger.info("   Tamaño: " + audioFile.getSize() + " bytes");
            logger.info("   Duracion: " + durationSeconds + " segundos");
            logger.info("========================================");

            if (durationSeconds > 120) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "La duracion maxima es de 2 minutos (120 segundos)"));
            }

            Optional<Feedback> existingFeedback = feedbackService.getFeedbackBySessionAndLearner(session, learner);
            Feedback feedback;

            if (existingFeedback.isPresent()) {
                feedback = existingFeedback.get();
                if (feedback.getAudioUrl() != null) {
                    feedbackAudioService.deleteAudio(feedback.getId());
                }
            } else {
                feedback = new Feedback();
                feedback.setLearningSession(session);
                feedback.setLearner(learner);
                feedback = feedbackService.saveFeedback(feedback);
            }

            logger.info("   Feedback ID: " + feedback.getId());

            Map<String, Object> uploadResult = feedbackAudioService.uploadAudioToCloudinary(
                    feedback.getId(), audioFile, durationSeconds);

            logger.info("[FeedbackRestController] Audio subido exitosamente");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Audio subido exitosamente",
                    "data", uploadResult
            ));

        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error subiendo audio: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Obtiene el estado de transcripcion de un feedback
     * GET /feedbacks/{sessionId}/audio-status
     * @param sessionId ID de la sesion
     * @return ResponseEntity con estado de transcripcion
     */
    @GetMapping("/{sessionId}/audio-status")
    public ResponseEntity<Map<String, Object>> getAudioStatus(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Usuario no autenticado"));
            }

            LearningSession session = learningSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesion no encontrada"));

            Learner learner = person.getLearner();
            if (learner == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No autorizado"));
            }

            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackBySessionAndLearner(session, learner);

            if (feedbackOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "status", "NO_FEEDBACK",
                        "message", "No hay feedback registrado"
                ));
            }

            Feedback feedback = feedbackOpt.get();
            Map<String, Object> status = feedbackAudioService.getTranscriptionStatus(feedback.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", status
            ));

        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Elimina audio grabado de un feedback
     * DELETE /feedbacks/{sessionId}/audio
     * @param sessionId ID de la sesion
     * @return ResponseEntity con resultado de eliminacion
     */
    @DeleteMapping("/{sessionId}/audio")
    public ResponseEntity<Map<String, Object>> deleteAudio(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Usuario no autenticado"));
            }

            LearningSession session = learningSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesion no encontrada"));

            Learner learner = person.getLearner();
            if (learner == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No autorizado"));
            }

            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackBySessionAndLearner(session, learner);

            if (feedbackOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Feedback feedback = feedbackOpt.get();
            Map<String, Object> result = feedbackAudioService.deleteAudio(feedback.getId());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error eliminando audio: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    //#endregion

    //#region Feedback Submission Endpoints

    /**
     * Obtiene el feedback de una sesion para el learner autenticado
     * GET /feedbacks/session/{sessionId}
     * @param sessionId ID de la sesion
     * @return ResponseEntity con datos del feedback
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getFeedbackForSession(@PathVariable Long sessionId) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Usuario no autenticado"));
            }

            LearningSession session = learningSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesion no encontrada"));

            Learner learner = person.getLearner();
            if (learner == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No autorizado"));
            }

            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackBySessionAndLearner(session, learner);

            if (feedbackOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", null,
                        "message", "No hay feedback registrado"
                ));
            }

            Feedback feedback = feedbackOpt.get();
            Map<String, Object> feedbackData = buildFeedbackResponse(feedback);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", feedbackData
            ));

        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo feedback: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Envia/actualiza feedback con rating y comentario
     * POST /feedbacks/{sessionId}/submit
     * @param sessionId ID de la sesion
     * @param feedbackData Mapa con rating y comentario
     * @return ResponseEntity con resultado del envio
     */
    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> feedbackData) {
        try {
            Person person = getAuthenticatedPerson();
            if (person == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Usuario no autenticado"));
            }

            LearningSession session = learningSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesion no encontrada"));

            Learner learner = person.getLearner();
            if (learner == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Solo learners pueden dejar feedback"));
            }

            Integer rating = null;
            String comment = null;

            if (feedbackData.containsKey("rating")) {
                rating = ((Number) feedbackData.get("rating")).intValue();

                if (rating < 1 || rating > 5) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "El rating debe estar entre 1 y 5"));
                }
            }

            if (feedbackData.containsKey("comment")) {
                comment = (String) feedbackData.get("comment");
            }

            logger.info("========================================");
            logger.info("[FeedbackRestController] Enviando feedback");
            logger.info("   Session ID: " + sessionId);
            logger.info("   Learner: " + person.getFullName());
            logger.info("   Rating: " + rating);
            logger.info("   Tiene comentario: " + (comment != null && !comment.isEmpty()));
            logger.info("========================================");

            Optional<Feedback> existingFeedback = feedbackService.getFeedbackBySessionAndLearner(session, learner);
            Feedback feedback;

            if (existingFeedback.isPresent()) {
                feedback = existingFeedback.get();
            } else {
                feedback = new Feedback();
                feedback.setLearningSession(session);
                feedback.setLearner(learner);
            }

            if (rating != null) {
                feedback.setRating(rating);
            }
            if (comment != null) {
                feedback.setComment(comment);
            }

            feedbackService.saveFeedback(feedback);

            logger.info("[FeedbackRestController] Feedback guardado exitosamente");
            logger.info("   Feedback ID: " + feedback.getId());

            Map<String, Object> responseData = buildFeedbackResponse(feedback);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Feedback enviado exitosamente",
                    "data", responseData
            ));

        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error enviando feedback: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    //#endregion

    //#region Instructor Review Endpoints

    /**
     * Obtiene los feedbacks del instructor autenticado de forma paginada
     * GET /feedbacks/mine? page=0&size=10&sort=creationDate,desc
     * @param pageable Parametros de paginacion y ordenamiento
     * @return ResponseEntity con pagina de feedbacks
     */
    @GetMapping("/mine")
    public ResponseEntity<Page<Feedback>> getMyFeedbacks(
            @PageableDefault(size = 10, page = 0, sort = "creationDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        try {
            Page<Feedback> feedbacks = feedbackService.getMyFeedbacks(pageable);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo feedbacks: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene los feedbacks de un instructor especifico de forma paginada
     * GET /feedbacks/instructor/{instructorId}?page=0&size=10&sort=creationDate,desc
     * @param instructorId ID del instructor
     * @param pageable Parametros de paginacion y ordenamiento
     * @return ResponseEntity con pagina de feedbacks
     */
    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<Page<Feedback>> getFeedbacksByInstructor(
            @PathVariable Long instructorId,
            @PageableDefault(size = 10, page = 0, sort = "creationDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        try {
            Page<Feedback> feedbacks = feedbackService.getFeedbacksByInstructor(instructorId, pageable);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo feedbacks: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Obtiene los feedbacks recientes del instructor autenticado sin paginacion
     * GET /feedbacks/recent?limit=10
     * @param limit Cantidad maxima de feedbacks
     * @return ResponseEntity con lista de feedbacks
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Feedback>> getRecentFeedbacks(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Feedback> feedbacks = feedbackService.getRecentFeedbacks(limit);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo feedbacks recientes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene los feedbacks recientes de un instructor especifico sin paginacion
     * GET /feedbacks/instructor/{instructorId}/recent?limit=10
     * @param instructorId ID del instructor
     * @param limit Cantidad maxima de feedbacks
     * @return ResponseEntity con lista de feedbacks
     */
    @GetMapping("/instructor/{instructorId}/recent")
    public ResponseEntity<List<Feedback>> getRecentFeedbacksByInstructor(
            @PathVariable Long instructorId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Feedback> feedbacks = feedbackService.getRecentFeedbacksByInstructor(instructorId, limit);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo feedbacks recientes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Obtiene un feedback especifico por su ID
     * GET /feedbacks/detail/{feedbackId}
     * @param feedbackId ID del feedback
     * @return ResponseEntity con el feedback
     */
    @GetMapping("/detail/{feedbackId}")
    public ResponseEntity<Feedback> getFeedbackById(@PathVariable Long feedbackId) {
        try {
            Feedback feedback = feedbackService.getFeedbackById(feedbackId);
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo feedback: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Obtiene el total de feedbacks del instructor autenticado
     * GET /feedbacks/mine/count
     * @return ResponseEntity con total de feedbacks
     */
    @GetMapping("/mine/count")
    public ResponseEntity<Map<String, Long>> getMyFeedbacksCount() {
        try {
            Long count = feedbackService.getTotalFeedbackCount();
            return ResponseEntity.ok(Map.of("totalFeedbacks", count));
        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo conteo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene el total de feedbacks de un instructor especifico
     * GET /feedbacks/instructor/{instructorId}/count
     * @param instructorId ID del instructor
     * @return ResponseEntity con total de feedbacks
     */
    @GetMapping("/instructor/{instructorId}/count")
    public ResponseEntity<Map<String, Long>> getFeedbacksCountByInstructor(@PathVariable Long instructorId) {
        try {
            Long count = feedbackService.getTotalFeedbackCountByInstructor(instructorId);
            return ResponseEntity.ok(Map.of("totalFeedbacks", count));
        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo conteo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Obtiene estadisticas de feedbacks del instructor autenticado
     * GET /feedbacks/stats
     * @return ResponseEntity con estadisticas
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFeedbackStats() {
        try {
            Map<String, Object> stats = feedbackService.getFeedbackStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo estadisticas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene estadisticas de feedbacks de un instructor especifico
     * GET /feedbacks/instructor/{instructorId}/stats
     * @param instructorId ID del instructor
     * @return ResponseEntity con estadisticas
     */
    @GetMapping("/instructor/{instructorId}/stats")
    public ResponseEntity<Map<String, Object>> getFeedbackStatsByInstructor(@PathVariable Long instructorId) {
        try {
            Map<String, Object> stats = feedbackService.getFeedbackStatsByInstructor(instructorId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.info("[FeedbackRestController] Error obteniendo estadisticas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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

    private Map<String, Object> buildFeedbackResponse(Feedback feedback) {
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("id", feedback.getId());
        feedbackData.put("rating", feedback.getRating());
        feedbackData.put("comment", feedback.getComment());
        feedbackData.put("audioUrl", feedback.getAudioUrl());
        feedbackData.put("audioTranscription", feedback.getAudioTranscription());
        feedbackData.put("durationSeconds", feedback.getDurationSeconds());
        feedbackData.put("creationDate", feedback.getCreationDate());
        return feedbackData;
    }

    //#endregion
}