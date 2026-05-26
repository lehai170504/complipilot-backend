package com.complipilot.backend.health;

import java.time.Instant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

record HealthResponse(
        String status,
        String service,
        Instant timestamp
) {
}

@Tag(name = "Health", description = "Health check APIs")
@RestController
public class HealthController {

    @Operation(
            summary = "Backend health check",
            description = "Returns service status and current timestamp."
    )
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