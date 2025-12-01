
package com.project.skillswap.logic.exceptions;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de excepciones para la aplicación.
 * Centraliza el manejo de errores y proporciona respuestas consistentes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja todas las excepciones de seguridad y generales de la aplicación.
     *
     * @param exception la excepción lanzada
     * @return ProblemDetail con los detalles del error
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleSecurityException(Exception exception) {
        ProblemDetail errorDetail = null;

        exception.printStackTrace();

        if (exception instanceof BadCredentialsException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(401), exception.getMessage());
            errorDetail.setProperty("description", "The username or password is incorrect");
            return errorDetail;
        }

        if (exception instanceof DisabledException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), exception.getMessage());

            if (exception.getMessage().contains("verifica tu correo") ||
                    exception.getMessage().contains("verify your email")) {
                errorDetail.setProperty("description", "Email verification required");
                errorDetail.setProperty("code", "email_verification_required");
                errorDetail.setProperty("status", "EMAIL_NOT_VERIFIED");
            } else {
                errorDetail.setProperty("description", "The account is disabled");
                errorDetail.setProperty("code", "account_disabled");
                errorDetail.setProperty("status", "ACCOUNT_DISABLED");
            }
            return errorDetail;
        }

        if (exception instanceof AccountStatusException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), exception.getMessage());
            errorDetail.setProperty("description", "The account is locked");
        }

        if (exception instanceof AccessDeniedException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), exception.getMessage());
            errorDetail.setProperty("description", "You are not authorized to access this resource");
        }

        if (exception instanceof SignatureException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), exception.getMessage());
            errorDetail.setProperty("description", "The JWT signature is invalid");
        }

        if (exception instanceof ExpiredJwtException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), exception.getMessage());
            errorDetail.setProperty("description", "The JWT token has expired");
        }

        if (exception instanceof IllegalArgumentException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), exception.getMessage());
            errorDetail.setProperty("description", "Invalid argument: " + exception.getMessage());
            return errorDetail;
        }

        if (exception instanceof RuntimeException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), exception.getMessage());
            errorDetail.setProperty("description", "Registration failed: " + exception.getMessage());
            return errorDetail;
        }

        if (errorDetail == null) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500), exception.getMessage());
            errorDetail.setProperty("description", "Unknown internal server error.");
        }

        return errorDetail;
    }
}