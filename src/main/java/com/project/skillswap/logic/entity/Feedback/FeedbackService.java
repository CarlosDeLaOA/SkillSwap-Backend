package com.project.skillswap.logic.entity.Feedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio de logica de negocio para operaciones relacionadas con Feedback/Resenas
 * Gestiona la obtencion, paginacion y estadisticas de feedbacks de instructores
 */
@Service
@Transactional(readOnly = true)
public class FeedbackService {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackService.class);

    //#region Dependencias
    private final FeedbackRepository feedbackRepository;
    private final PersonRepository personRepository;
    private final InstructorRepository instructorRepository;
    //#endregion

    //#region Constructor
    public FeedbackService(
            FeedbackRepository feedbackRepository,
            PersonRepository personRepository,
            InstructorRepository instructorRepository) {
        this.feedbackRepository = feedbackRepository;
        this.personRepository = personRepository;
        this.instructorRepository = instructorRepository;
    }
    //#endregion

    //#region Metodos publicos

    /**
     * Obtiene los feedbacks del instructor autenticado de forma paginada
     * @param pageable Informacion de paginacion y ordenamiento
     * @return Pagina con feedbacks del instructor autenticado
     * @throws FeedbackException Si el usuario no es instructor o no se encuentra
     */
    public Page<Feedback> getMyFeedbacks(Pageable pageable) {
        Long instructorId = getAuthenticatedInstructorId();
        return feedbackRepository.findFeedbacksByInstructorId(instructorId, pageable);
    }

    /**
     * Obtiene los feedbacks de un instructor especifico de forma paginada
     * @param instructorId ID del instructor
     * @param pageable Informacion de paginacion y ordenamiento
     * @return Pagina con feedbacks del instructor especificado
     * @throws FeedbackException Si el instructor no existe
     */
    public Page<Feedback> getFeedbacksByInstructor(Long instructorId, Pageable pageable) {
        validateInstructorExists(instructorId);
        return feedbackRepository.findFeedbacksByInstructorId(instructorId, pageable);
    }

    /**
     * Obtiene los feedbacks recientes de un instructor sin paginacion
     * Util para dashboards y vistas rapidas
     * @param limit Cantidad maxima de feedbacks a retornar
     * @return Lista de feedbacks recientes del instructor autenticado
     * @throws FeedbackException Si el usuario no es instructor o no se encuentra
     */
    public List<Feedback> getRecentFeedbacks(int limit) {
        Long instructorId = getAuthenticatedInstructorId();
        return feedbackRepository.findRecentFeedbacksByInstructorId(instructorId, limit);
    }

    /**
     * Obtiene los feedbacks recientes de un instructor especifico sin paginacion
     * @param instructorId ID del instructor
     * @param limit Cantidad maxima de feedbacks a retornar
     * @return Lista de feedbacks recientes del instructor especificado
     * @throws FeedbackException Si el instructor no existe
     */
    public List<Feedback> getRecentFeedbacksByInstructor(Long instructorId, int limit) {
        validateInstructorExists(instructorId);
        return feedbackRepository.findRecentFeedbacksByInstructorId(instructorId, limit);
    }

    /**
     * Obtiene un feedback especifico por su ID
     * @param feedbackId ID del feedback
     * @return El feedback solicitado
     * @throws FeedbackException Si el feedback no existe
     */
    public Feedback getFeedbackById(Long feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> FeedbackException.feedbackNotFound(feedbackId));
    }

    /**
     * Obtiene el total de feedbacks del instructor autenticado
     * @return Cantidad total de feedbacks
     * @throws FeedbackException Si el usuario no es instructor o no se encuentra
     */
    public Long getTotalFeedbackCount() {
        Long instructorId = getAuthenticatedInstructorId();
        return feedbackRepository.countFeedbacksByInstructorId(instructorId);
    }

    /**
     * Obtiene el total de feedbacks de un instructor especifico
     * @param instructorId ID del instructor
     * @return Cantidad total de feedbacks
     * @throws FeedbackException Si el instructor no existe
     */
    public Long getTotalFeedbackCountByInstructor(Long instructorId) {
        validateInstructorExists(instructorId);
        return feedbackRepository.countFeedbacksByInstructorId(instructorId);
    }

    /**
     * Obtiene estadisticas completas de feedbacks del instructor autenticado
     * Incluye: promedio, conteos por rating, total
     * @return Mapa con estadisticas de feedbacks
     * @throws FeedbackException Si el usuario no es instructor o no se encuentra
     */
    public Map<String, Object> getFeedbackStats() {
        Long instructorId = getAuthenticatedInstructorId();
        return calculateFeedbackStats(instructorId);
    }

    /**
     * Obtiene estadisticas completas de feedbacks de un instructor especifico
     * @param instructorId ID del instructor
     * @return Mapa con estadisticas de feedbacks
     * @throws FeedbackException Si el instructor no existe
     */
    public Map<String, Object> getFeedbackStatsByInstructor(Long instructorId) {
        validateInstructorExists(instructorId);
        return calculateFeedbackStats(instructorId);
    }

    /**
     * Obtiene el feedback de una sesion y learner especificos
     * @param learningSession Sesion de aprendizaje
     * @param learner Learner que dejo el feedback
     * @return Optional con el feedback si existe
     */
    public Optional<Feedback> getFeedbackBySessionAndLearner(LearningSession learningSession, Learner learner) {
        return feedbackRepository.findByLearningSessionAndLearner(learningSession, learner);
    }

    /**
     * Guarda un feedback en la base de datos
     * @param feedback Feedback a guardar
     * @return Feedback guardado
     */
    @Transactional
    public Feedback saveFeedback(Feedback feedback) {
        logger.info("[FeedbackService] Guardando feedback ID: " + feedback.getId());
        return feedbackRepository.save(feedback);
    }

    //#endregion

    //#region Metodos privados

    /**
     * Obtiene el ID del instructor del usuario autenticado en el contexto de seguridad
     * @return ID del instructor autenticado
     * @throws FeedbackException Si el usuario no es instructor o no se encuentra
     */
    private Long getAuthenticatedInstructorId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            logger.info("[FeedbackService] Authentication object: " + authentication);
            logger.info("[FeedbackService] Is Authenticated: " + (authentication != null ?   authentication.isAuthenticated() : "NULL"));

            if (authentication == null || !authentication.isAuthenticated()) {
                logger.info("[FeedbackService] Authentication is null or not authenticated");
                throw FeedbackException.userNotInstructor();
            }

            String email = authentication.getName();
            logger.info("[FeedbackService] Email from authentication: " + email);

            Person person = personRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.info("[FeedbackService] Person not found for email: " + email);
                        return FeedbackException.userNotInstructor();
                    });

            logger.info("[FeedbackService] Person found: " + person.getId() + " (" + person.getFullName() + ")");

            Instructor instructor = person.getInstructor();
            logger.info("[FeedbackService] Instructor object: " + instructor);

            if (instructor == null) {
                logger.info("[FeedbackService] User has no instructor role");
                throw FeedbackException.userNotInstructor();
            }

            logger.info("[FeedbackService] Instructor ID: " + instructor.getId());
            return instructor.getId();

        } catch (FeedbackException e) {
            logger.info("[FeedbackService] FeedbackException: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.info("[FeedbackService] Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw FeedbackException.processingError("Error al obtener instructor autenticado: " + e.getMessage());
        }
    }

    /**
     * Valida que un instructor exista en la base de datos
     * @param instructorId ID del instructor a validar
     * @throws FeedbackException Si el instructor no existe
     */
    private void validateInstructorExists(Long instructorId) {
        logger.info("[FeedbackService] Validating instructor ID: " + instructorId);
        boolean exists = instructorRepository.existsById(instructorId);
        if (!exists) {
            logger.info("[FeedbackService] Instructor not found: " + instructorId);
            throw FeedbackException.instructorNotFound(instructorId);
        }
        logger.info("[FeedbackService] Instructor exists: " + instructorId);
    }

    /**
     * Calcula estadisticas completas de feedbacks para un instructor
     * @param instructorId ID del instructor
     * @return Mapa con estadisticas: promedio, totales por rating, etc.
     */
    private Map<String, Object> calculateFeedbackStats(Long instructorId) {
        try {
            Map<String, Object> stats = new HashMap<>();

            Long totalFeedbacks = feedbackRepository.countFeedbacksByInstructorId(instructorId);
            Double averageRating = feedbackRepository.getAverageRatingByInstructorId(instructorId);

            stats.put("totalReviews", totalFeedbacks != null ? totalFeedbacks : 0L);
            stats.put("averageRating", averageRating != null ? Math.round(averageRating * 100.0) / 100.0 : 0.0);

            stats.put("fiveStarCount", feedbackRepository.countFeedbacksByRatingAndInstructor(instructorId, 5));
            stats.put("fourStarCount", feedbackRepository.countFeedbacksByRatingAndInstructor(instructorId, 4));
            stats.put("threeStarCount", feedbackRepository.countFeedbacksByRatingAndInstructor(instructorId, 3));
            stats.put("twoStarCount", feedbackRepository.countFeedbacksByRatingAndInstructor(instructorId, 2));
            stats.put("oneStarCount", feedbackRepository.countFeedbacksByRatingAndInstructor(instructorId, 1));

            return stats;
        } catch (Exception e) {
            logger.info("[FeedbackService] Error calculating stats: " + e.getMessage());
            throw FeedbackException.statisticsError(instructorId);
        }
    }

    //#endregion
}