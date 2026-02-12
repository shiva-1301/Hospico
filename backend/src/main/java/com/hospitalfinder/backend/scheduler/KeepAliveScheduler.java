package com.hospitalfinder.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeepAliveScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);
    private final RestTemplate restTemplate = new RestTemplate();

    // Render automatically sets 'RENDER_EXTERNAL_URL' env var
    // Default fallback to null if not present
    // Default fallback to null if not present
    private final String backendUrl = System.getenv("RENDER_EXTERNAL_URL");
    private final String frontendUrl = System.getenv("FRONTEND_URL") != null ? System.getenv("FRONTEND_URL")
            : "https://www.hospiico.com";

    // Ping every 10 minutes (600,000 ms)
    @Scheduled(fixedRate = 600000)
    public void pingServices() {
        // 1. Keep Backend Alive
        if (backendUrl != null && !backendUrl.isBlank()) {
            try {
                String healthUrl = backendUrl + "/api/health";
                logger.debug("Pinging backend health: {}", healthUrl);
                restTemplate.getForObject(healthUrl, String.class);
                logger.info("Backend self-ping successful");
            } catch (Exception e) {
                logger.error("Failed to ping backend: {}", e.getMessage());
            }
        } else {
            logger.debug("RENDER_EXTERNAL_URL not set, skipping backend ping");
        }

        // 2. Keep Frontend Alive (if deployed as a Web Service)
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            try {
                logger.debug("Pinging frontend: {}", frontendUrl);
                restTemplate.getForObject(frontendUrl, String.class);
                logger.info("Frontend keep-alive ping successful");
            } catch (Exception e) {
                logger.error("Failed to ping frontend: {}", e.getMessage());
            }
        }
    }
}
