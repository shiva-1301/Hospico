package com.hospitalfinder.backend.service;

import java.time.Instant;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.config.ZohoConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZohoAuthService {

    private final ZohoConfig zohoConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private Instant tokenExpiry;

    public synchronized String getAccessToken() {
        // Prioritize OAuth Refresh Token if available
        if (zohoConfig.getRefreshToken() != null && !zohoConfig.getRefreshToken().isEmpty()) {
            if (accessToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
                return accessToken;
            }
            refreshAccessToken();
            return accessToken;
        }

        // Fallback to PAT
        if (zohoConfig.getPatToken() != null && !zohoConfig.getPatToken().isEmpty()) {
            return zohoConfig.getPatToken();
        }

        throw new RuntimeException("No valid Zoho credentials found (Client ID/Secret/Refresh Token OR PAT)");
    }

    public boolean isUsingPat() {
        // If we have a refresh token, we are NOT using PAT (we are using OAuth)
        if (zohoConfig.getRefreshToken() != null && !zohoConfig.getRefreshToken().isEmpty()) {
            return false;
        }
        return zohoConfig.getPatToken() != null && !zohoConfig.getPatToken().isEmpty();
    }

    private void refreshAccessToken() {
        try {
            RestTemplate restTemplate = new RestTemplate();

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("refresh_token", zohoConfig.getRefreshToken());
            params.add("client_id", zohoConfig.getClientId());
            params.add("client_secret", zohoConfig.getClientSecret());
            params.add("grant_type", "refresh_token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    zohoConfig.getTokenUrl(),
                    request,
                    String.class);

            JsonNode node = objectMapper.readTree(response.getBody());

            if (node.has("error")) {
                log.error("Zoho OAuth error: {}", response.getBody());
                throw new RuntimeException("Failed to refresh token: " + node.get("error").asText());
            }

            this.accessToken = node.get("access_token").asText();
            int expiresIn = node.get("expires_in").asInt();
            this.tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);

            log.info("Successfully refreshed Zoho access token, expires in {} seconds", expiresIn);
        } catch (Exception e) {
            log.error("Failed to refresh Zoho access token", e);
            throw new RuntimeException("Failed to obtain Zoho access token", e);
        }
    }
}
