package com.project.skillswap.rest.notification;

import com.project.skillswap.logic.entity.Notification.CredentialAlertService;
import com.project.skillswap.logic.entity.Notification.SessionAlertService;
import com.project.skillswap.logic.entity.Notification.CredentialAlertDTO;
import com.project.skillswap.logic.entity.Notification.UserSessionAlertDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para testing de alertas
 */
@RestController
@RequestMapping("/api/alerts/test")
@CrossOrigin
public class AlertTestController {

    private final CredentialAlertService credentialAlertService;
    private final SessionAlertService sessionAlertService;

    public AlertTestController(
            CredentialAlertService credentialAlertService,
            SessionAlertService sessionAlertService) {
        this.credentialAlertService = credentialAlertService;
        this.sessionAlertService = sessionAlertService;
    }

    /**
     * Ver quiénes recibirían alertas de credenciales (SIN ENVIAR)
     */
    @GetMapping("/credentials/preview")
    public ResponseEntity<Map<String, Object>> previewCredentialAlerts() {
        List<CredentialAlertDTO> alerts = credentialAlertService.getAlertsPreview();

        Map<String, Object> response = new HashMap<>();
        response.put("totalAlerts", alerts.size());
        response.put("alerts", alerts);
        response.put("message", "Estas son las alertas que se enviarían");

        return ResponseEntity.ok(response);
    }

    /**
     * Ver quiénes recibirían alertas de sesiones (SIN ENVIAR)
     */
    @GetMapping("/sessions/preview")
    public ResponseEntity<Map<String, Object>> previewSessionAlerts() {
        Map<Long, UserSessionAlertDTO> userAlerts = sessionAlertService.getAlertsPreview();

        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", userAlerts.size());
        response.put("userAlerts", userAlerts.values());
        response.put("message", "Estas son las alertas que se enviarían");

        return ResponseEntity.ok(response);
    }

    /**
     * ENVIAR alertas de credenciales AHORA (para testing)
     */
    @PostMapping("/credentials/send")
    public ResponseEntity<Map<String, String>> sendCredentialAlertsNow() {
        try {
            credentialAlertService.processAndSendCredentialAlerts();

            Map<String, String> response = new HashMap<>();
            response.put("message", "Alertas de credenciales enviadas exitosamente");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error al enviar alertas: " + e.getMessage());
            response.put("status", "error");

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ENVIAR alertas de sesiones (modo testing - sin validar duplicados)
     */
    @PostMapping("/sessions/send-testing")
    public ResponseEntity<Map<String, String>> sendSessionAlertsTestingMode() {
        try {
            sessionAlertService.processAndSendSessionAlerts();

            Map<String, String> response = new HashMap<>();
            response.put("message", "Alertas de sesiones enviadas en modo testing (sin validar duplicados)");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error al enviar alertas: " + e.getMessage());
            response.put("status", "error");

            return ResponseEntity.status(500).body(response);
        }
    }
}