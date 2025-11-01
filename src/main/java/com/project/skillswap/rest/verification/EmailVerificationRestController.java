package com.project.skillswap.rest.verification;

import com.project.skillswap.logic.entity.verification.VerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestionar la verificación de correo electrónico.
 */
@RestController
@RequestMapping("/verification")
@CrossOrigin(origins = "*")
public class EmailVerificationRestController {

    //#region Dependencies
    private final VerificationService verificationService;

    public EmailVerificationRestController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }
    //#endregion

    //#region Endpoints
    /**
     * Verifica un token de correo electrónico.
     *
     * @param token el token a verificar
     * @return respuesta con el resultado de la verificación
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("El token es requerido"));
        }

        VerificationService.VerificationResult result = verificationService.verifyToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        response.put("status", result.getStatus().name());

        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            HttpStatus httpStatus = determineHttpStatus(result.getStatus());
            return ResponseEntity.status(httpStatus).body(response);
        }
    }

    /**
     * Reenvía un correo de verificación a un usuario.
     *
     * @param request mapa con el email del usuario
     * @return respuesta con el resultado del reenvío
     */
    @PostMapping("/resend")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("El correo electrónico es requerido"));
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("El formato del correo electrónico no es válido"));
        }

        VerificationService.ResendResult result = verificationService.resendVerificationToken(email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        
        if (result.getEmail() != null) {
            response.put("email", result.getEmail());
        }

        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    //#endregion

    //#region Private Methods
    /**
     * Crea un mapa de respuesta de error.
     *
     * @param message mensaje de error
     * @return mapa con el error
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    /**
     * Determina el código HTTP apropiado según el estado de verificación.
     *
     * @param status estado de verificación
     * @return código HTTP correspondiente
     */
    private HttpStatus determineHttpStatus(VerificationService.VerificationStatus status) {
        return switch (status) {
            case INVALID_TOKEN -> HttpStatus.NOT_FOUND;
            case EXPIRED_TOKEN -> HttpStatus.GONE;
            case TOKEN_USED, ALREADY_VERIFIED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
    //#endregion
}
