package com.hospitalfinder.backend.config;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages Zoho Catalyst OAuth tokens and provides the base URL
 * for Data Store REST API calls.
 */
@Configuration
@Slf4j
public class CloudScaleConfig {

    @Value("${zoho.project.id}")
    @Getter
    private String projectId;

    @Value("${zoho.client.id}")
    private String clientId;

    @Value("${zoho.client.secret}")
    private String clientSecret;

    @Value("${zoho.refresh.token}")
    private String refreshToken;

    @Value("${zoho.region:accounts.zoho.in}")
    private String region;

    private final RestTemplate restTemplate = new RestTemplate();

    private String accessToken;
    private Instant tokenExpiry = Instant.MIN;

    @PostConstruct
    public void init() {
        try {
            refreshAccessToken();
            log.info("✅ Zoho OAuth access token obtained for project: {}", projectId);
        } catch (Exception e) {
            log.error("❌ Failed to obtain Zoho OAuth access token", e);
            throw new RuntimeException("Failed to initialize Zoho OAuth", e);
        }
    }

    /**
     * Returns a valid access token, refreshing if expired.
     */
    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(tokenExpiry)) {
            refreshAccessToken();
        }
        return accessToken;
    }

    /**
     * Returns the Data Store REST API base URL for this project.
     * Example:
     * https://api.catalyst.zoho.in/baas/v1/project/26566000000013009
     */
    public String getBaseUrl() {
        // Derive API host from the accounts region
        // accounts.zoho.in -> api.catalyst.zoho.in
        // accounts.zoho.com -> api.catalyst.zoho.com
        String apiHost = region.replace("accounts.", "api.catalyst.");
        return "https://" + apiHost + "/baas/v1/project/" + projectId;
    }

    private void refreshAccessToken() {
        String tokenUrl = "https://" + region + "/oauth/v2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", refreshToken);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("access_token")) {
                this.accessToken = (String) body.get("access_token");
                // Zoho tokens last ~1 hour; refresh 5 min early
                int expiresIn = body.containsKey("expires_in")
                        ? ((Number) body.get("expires_in")).intValue()
                        : 3600;
                this.tokenExpiry = Instant.now().plusSeconds(expiresIn - 300);
                log.debug("Zoho access token refreshed, expires in {}s", expiresIn);
            } else {
                throw new RuntimeException("No access_token in Zoho OAuth response: " + body);
            }
        } catch (Exception e) {
            log.error("Failed to refresh Zoho access token", e);
            throw new RuntimeException("Failed to refresh Zoho access token", e);
        }
    }
}
