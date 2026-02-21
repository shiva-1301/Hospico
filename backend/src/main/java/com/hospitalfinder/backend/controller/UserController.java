package com.hospitalfinder.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.dto.UserUpdateDTO;
import com.hospitalfinder.backend.service.UserStoreService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserStoreService userStoreService;

    public UserController(UserStoreService userStoreService) {
        this.userStoreService = userStoreService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserData> getMe() {
        return getProfileInternal();
    }

    @GetMapping("/profile")
    public ResponseEntity<UserData> getProfile() {
        return getProfileInternal();
    }

    @PatchMapping("/me")
    public ResponseEntity<UserData> updateMe(@RequestBody UserUpdateDTO request) {
        return updateProfileInternal(request);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserData> updateProfile(@RequestBody UserUpdateDTO request) {
        return updateProfileInternal(request);
    }

    private ResponseEntity<UserData> updateProfileInternal(UserUpdateDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();

        UserData updated = userStoreService.updateUser(
                email,
                request.getName(),
                request.getPhone(),
                request.getAge(),
                request.getGender(),
                request.getPassword());

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    private ResponseEntity<UserData> getProfileInternal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();

        UserData user = userStoreService.findByEmail(email);
        return ResponseEntity.ok(user);
    }
}