package com.hospitalfinder.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospitalfinder.backend.config.ZohoConfig;
import com.hospitalfinder.backend.service.ZohoAuthService;
import com.hospitalfinder.backend.service.ZohoDataStoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/test-zoho")
@ConditionalOnProperty(name = "data.store.provider", havingValue = "zoho")
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

        // 3. Test ZCQL (Alternative Access)
        try {
            JsonNode zcqlResult = dataStoreService.executeZCQL("SELECT * FROM " + usersTableName + " LIMIT 1");
            report.put("zcql_status", "SUCCESS");
            report.put("zcql_sample", zcqlResult);
        } catch (Exception e) {
            report.put("zcql_status", "FAILED");
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " Cause: " + e.getCause().getMessage();
            }
            report.put("zcql_error", errorMessage);
        }

        // 4. Test Environment Fetch
        try {
            // Basic RestTemplate to fetch envs
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders h = new org.springframework.http.HttpHeaders();
            h.set("Authorization", "Zoho-oauthtoken " + authService.getAccessToken());
            org.springframework.http.HttpEntity<String> req = new org.springframework.http.HttpEntity<>(h);

            String envUrl = zohoConfig.getBaseUrl() + "/project/" + projectId + "/environment";
            ResponseEntity<String> resp = rt.exchange(envUrl, org.springframework.http.HttpMethod.GET, req,
                    String.class);

            JsonNode envData = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.getBody());
            report.put("environments_raw", envData);
            report.put("environments_status", "SUCCESS");
        } catch (Exception e) {
            report.put("environments_status", "FAILED");
            report.put("environments_error", e.getMessage());
        }

        // 5. Test Project Reachability (Authorization Check)
        try {
            // Basic RestTemplate to fetch project details (SELF Check)
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders h = new org.springframework.http.HttpHeaders();
            h.set("Authorization", "Zoho-oauthtoken " + authService.getAccessToken());
            org.springframework.http.HttpEntity<String> req = new org.springframework.http.HttpEntity<>(h);

            // Try to just get the project details specifically
            String projUrl = zohoConfig.getBaseUrl() + "/project/" + projectId;
            ResponseEntity<String> resp = rt.exchange(projUrl, org.springframework.http.HttpMethod.GET, req,
                    String.class);

            JsonNode projData = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.getBody());
            report.put("project_details_status", "SUCCESS");
            report.put("project_details", projData);
        } catch (Exception e) {
            report.put("project_details_status", "FAILED");
            report.put("project_details_error", e.getMessage());
            // Capture headers if possible in real debug, but here just message
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                report.put("project_details_response",
                        ((org.springframework.web.client.HttpClientErrorException) e).getResponseBodyAsString());
            }
        }

        return ResponseEntity.ok(report);
    }
}
