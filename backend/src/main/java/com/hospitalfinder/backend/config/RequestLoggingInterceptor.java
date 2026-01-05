package com.hospitalfinder.backend.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 10;
    private static final LinkedList<Map<String, Object>> recentRequests = new LinkedList<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip logging for static resources, health checks, and auth endpoints
        String path = request.getRequestURI();
        if (path.equals("/") || path.equals("/api/health") || path.equals("/actuator/health") 
            || path.equals("/api/requests/recent") || path.startsWith("/api/auth/")) {
            return true;
        }

        synchronized (recentRequests) {
            Map<String, Object> requestInfo = new LinkedHashMap<>();
            requestInfo.put("timestamp", LocalDateTime.now().format(formatter));
            requestInfo.put("method", request.getMethod());
            requestInfo.put("path", path);
            
            // Add query parameters if present
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                requestInfo.put("queryParams", queryString);
            }
            
            // Skip body reading for auth endpoints to avoid "getReader() already called" error
            // Add request body for POST/PUT/PATCH (except auth endpoints)
            if (("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod()) || "PATCH".equals(request.getMethod()))
                && !path.startsWith("/api/auth/")) {
                try {
                    String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                    if (body != null && !body.isEmpty() && body.length() < 500) {
                        // Mask sensitive fields
                        body = maskSensitiveData(body);
                        requestInfo.put("requestBody", body);
                    } else if (body != null && body.length() >= 500) {
                        requestInfo.put("requestBody", "[Large payload: " + body.length() + " bytes]");
                    }
                } catch (IOException e) {
                    requestInfo.put("requestBody", "[Unable to read]");
                }
            }
            
            recentRequests.addFirst(requestInfo);
            
            if (recentRequests.size() > MAX_REQUESTS) {
                recentRequests.removeLast();
            }
        }
        
        return true;
    }
    
    private String maskSensitiveData(String body) {
        // Mask password fields
        return body.replaceAll("(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3")
                   .replaceAll("(\"token\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3");
    }

    public static List<Map<String, Object>> getRecentRequests() {
        synchronized (recentRequests) {
            return new LinkedList<>(recentRequests);
        }
    }
}
