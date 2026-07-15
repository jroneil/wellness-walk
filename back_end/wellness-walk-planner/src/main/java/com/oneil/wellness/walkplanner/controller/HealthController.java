package com.oneil.wellness.walkplanner.controller;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oneil.wellness.walkplanner.dto.BackendStatusResponse;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;
    private final String developmentStage;

    public HealthController(
            @Value("${spring.application.name:wellness-window}") String applicationName,
            @Value("${app.development-stage:phase-1}") String developmentStage) {
        this.applicationName = applicationName;
        this.developmentStage = developmentStage;
    }

    @GetMapping("/status")
    public BackendStatusResponse status() {
        return new BackendStatusResponse(
                applicationName,
                "UP",
                Instant.now().toString(),
                developmentStage
        );
    }
}
