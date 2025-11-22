package com.project.skillswap.logic.entity.PassReset;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/password/reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // Paso 1: solicitar envío del código
    @PostMapping("/request")
    public ResponseEntity<?> requestReset(
            @RequestParam String email,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        try {
            passwordResetService.requestReset(email, forwardedFor, userAgent);
            return ResponseEntity.ok("Si el correo existe, se ha enviado un código de verificación.");
        } catch (PasswordResetService.CooldownException e) {
            return ResponseEntity.status(429).body("Debes esperar antes de solicitar otro código.");
        } catch (PasswordResetService.TooManyResetRequestsException e) {
            return ResponseEntity.status(429).body("Has superado el límite de solicitudes permitidas.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al procesar la solicitud.");
        }
    }

    // Paso 2: verificar código
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(
            @RequestParam String email,
            @RequestParam String code
    ) {
        try {
            passwordResetService.verifyCode(email, code);
            return ResponseEntity.ok("Código verificado correctamente.");
        } catch (PasswordResetService.NotFoundOrExpiredException e) {
            return ResponseEntity.badRequest().body("El código no existe o ha expirado.");
        } catch (PasswordResetService.InvalidCodeException e) {
            return ResponseEntity.badRequest().body("El código ingresado no es válido.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al verificar el código.");
        }
    }

    // Paso 3: confirmar nueva contraseña
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmReset(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam String newPassword
    ) {
        try {
            passwordResetService.confirmReset(email, code, newPassword);
            return ResponseEntity.ok("Contraseña actualizada correctamente.");
        } catch (PasswordResetService.NotFoundOrExpiredException e) {
            return ResponseEntity.badRequest().body("Código inválido o expirado.");
        } catch (PasswordResetService.InvalidCodeException e) {
            return ResponseEntity.badRequest().body("El código ingresado es incorrecto.");
        } catch (PasswordResetService.WeakPasswordException e) {
            return ResponseEntity.badRequest().body("La nueva contraseña no cumple con los requisitos de seguridad.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al actualizar la contraseña.");
        }
    }
}
