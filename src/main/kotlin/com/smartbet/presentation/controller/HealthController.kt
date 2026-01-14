package com.smartbet.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Health check endpoints")
class HealthController {
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se a aplicação está funcionando")
    fun health(): ResponseEntity<HealthResponse> {
        return ResponseEntity.ok(
            HealthResponse(
                status = "UP",
                timestamp = Instant.now(),
                version = "1.0.0"
            )
        )
    }
}

data class HealthResponse(
    val status: String,
    val timestamp: Instant,
    val version: String
)
