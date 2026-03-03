package com.hospitalfinder.backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.hospitalfinder.backend.filter.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final Environment environment;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, Environment environment) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean enableSpringCors = Boolean.parseBoolean(
                environment.getProperty("ENABLE_SPRING_CORS", "false"));

        if (enableSpringCors) {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        } else {
            // CORS handled by Catalyst AppSail infrastructure - disabled here to prevent
            // duplicate headers
            http.cors(AbstractHttpConfigurer::disable);
        }

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight - MUST BE FIRST
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // Health check endpoints
                        .requestMatchers("/", "/api/health", "/health/**", "/actuator/**", "/actuator/health")
                        .permitAll()
                        // Auth endpoints
                        .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/users/me").permitAll()
                        // Public API endpoints
                        .requestMatchers("/api/clinics", "/api/clinics/**",
                                "/api/specializations", "/api/specializations/**",
                                "/api/doctors", "/api/doctors/**",
                                "/api/appointments", "/api/appointments/**",
                                "/api/medical-records", "/api/medical-records/**",
                                "/api/chat", "/api/chat/action",
                                "/api/reviews", "/api/reviews/**")
                        .permitAll()
                        .requestMatchers("/api/requests", "/api/requests/**").permitAll()
                        // Documentation
                        .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Protected endpoints
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        String origins = environment.getProperty("CORS_ALLOWED_ORIGINS", "http://localhost:5173");
        List<String> allowedOrigins = new ArrayList<>();
        for (String origin : origins.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty() && !allowedOrigins.contains(trimmed)) {
                allowedOrigins.add(trimmed);
            }
        }
        // Use setAllowedOrigins for exact match (prevents pattern wildcards)
        config.setAllowedOrigins(allowedOrigins);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
