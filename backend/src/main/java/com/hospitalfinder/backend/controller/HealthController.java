package com.hospitalfinder.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<String> rootHealth() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "message", "Service is running"));
    }
}
