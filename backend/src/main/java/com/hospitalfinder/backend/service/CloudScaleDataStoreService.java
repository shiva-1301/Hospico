package com.hospitalfinder.backend.service;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hospitalfinder.backend.config.CloudScaleConfig;

import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements DataStoreService using Zoho Catalyst Data Store REST API.
 * No Java SDK dependency — pure HTTP calls with RestTemplate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudScaleDataStoreService implements DataStoreService {

    private final CloudScaleConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${zoho.environment:Development}")
    private String environment;

    // ── CRUD Operations ──────────────────────────────────────────

    @Override
    public JsonNode insertRecord(String tableName, Map<String, Object> data) {
        String url = config.getBaseUrl() + "/table/" + tableName + "/row";
        try {
            // Zoho expects a JSON array of row objects: [{...}]
            String jsonBody = objectMapper.writeValueAsString(java.util.List.of(data));
            log.debug("INSERT into '{}' url={} body={}", tableName, url, jsonBody);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, authHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode body = parseResponse(response);
            // Response is an array; return the first inserted row
            if (body != null && body.isArray() && body.size() > 0) {
                return body.get(0);
            }
            return body;
        } catch (Exception e) {
            log.error("Failed to insert record into '{}': {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to insert record into " + tableName, e);
        }
    }

    @Override
    public JsonNode updateRecord(String tableName, Long rowId, Map<String, Object> data) {
        String url = config.getBaseUrl() + "/table/" + tableName + "/row/" + rowId;
        try {
            String jsonBody = objectMapper.writeValueAsString(data);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            JsonNode body = parseResponse(response);
            log.debug("Updated record in '{}', rowId={}", tableName, rowId);
            return body;
        } catch (Exception e) {
            log.error("Failed to update record in '{}', rowId={}", tableName, rowId, e);
            throw new RuntimeException("Failed to update record in " + tableName, e);
        }
    }

    @Override
    public void deleteRecord(String tableName, Long rowId) {
        String url = config.getBaseUrl() + "/table/" + tableName + "/row/" + rowId;
        try {
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
            log.debug("Deleted record from '{}', rowId={}", tableName, rowId);
        } catch (Exception e) {
            log.error("Failed to delete record from '{}', rowId={}", tableName, rowId, e);
            throw new RuntimeException("Failed to delete record from " + tableName, e);
        }
    }

    @Override
    public JsonNode findById(String tableName, Long rowId) {
        String url = config.getBaseUrl() + "/table/" + tableName + "/row/" + rowId;
        try {
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Failed to find record in '{}', rowId={}", tableName, rowId, e);
            return null;
        }
    }

    @Override
    public JsonNode findByField(String tableName, String fieldName, String fieldValue) {
        // Use ZCQL for field-based lookup
        String zcql = "SELECT * FROM " + tableName + " WHERE " + fieldName + " = '" + escapeZcql(fieldValue) + "'";
        try {
            JsonNode results = executeZcql(zcql);
            if (results != null && results.isArray() && results.size() > 0) {
                JsonNode first = results.get(0);
                // ZCQL wraps rows like { "tableName": { ...fields... } }
                return first.has(tableName) ? first.get(tableName) : first;
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to find record in '{}' where {}={}", tableName, fieldName, fieldValue, e);
            return null;
        }
    }

    // ── Query Execution ──────────────────────────────────────────

    @Override
    public JsonNode executeQuery(String query) {
        try {
            log.debug("Executing ZCQL: {}", query);
            String upperQuery = query.trim().toUpperCase();

            if (upperQuery.startsWith("SELECT")) {
                JsonNode results = executeZcql(query);
                return results != null ? results : objectMapper.createArrayNode();
            } else if (upperQuery.startsWith("DELETE")) {
                return handleDeleteQuery(query);
            } else {
                throw new RuntimeException("Unsupported query: " + query);
            }
        } catch (Exception e) {
            log.error("Failed to execute query: {}", query, e);
            throw new RuntimeException("Failed to execute query: " + query, e);
        }
    }

    // ── Internal Helpers ─────────────────────────────────────────

    /**
     * Execute a ZCQL query via the Zoho Data Store REST API.
     */
    private JsonNode executeZcql(String query) {
        String url = config.getBaseUrl() + "/query";
        try {
            String jsonBody = objectMapper.writeValueAsString(Collections.singletonMap("query", query));
            log.debug("ZCQL url={} body={}", url, jsonBody);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, authHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("ZCQL query failed: {} - {}", query, e.getMessage());
            throw new RuntimeException("ZCQL query failed: " + query, e);
        }
    }

    /**
     * Handle DELETE queries by first finding matching rows via ZCQL SELECT,
     * then deleting each row by ROWID.
     */
    private JsonNode handleDeleteQuery(String query) {
        // Convert "DELETE FROM table WHERE ..." to "SELECT * FROM table WHERE ..."
        String selectQuery = query.replaceFirst("(?i)DELETE\\s+FROM", "SELECT * FROM");
        String tableName = extractTableName(query);

        JsonNode rows = executeZcql(selectQuery);
        int deleted = 0;

        if (rows != null && rows.isArray()) {
            for (JsonNode row : rows) {
                JsonNode data = row.has(tableName) ? row.get(tableName) : row;
                Long rowId = extractRowId(data);
                if (rowId != null) {
                    deleteRecord(tableName, rowId);
                    deleted++;
                }
            }
        }

        log.debug("Deleted {} rows from '{}'", deleted, tableName);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("deleted", deleted);
        return result;
    }

    private String extractTableName(String query) {
        String upper = query.toUpperCase();
        int fromIdx = upper.indexOf("FROM");
        if (fromIdx == -1)
            throw new RuntimeException("Cannot parse table name from: " + query);
        String afterFrom = query.substring(fromIdx + 4).trim();
        int endIdx = afterFrom.indexOf(' ');
        return endIdx == -1 ? afterFrom.trim() : afterFrom.substring(0, endIdx).trim();
    }

    private Long extractRowId(JsonNode data) {
        if (data.has("ROWID") && !data.get("ROWID").isNull()) {
            return data.get("ROWID").asLong();
        }
        if (data.has("_id") && !data.get("_id").isNull()) {
            return data.get("_id").asLong();
        }
        return null;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Zoho-oauthtoken " + config.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Environment", environment);
        return headers;
    }

    private JsonNode parseResponse(ResponseEntity<String> response) {
        try {
            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                // Zoho wraps results in a "data" field for some endpoints
                if (root.has("data")) {
                    return root.get("data");
                }
                return root;
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to parse response body: {}", response.getBody(), e);
            return null;
        }
    }

    private String escapeZcql(String value) {
        return value != null ? value.replace("'", "\\'") : "";
    }
}
