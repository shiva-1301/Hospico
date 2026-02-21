package com.hospitalfinder.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.entity.Role;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * User service backed by Zoho Data Store REST API.
 * Delegates all data access to DataStoreService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudScaleNoSqlUserService implements UserStoreService {

    private final DataStoreService dataStoreService;
    private final PasswordEncoder passwordEncoder;

    @Value("${zoho.users.table.id:users}")
    private String usersTable;

    @Override
    public boolean existsByEmail(String email) {
        try {
            JsonNode result = dataStoreService.findByField(usersTable, "email", email);
            return result != null;
        } catch (Exception e) {
            log.error("Error checking if user exists by email: {}", email, e);
            return false;
        }
    }

    @Override
    public UserData findByEmail(String email) {
        try {
            JsonNode result = dataStoreService.findByField(usersTable, "email", email);
            return result != null ? mapToUserData(result) : null;
        } catch (Exception e) {
            log.error("Error finding user by email: {}", email, e);
            return null;
        }
    }

    @Override
    public UserData findById(Long id) {
        try {
            JsonNode result = dataStoreService.findById(usersTable, id);
            return result != null ? mapToUserData(result) : null;
        } catch (Exception e) {
            log.error("Error finding user by id: {}", id, e);
            return null;
        }
    }

    @Override
    public UserData createUser(String name, String email, String phone, String password, Role role) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("email", email);
            data.put("phone", phone);
            data.put("password", passwordEncoder.encode(password));
            data.put("role", role.name());

            JsonNode created = dataStoreService.insertRecord(usersTable, data);
            log.info("✅ User created: {}", email);
            return created != null ? mapToUserData(created) : null;
        } catch (Exception e) {
            log.error("Error creating user: {}", email, e);
            return null;
        }
    }

    @Override
    public UserData updateUser(String email, String name, String phone, Integer age, String gender, String password) {
        try {
            // Find user first to get ROWID
            JsonNode existing = dataStoreService.findByField(usersTable, "email", email);
            if (existing == null)
                return null;

            Long rowId = extractRowId(existing);
            if (rowId == null) {
                log.error("Cannot determine ROWID for user: {}", email);
                return null;
            }

            Map<String, Object> updates = new HashMap<>();
            if (name != null)
                updates.put("name", name);
            if (phone != null)
                updates.put("phone", phone);
            if (age != null)
                updates.put("age", age);
            if (gender != null)
                updates.put("gender", gender);
            if (password != null)
                updates.put("password", passwordEncoder.encode(password));

            JsonNode updated = dataStoreService.updateRecord(usersTable, rowId, updates);
            log.info("✅ User updated: {}", email);
            return updated != null ? mapToUserData(updated) : null;
        } catch (Exception e) {
            log.error("Error updating user: {}", email, e);
            return null;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private UserData mapToUserData(JsonNode node) {
        if (node == null)
            return null;
        UserData user = new UserData();
        user.setId(getLongOrNull(node, "ROWID", "_id"));
        user.setName(getTextOrNull(node, "name"));
        user.setEmail(getTextOrNull(node, "email"));
        user.setPhone(getTextOrNull(node, "phone"));
        user.setPassword(getTextOrNull(node, "password"));
        user.setAge(node.has("age") && !node.get("age").isNull()
                ? node.get("age").asInt()
                : null);
        user.setGender(getTextOrNull(node, "gender"));
        String roleStr = getTextOrNull(node, "role");
        user.setRole(roleStr != null ? Role.valueOf(roleStr) : null);
        return user;
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private Long getLongOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            if (node.has(field) && !node.get(field).isNull()) {
                try {
                    return node.get(field).asLong();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private Long extractRowId(JsonNode data) {
        if (data.has("ROWID") && !data.get("ROWID").isNull())
            return data.get("ROWID").asLong();
        if (data.has("_id") && !data.get("_id").isNull())
            return data.get("_id").asLong();
        return null;
    }
}
