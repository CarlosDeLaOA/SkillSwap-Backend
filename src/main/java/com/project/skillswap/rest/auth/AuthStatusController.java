package com.project.skillswap.rest.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class AuthStatusController {

    @GetMapping("/auth/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "ok",
                        "time", Instant.now().toString()
                )
        );
    }
}
