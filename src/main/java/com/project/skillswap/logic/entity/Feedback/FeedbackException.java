package com.project.skillswap.logic.entity.Feedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Excepción personalizada para operaciones relacionadas con Feedback/Reseñas
 * Se lanza cuando ocurren errores específicos en el dominio de feedback
 */
public class FeedbackException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackException.class);

    //#region Constructores

    /**
     * Constructor básico con mensaje
     * @param message Mensaje de error descriptivo
     */
    public FeedbackException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa
     * @param message Mensaje de error descriptivo
     * @param cause Excepción que causó el error
     */
    public FeedbackException(String message, Throwable cause) {
        super(message, cause);
    }

    //#endregion

    //#region Métodos estáticos para crear excepciones comunes

    /**
     * Excepción cuando un feedback no es encontrado
     * @param feedbackId ID del feedback no encontrado
     * @return FeedbackException con mensaje descriptivo
     */
    public static FeedbackException feedbackNotFound(Long feedbackId) {
        return new FeedbackException("Feedback con ID " + feedbackId + " no fue encontrado");
    }

    /**
     * Excepción cuando un instructor no es encontrado
     * @param instructorId ID del instructor no encontrado
     * @return FeedbackException con mensaje descriptivo
     */
    public static FeedbackException instructorNotFound(Long instructorId) {
        return new FeedbackException("Instructor con ID " + instructorId + " no fue encontrado");
    }

    /**
     * Excepción cuando el usuario autenticado no es instructor
     * @return FeedbackException con mensaje descriptivo
     */
    public static FeedbackException userNotInstructor() {
        return new FeedbackException("El usuario autenticado no es instructor");
    }

    /**
     * Excepción cuando no hay feedbacks disponibles
     * @param instructorId ID del instructor sin feedbacks
     * @return FeedbackException con mensaje descriptivo
     */
    public static FeedbackException noFeedbacksAvailable(Long instructorId) {
        return new FeedbackException("No hay feedbacks disponibles para el instructor con ID " + instructorId);
    }

    /**
     * Excepción cuando ocurre un error al procesar feedbacks
     * @param message Descripción del error
     * @return FeedbackException con mensaje descriptivo
     */
    public static FeedbackException processingError(String message) {
        return new FeedbackException("Error al procesar feedbacks: " + message);
    }

    /**
     * Excepción cuando ocurre un error al calcular estadísticas
     * @param instructorId ID del instructor
     * @return FeedbackException con mensaje descriptivo
     */
    public static FeedbackException statisticsError(Long instructorId) {
        return new FeedbackException("Error al calcular estadísticas de feedbacks para el instructor con ID " + instructorId);
    }

    //#endregion
}