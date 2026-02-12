package com.hospitalfinder.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.entity.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "data.store.provider", havingValue = "zoho")
@RequiredArgsConstructor
@Slf4j
public class ZohoUserService implements UserStoreService {

    private final DataStoreService dataStoreService;
    private final PasswordEncoder passwordEncoder;

    @Value("${zoho.enabled:false}")
    private boolean zohoEnabled;

    @Value("${zoho.users.table.id}")
    private String usersTableId;

    private static final DateTimeFormatter ZOHO_DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    /**
     * Check if a user exists by email
     */
    @Override
    public boolean existsByEmail(String email) {
        if (!zohoEnabled) {
            return false;
        }

        try {
            log.info("Checking if user exists by email: {} (tableId: {})", email, usersTableId);
            JsonNode user = dataStoreService.findByField(usersTableId, "email", email);
            boolean exists = user != null;
            log.info("User exists check result: {}", exists);
            return exists;
        } catch (Exception e) {
            log.error("Failed to check if user exists by email: {} (tableId: {})", email, usersTableId, e);
            throw new RuntimeException("Failed to check user existence: " + e.getMessage(), e);
        }
    }

    /**
     * Find a user by email
     */
    @Override
    public UserData findByEmail(String email) {
        if (!zohoEnabled) {
            return null;
        }

        try {
            log.info("Finding user by email: {} (tableId: {})", email, usersTableId);
            JsonNode user = dataStoreService.findByField(usersTableId, "email", email);
            if (user == null) {
                log.info("User not found for email: {}", email);
                return null;
            }
            log.info("User found for email: {}", email);
            return mapToUserData(user);
        } catch (Exception e) {
            log.error("Failed to find user by email: {} (tableId: {})", email, usersTableId, e);
            throw new RuntimeException("Failed to find user: " + e.getMessage(), e);
        }
    }

    /**
     * Find a user by ID
     */
    @Override
    public UserData findById(Long id) {
        if (!zohoEnabled) {
            return null;
        }

        try {
            JsonNode user = dataStoreService.findById(usersTableId, id);
            if (user == null) {
                return null;
            }
            return mapToUserData(user);
        } catch (Exception e) {
            log.error("Failed to find user by id: {}", id, e);
            throw new RuntimeException("Failed to find user by id", e);
        }
    }

    /**
     * Create a new user
     */
    @Override
    public UserData createUser(String name, String email, String phone, String password, Role role) {
        if (!zohoEnabled) {
            throw new RuntimeException("Zoho Data Store is not enabled");
        }

        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("email", email);
            userData.put("phone", phone);
            userData.put("password", passwordEncoder.encode(password));
            userData.put("role", role.name());
            userData.put("created_at", LocalDateTime.now().format(ZOHO_DATETIME_FORMAT));
            userData.put("updated_at", LocalDateTime.now().format(ZOHO_DATETIME_FORMAT));

            JsonNode response = dataStoreService.insertRecord(usersTableId, userData);

            if (response != null && response.has("data")) {
                JsonNode dataNode = response.get("data");
                if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                    return mapToUserData(dataNode.get(0));
                }
            }

            if (response != null && response.has("email")) {
                return mapToUserData(response);
            }

            throw new RuntimeException("Failed to create user: no data returned");

        } catch (Exception e) {
            log.error("Failed to create user: {}", email, e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    @Override
    public UserData updateUser(String email, String name, String phone, Integer age, String gender, String password) {
        if (!zohoEnabled) {
            throw new RuntimeException("Zoho Data Store is not enabled");
        }

        UserData existing = findByEmail(email);
        if (existing == null) {
            return null;
        }

        Map<String, Object> updates = new HashMap<>();
        if (name != null) {
            updates.put("name", name);
        }
        if (phone != null) {
            updates.put("phone", phone);
        }
        if (age != null) {
            updates.put("age", age);
        }
        if (gender != null) {
            updates.put("gender", gender);
        }
        if (password != null && !password.isBlank()) {
            updates.put("password", passwordEncoder.encode(password));
        }
        updates.put("updated_at", LocalDateTime.now().format(ZOHO_DATETIME_FORMAT));

        if (updates.size() == 1) {
            return existing;
        }

        try {
            dataStoreService.updateRecord(usersTableId, existing.getId(), updates);
            return findByEmail(email);
        } catch (Exception e) {
            log.error("Failed to update user: {}", email, e);
            throw new RuntimeException("Failed to update user", e);
        }
    }

    /**
     * Map Zoho record to UserData DTO
     */
    private UserData mapToUserData(JsonNode node) {
        UserData user = new UserData();
        user.setId(node.get("ROWID").asLong());
        user.setName(node.get("name").asText());
        user.setEmail(node.get("email").asText());
        user.setPhone(node.has("phone") ? node.get("phone").asText() : null);
        user.setPassword(node.get("password").asText());
        if (node.has("age") && !node.get("age").isNull()) {
            user.setAge(node.get("age").asInt());
        }
        if (node.has("gender") && !node.get("gender").isNull()) {
            user.setGender(node.get("gender").asText());
        }
        user.setRole(Role.valueOf(node.get("role").asText()));
        return user;
    }
}
