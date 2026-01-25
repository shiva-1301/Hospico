package com.hospitalfinder.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospitalfinder.backend.service.ZohoAuthService;
import com.hospitalfinder.backend.service.ZohoDataStoreService;
import com.hospitalfinder.backend.config.ZohoConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/test-zoho")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final ZohoDataStoreService dataStoreService;
    private final ZohoAuthService authService;
    private final ZohoConfig zohoConfig;

    @Value("${zoho.project.id}")
    private String projectId;

    @Value("${zoho.users.table.name:users}")
    private String usersTableName;

    @Value("${zoho.users.table.id}")
    private String usersTableId;

    @GetMapping
    public ResponseEntity<Map<String, Object>> testZohoIntegration(
            @RequestParam(required = false, defaultValue = "SELECT * FROM users") String query) {

        Map<String, Object> report = new HashMap<>();
        report.put("config_project_id", projectId);
        report.put("config_users_table_name", usersTableName);
        report.put("config_users_table_id", usersTableId);
        report.put("sdk_mode", "FALSE");

        // 1. Test Auth
        try {
            String token = authService.getAccessToken();
            report.put("auth_token_status", "SUCCESS");
            report.put("auth_token_preview", token.substring(0, Math.min(token.length(), 10)) + "...");
            report.put("is_using_pat", authService.isUsingPat());
        } catch (Exception e) {
            report.put("auth_token_status", "FAILED");
            report.put("auth_token_error", e.getMessage());
            return ResponseEntity.ok(report);
        }

        // 2. Test Row Fetch (replaces ZCQL)
        try {
            // Default query string "SELECT * FROM users" is not compatible with
            // queryRecords criteria
            // We'll ignore the query param for now and just fetch all (empty criteria) or
            // limit
            String criteria = "";
            if (query != null && !query.startsWith("SELECT")) {
                criteria = query; // allow passing raw criteria like "email==..."
            }

            JsonNode result = dataStoreService.queryRecords(usersTableId, criteria);
            report.put("fetch_status", "SUCCESS");
            report.put("fetch_result_count", result != null ? result.size() : 0);
            report.put("fetch_sample", result);
        } catch (Exception e) {
            report.put("fetch_status", "FAILED");
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " Cause: " + e.getCause().getMessage();
            }
            report.put("fetch_error", errorMessage);
        }

        return ResponseEntity.ok(report);
    }
}
