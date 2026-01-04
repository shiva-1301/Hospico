package com.hospitalfinder.backend.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 50;
    private static final LinkedList<Map<String, String>> recentRequests = new LinkedList<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip logging for static resources and health checks
        String path = request.getRequestURI();
        if (path.equals("/") || path.equals("/api/health") || path.equals("/actuator/health") || path.equals("/api/requests/recent")) {
            return true;
        }

        synchronized (recentRequests) {
            Map<String, String> requestInfo = Map.of(
                "timestamp", LocalDateTime.now().format(formatter),
                "method", request.getMethod(),
                "path", path,
                "ip", getClientIP(request)
            );
            
            recentRequests.addFirst(requestInfo);
            
            if (recentRequests.size() > MAX_REQUESTS) {
                recentRequests.removeLast();
            }
        }
        
        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }

    public static List<Map<String, String>> getRecentRequests() {
        synchronized (recentRequests) {
            return new LinkedList<>(recentRequests);
        }
    }
}
