package com.project.skillswap.logic.entity.passreset;

import com.project.skillswap.logic.entity.passreset.PasswordResetService.TooManyResetRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/request")
    public ResponseEntity<String> requestReset(
            @RequestParam("email") String email,
            HttpServletRequest request
    ) {
        try {
            passwordResetService.requestReset(
                    email,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );
            return ResponseEntity.ok("Si el correo se encuentra registrado se enviará el correo de recuperación");
        } catch (TooManyResetRequestsException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Se ha excedido el límite de solicitudes. Inténtalo más tarde.");
        }
    }
}
