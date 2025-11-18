package com.project.skillswap.rest.LearningSession;

import com.project.skillswap.logic.entity.LearningSession.InstructorSessionService;
import com.project.skillswap.logic.entity.LearningSession.SessionListResponse;
import com.project.skillswap.logic.entity.LearningSession.SessionUpdateRequest;
import com.project.skillswap.logic.entity.LearningSession.SessionUpdateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instructor/sessions")
@CrossOrigin(origins = "http://localhost:4200")
public class InstructorSessionController {

    @Autowired
    private InstructorSessionService sessionService;

    @GetMapping
    public ResponseEntity<Page<SessionListResponse>> getInstructorSessions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Authentication authentication) {

        try {
            String userEmail = authentication.getName();

            Page<SessionListResponse> sessions = sessionService.getInstructorSessions(
                    userEmail,
                    status,
                    page,
                    size
            );

            return ResponseEntity.ok(sessions);

        } catch (Exception e) {
            System.err.println("❌ [ERROR] Error al listar sesiones: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionUpdateResponse> updateSession(
            @PathVariable Long id,
            @RequestBody SessionUpdateRequest request,
            Authentication authentication) {

        try {
            String userEmail = authentication.getName();

            SessionUpdateResponse updatedSession = sessionService.updateSession(
                    id,
                    request,
                    userEmail
            );

            return ResponseEntity.ok(updatedSession);

        } catch (RuntimeException e) {
            System.err.println("❌ [ERROR] Error al actualizar sesión: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}