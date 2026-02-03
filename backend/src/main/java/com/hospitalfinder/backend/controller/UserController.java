package com.hospitalfinder.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.service.ZohoUserService;
import com.hospitalfinder.backend.service.ZohoUserService.UserData;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ZohoUserService zohoUserService;

    public UserController(ZohoUserService zohoUserService) {
        this.zohoUserService = zohoUserService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserData> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        UserData user = zohoUserService.findByEmail(email);
        return ResponseEntity.ok(user);
    }
}