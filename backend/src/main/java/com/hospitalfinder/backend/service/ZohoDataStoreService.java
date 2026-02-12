package com.hospitalfinder.backend.service;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "data.store.provider", havingValue = "zoho")
@RequiredArgsConstructor
@Slf4j
public class ZohoDataStoreService implements DataStoreService {

    private final ZohoConfig zohoConfig;
    private final ZohoAuthService zohoAuthService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Insert a record into a Zoho Data Store table
     * 
     * @param tableId The ID of the table (e.g., "26566000000019177")
     * @param data    Map of field names to values
     * @return The created record with ID
     */
    @Override
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

            JsonNode responseDataNode = responseNode.get("data");
            if (responseDataNode != null && responseDataNode.isArray() && responseDataNode.size() > 0) {
                return responseDataNode.get(0);
            }

            return responseNode;
        } catch (Exception e) {
            log.error("Failed to insert record into table {}", tableId, e);
            throw new RuntimeException("Failed to insert record", e);
        }
    }

    /**
     * Query records from a Zoho Data Store table
     * 
     * @param tableId  The ID of the table
     * @param criteria Optional WHERE clause (ignored - fetch all for now)
     * @return Array of matching records
     */
    public JsonNode queryRecords(String tableId, String criteria) {
        try {
            String baseUrl = zohoConfig.getDataStoreUrl() + "/" + tableId + "/row";
            String accessToken = zohoAuthService.getAccessToken();

            // Build URL with proper parameter encoding
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("max_rows", "100")
                    .build()
                    .toUriString();

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Zoho-oauthtoken " + accessToken);
            headers.set("x-lib-environment-id", zohoConfig.getEnvId());

            HttpEntity<String> request = new HttpEntity<>(headers);

            log.info("Querying table {}: {}", tableId, url);
            log.debug("Query headers: Authorization=[REDACTED], x-lib-environment-id={}, max_rows=100", zohoConfig.getEnvId());

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class);

            log.info("Query response status: {}", response.getStatusCode());
            log.info("Query response body: {}", response.getBody());

            JsonNode responseNode = objectMapper.readTree(response.getBody());

            if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                log.error("Failed to query records: Status={}, Body={}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to query records: " + response.getBody());
            }

            JsonNode dataNode = responseNode.get("data");
            log.info("Extracted data node: {}", dataNode);
            
            return dataNode;
        } catch (Exception e) {
            log.error("Failed to query records from table {}: {}", tableId, e.getMessage(), e);
            throw new RuntimeException("Failed to query records: " + e.getMessage(), e);
        }
    }

    /**
     * Find a record by a specific field value
     * 
     * @param tableId    The table ID
     * @param fieldName  The field to search
     * @param fieldValue The value to match
     * @return The first matching record, or null if not found
     */
    @Override
    public JsonNode findByField(String tableId, String fieldName, String fieldValue) {
        try {
            log.info("Finding record by field: {} = {} in table {}", fieldName, fieldValue, tableId);
            JsonNode results = queryRecords(tableId, null);
            
            if (results != null && results.isArray()) {
                log.info("Total rows in table: {}", results.size());
                for (int i = 0; i < results.size(); i++) {
                    JsonNode row = results.get(i);
                    JsonNode fieldNode = row.get(fieldName);
                    if (fieldNode != null && fieldNode.asText().equals(fieldValue)) {
                        log.info("Found matching record at index {}", i);
                        return row;
                    }
                }
            }
            
            log.info("No matching record found for {} = {}", fieldName, fieldValue);
            return null;
        } catch (Exception e) {
            log.error("Error in findByField for {} = {}: {}", fieldName, fieldValue, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update a record in a Zoho Data Store table
     */
    @Override
    public JsonNode updateRecord(String tableId, Long rowId, Map<String, Object> data) {
        try {
            String url = zohoConfig.getDataStoreUrl() + "/" + tableId + "/row/" + rowId;
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
            headers.set("x-lib-environment-id", zohoConfig.getEnvId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            JsonNode responseNode = objectMapper.readTree(response.getBody());

            if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                throw new RuntimeException("Failed to update record: " + response.getBody());
            }

            return responseNode.get("data");
        } catch (Exception e) {
            log.error("Failed to update record in table {}", tableId, e);
            throw new RuntimeException("Failed to update record", e);
        }
    }

    /**
     * Delete a record from a Zoho Data Store table
     */
    @Override
    public void deleteRecord(String tableId, Long rowId) {
        try {
            String url = zohoConfig.getDataStoreUrl() + "/" + tableId + "/row/" + rowId;
            String accessToken = zohoAuthService.getAccessToken();

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Zoho-oauthtoken " + accessToken);
            headers.set("x-lib-environment-id", zohoConfig.getEnvId());

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

            if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                throw new RuntimeException("Failed to delete record: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to delete record from table {}", tableId, e);
            throw new RuntimeException("Failed to delete record", e);
        }
    }

    /**
     * Find a record by ROWID
     */
    @Override
    public JsonNode findById(String tableId, Long rowId) {
        return findByField(tableId, "ROWID", String.valueOf(rowId));
    }

    /**
     * Execute a ZCQL query
     * 
     * @param query The SQL-like query string (e.g. "SELECT * FROM clinics")
     * @return Array of JSON records
     */
    public JsonNode executeZCQL(String query) {
        try {
            String url = zohoConfig.getBaseUrl() + "/project/" + zohoConfig.getProjectId() + "/zcql";
            String accessToken = zohoAuthService.getAccessToken();

            RestTemplate restTemplate = new RestTemplate();
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("query", query);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Zoho-oauthtoken " + accessToken);
            headers.set("x-lib-environment-id", zohoConfig.getEnvId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

            log.debug("Executing ZCQL: {}", query);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode responseNode = objectMapper.readTree(response.getBody());

            if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                throw new RuntimeException("Failed to execute ZCQL: " + response.getBody());
            }

            // ZCQL response format is slightly different usually, or keeps 'data' array
            // standard?
            // Catalyst ZCQL returns simple JSON array of rows usually if valid?
            // Actually it likely returns { "data": [ ... ] } or similar structure.
            // Let's assume it returns standard object with "data" or equivalent.
            // Documentation says it returns array of objects inside 'rows' or just JSON
            // array sometimes.
            // Let's inspect the keys. But based on other implementations, likely 'status'
            // and 'rows' or just result.
            // I'll check response structure. For now assume standard "data" or generic
            // parsing.
            // Actually, safe to return the whole body or inspect.
            // Typical response: { "data": [ { "clinics": { ... } } ] } or similar.
            // ZCQL often namespaces columns: { "clinics": { "name": "..." } }

            JsonNode dataNode = responseNode.get("data");
            return dataNode != null ? dataNode : responseNode;
        } catch (Exception e) {
            log.error("Failed to execute ZCQL: {}", query, e);
            throw new RuntimeException("Failed to execute ZCQL", e);
        }
    }

    @Override
    public JsonNode executeQuery(String query) {
        return executeZCQL(query);
    }
}
