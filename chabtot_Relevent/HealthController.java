package com.hospitalfinder.backend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.config.RequestLoggingInterceptor;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> rootHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "Hospico Hospital Booking API");
        response.put("status", "UP");
        response.put("version", "1.0");
        
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("Health Check", "/api/health");
        endpoints.put("Authentication", "/api/auth/login, /api/auth/signup");
        endpoints.put("Clinics", "/api/clinics");
        endpoints.put("Doctors", "/api/doctors");
        endpoints.put("Appointments", "/api/appointments");
        endpoints.put("Specializations", "/api/specializations");
        endpoints.put("Reviews", "/api/reviews");
        endpoints.put("Medical Records", "/api/medical-records");
        endpoints.put("Chat", "/api/chat");
        endpoints.put("Recent Requests", "/api/requests/recent");
        endpoints.put("API Documentation", "/swagger-ui.html");
        
        response.put("endpoints", endpoints);
        response.put("recentRequests", RequestLoggingInterceptor.getRecentRequests());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/requests/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentRequests() {
        return ResponseEntity.ok(RequestLoggingInterceptor.getRecentRequests());
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "message", "Service is running"));
    }
}
