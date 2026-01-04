package com.hospitalfinder.backend.controller;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class StartupHealthController {

    private final ApplicationAvailability applicationAvailability;

    public StartupHealthController(ApplicationAvailability applicationAvailability) {
        this.applicationAvailability = applicationAvailability;
    }

    // Liveness probe - answers "is the app alive?" even during startup
    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        LivenessState livenessState = applicationAvailability.getLivenessState();
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("livenessState", livenessState.toString());
        
        return ResponseEntity.ok(response);
    }

    // Readiness probe - indicates when app is ready to serve traffic
    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, String>> readiness() {
        ReadinessState readinessState = applicationAvailability.getReadinessState();
        Map<String, String> response = new HashMap<>();
        
        if (readinessState == ReadinessState.ACCEPTING_TRAFFIC) {
            response.put("status", "UP");
            response.put("readinessState", readinessState.toString());
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "OUT_OF_SERVICE");
            response.put("readinessState", readinessState.toString());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
