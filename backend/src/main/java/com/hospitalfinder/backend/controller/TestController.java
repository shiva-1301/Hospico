package com.hospitalfinder.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospitalfinder.backend.service.DataStoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/test-zoho")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final DataStoreService dataStoreService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> testCloudScaleIntegration() {
        Map<String, Object> report = new HashMap<>();
        report.put("sdk_mode", "CLOUDSCALE_NOSQL");

        // Test: Fetch users from CloudScale NoSQL
        try {
            JsonNode result = dataStoreService.executeQuery("SELECT * FROM users");
            report.put("users_fetch_status", "SUCCESS");
            report.put("users_count", result != null ? result.size() : 0);
            report.put("users_sample", result);
        } catch (Exception e) {
            report.put("users_fetch_status", "FAILED");
            report.put("users_fetch_error", e.getMessage());
        }

        // Test: Fetch clinics from CloudScale NoSQL
        try {
            JsonNode result = dataStoreService.executeQuery("SELECT * FROM clinics");
            report.put("clinics_fetch_status", "SUCCESS");
            report.put("clinics_count", result != null ? result.size() : 0);
        } catch (Exception e) {
            report.put("clinics_fetch_status", "FAILED");
            report.put("clinics_fetch_error", e.getMessage());
        }

        return ResponseEntity.ok(report);
    }
}
