package com.hospitalfinder.backend.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hospitalfinder.backend.config.ZohoConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZohoDataStoreService {
    
    private final ZohoConfig zohoConfig;
    private final ZohoAuthService zohoAuthService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Insert a record into a Zoho Data Store table
     * @param tableId The ID of the table (e.g., "26566000000019177")
     * @param data Map of field names to values
     * @return The created record with ID
     */
    public JsonNode insertRecord(String tableId, Map<String, Object> data) {
        try {
            String url = zohoConfig.getDataStoreUrl() + "/" + tableId + "/row";
            String accessToken = zohoAuthService.getAccessToken();
            
            RestTemplate restTemplate = new RestTemplate();
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            ObjectNode dataNode = objectMapper.createObjectNode();
            
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof String) {
                    dataNode.put(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    dataNode.put(entry.getKey(), (Integer) entry.getValue());
                } else if (entry.getValue() instanceof Long) {
                    dataNode.put(entry.getKey(), (Long) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    dataNode.put(entry.getKey(), (Boolean) entry.getValue());
                }
            }
            
            requestBody.set("data", dataNode);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Zoho-oauthtoken " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            log.debug("Inserting record into table {}: {}", tableId, requestBody);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            log.debug("Zoho Data Store insert response: {}", response.getBody());
            
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            
            if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                log.error("Failed to insert record: {}", response.getBody());
                throw new RuntimeException("Failed to insert record: " + response.getBody());
            }
            
            return responseNode;
        } catch (Exception e) {
            log.error("Failed to insert record into table {}", tableId, e);
            throw new RuntimeException("Failed to insert record", e);
        }
    }
    
    /**
     * Query records from a Zoho Data Store table
     * @param tableId The ID of the table
     * @param criteria Optional WHERE clause (e.g., "Email=='test@example.com'")
     * @return Array of matching records
     */
    public JsonNode queryRecords(String tableId, String criteria) {
        try {
            String baseUrl = zohoConfig.getDataStoreUrl() + "/" + tableId + "/row";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
            
            if (criteria != null && !criteria.isEmpty()) {
                builder.queryParam("filter", criteria);
            }
            
            String url = builder.toUriString();
            String accessToken = zohoAuthService.getAccessToken();
            
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Zoho-oauthtoken " + accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            log.debug("Querying table {}: {}", tableId, url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
            );
            
            log.debug("Zoho Data Store query response: {}", response.getBody());
            
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            
            if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                log.error("Failed to query records: {}", response.getBody());
                throw new RuntimeException("Failed to query records: " + response.getBody());
            }
            
            return responseNode.get("data");
        } catch (Exception e) {
            log.error("Failed to query records from table {}", tableId, e);
            throw new RuntimeException("Failed to query records", e);
        }
    }
    
    /**
     * Find a record by a specific field value
     * @param tableId The table ID
     * @param fieldName The field to search
     * @param fieldValue The value to match
     * @return The first matching record, or null if not found
     */
    public JsonNode findByField(String tableId, String fieldName, String fieldValue) {
        String criteria = String.format("%s==\"%s\"", fieldName, fieldValue.replace("\"", "\\\""));
        JsonNode results = queryRecords(tableId, criteria);
        
        if (results != null && results.isArray() && results.size() > 0) {
            return results.get(0);
        }
        
        return null;
    }
}
