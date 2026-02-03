package com.hospitalfinder.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class ZohoConfig {

    @Value("${zoho.project.id}")
    private String projectId;

    @Value("${zoho.env.id}")
    private String envId;

    @Value("${zoho.region:accounts.zoho.in}")
    private String region;

    @Value("${zoho.client.id}")
    private String clientId;

    @Value("${zoho.client.secret}")
    private String clientSecret;

    @Value("${zoho.refresh.token}")
    private String refreshToken;

    @Value("${zoho.users.table.id}")
    private String usersTableId;

    public String getBaseUrl() {
        // Correct Base URL for Catalyst API in IN region
        return "https://api.catalyst.zoho.in/baas/v1";
    }

    public String getDataStoreUrl() {
        return String.format("%s/project/%s/table", getBaseUrl(), projectId);
    }

    public String getTokenUrl() {
        return String.format("https://%s/oauth/v2/token", region);
    }
}
