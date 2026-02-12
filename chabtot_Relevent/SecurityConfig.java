package com.hospitalfinder.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.hospitalfinder.backend.filter.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight - MUST BE FIRST
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // Health check endpoints
                        .requestMatchers("/", "/api/health", "/health/**", "/actuator/**", "/actuator/health")
                        .permitAll()
                        // Auth endpoints
                        .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/partner/signup").permitAll()
                        // User profile endpoints (controller validates token)
                        .requestMatchers("/api/users/me").permitAll()
                        // Public API endpoints (read-only)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/clinics/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/specializations/**").permitAll()
                        // Chat endpoints - permit all for chatbot conversations and booking workflow
                        .requestMatchers("/api/chat").permitAll()
                        .requestMatchers("/api/chat/action").permitAll()
                        // Clinic modifications require authentication
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/clinics").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/clinics/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/clinics/**").authenticated()
                        // Doctor modifications require authentication
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/clinics/*/doctors")
                        .authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/doctors/**").authenticated()
                        // Appointment endpoints - permit for chat booking workflow (will be validated in controller)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/appointments/user/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/appointments").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/appointments/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/appointments/**").authenticated()
                        // Medical records endpoints - permit for chat workflow
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/medical-records/user/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/medical-records").permitAll()
                        .requestMatchers("/api/requests/**").permitAll()
                        // Documentation
                        .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Protected endpoints (admin-level)
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
