package com.hospitalfinder.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.dto.LoginRequest;
import com.hospitalfinder.backend.dto.LoginResponse;
import com.hospitalfinder.backend.entity.User;
import com.hospitalfinder.backend.service.JwtService;
import com.hospitalfinder.backend.service.ZohoUserService;
import com.hospitalfinder.backend.service.ZohoUserService.UserData;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final ZohoUserService zohoUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginController(ZohoUserService zohoUserService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.zohoUserService = zohoUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        UserData userData = zohoUserService.findByEmail(request.getEmail());

        if (userData == null || !passwordEncoder.matches(request.getPassword(), userData.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "Invalid credentials", null, null, null, null, null));
        }

        // Convert UserData to User POJO for JWT generation
        User user = new User();
        user.setId(userData.getId());
        user.setEmail(userData.getEmail());
        user.setName(userData.getName());
        user.setRole(userData.getRole());

        // Generate JWT token
        String jwtToken = jwtService.generateToken(user);

        // Create JWT cookie
        Cookie jwtCookie = new Cookie("jwt_token", jwtToken);
        jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true); // For security - prevents XSS
        jwtCookie.setSecure(true); // Only sent over HTTPS
        jwtCookie.setAttribute("SameSite", "None");
        response.addCookie(jwtCookie);

        // Create user info cookie (for frontend convenience)
        Cookie userCookie = new Cookie("user_info", user.getEmail());
        userCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        userCookie.setPath("/");
        userCookie.setHttpOnly(false); // Allow JavaScript access
        userCookie.setSecure(true);
        userCookie.setAttribute("SameSite", "None");
        response.addCookie(userCookie);

        return ResponseEntity.ok(new LoginResponse(
                true,
                "Login successful",
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                jwtToken));
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request) {

        // Try Authorization header first (Bearer token)
        String token = null;

        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        } else {
            // Fallback to cookie named "jwt_token"
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie c : cookies) {
                    if ("jwt_token".equals(c.getName())) {
                        token = c.getValue();
                        break;
                    }
                }
            }
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "Missing token", null, null, null, null, null));
        }

        String email;
        try {
            // Assumes JwtService provides a method to extract username/email from token
            email = jwtService.extractUsername(token);
            // Optionally validate the token (if your JwtService exposes such method)
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponse(false, "Invalid token", null, null, null, null, null));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "Invalid token", null, null, null, null, null));
        }

        UserData userData = zohoUserService.findByEmail(email);
        if (userData == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "User not found", null, null, null, null, null));
        }

        return ResponseEntity.ok(new LoginResponse(
                true,
                "User fetched",
                userData.getId(),
                userData.getEmail(),
                userData.getName(),
                userData.getRole(),
                token));
    }
}