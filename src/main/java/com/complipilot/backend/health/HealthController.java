package com.complipilot.backend.health;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

record HealthResponse(
        String status,
        String service,
        Instant timestamp
) {
}

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(
                new HealthResponse(
                        "UP",
                        "complipilot-backend",
                        Instant.now()
                )
        );
    }
}