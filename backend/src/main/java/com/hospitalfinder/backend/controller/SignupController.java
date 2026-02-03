package com.hospitalfinder.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.dto.LoginResponse;
import com.hospitalfinder.backend.dto.SignupRequest;
import com.hospitalfinder.backend.entity.Role;
import com.hospitalfinder.backend.entity.User;
import com.hospitalfinder.backend.service.JwtService;
import com.hospitalfinder.backend.service.ZohoUserService;
import com.hospitalfinder.backend.service.ZohoUserService.UserData;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class SignupController {

    private final JwtService jwtService;
    private final ZohoUserService zohoUserService;

    public SignupController(PasswordEncoder passwordEncoder,
            JwtService jwtService, ZohoUserService zohoUserService) {
        this.jwtService = jwtService;
        this.zohoUserService = zohoUserService;
    }

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(@RequestBody SignupRequest request, HttpServletResponse response) {
        // Use Zoho Data Store exclusively
        return signupWithZoho(request, response);
    }

    private ResponseEntity<LoginResponse> signupWithZoho(SignupRequest request, HttpServletResponse response) {
        if (zohoUserService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new LoginResponse(false, "Email already registered", null, null, null, null, null));
        }

        Role role = request.getRole() != null ? request.getRole() : Role.USER;
        UserData userData = zohoUserService.createUser(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                role);

        // Generate JWT token - create temporary User entity for JWT generation
        User tempUser = new User();
        tempUser.setId(userData.getId());
        tempUser.setEmail(userData.getEmail());
        tempUser.setName(userData.getName());
        tempUser.setRole(userData.getRole());

        String jwtToken = jwtService.generateToken(tempUser);

        // Set cookies
        setCookies(response, jwtToken, userData.getEmail());

        return ResponseEntity.ok(new LoginResponse(
                true,
                "User registered successfully",
                userData.getId(),
                userData.getEmail(),
                userData.getName(),
                userData.getRole(),
                jwtToken));
    }

    // Postgres implementation removed

    private void setCookies(HttpServletResponse response, String jwtToken, String email) {
        // Create JWT cookie
        Cookie jwtCookie = new Cookie("jwt_token", jwtToken);
        jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setAttribute("SameSite", "None");
        response.addCookie(jwtCookie);

        // Create user info cookie
        Cookie userCookie = new Cookie("user_info", email);
        userCookie.setMaxAge(7 * 24 * 60 * 60);
        userCookie.setPath("/");
        userCookie.setHttpOnly(false);
        userCookie.setSecure(true);
        userCookie.setAttribute("SameSite", "None");
        response.addCookie(userCookie);
    }
}