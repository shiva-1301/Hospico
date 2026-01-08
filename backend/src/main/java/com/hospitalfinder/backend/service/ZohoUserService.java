package com.hospitalfinder.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospitalfinder.backend.entity.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZohoUserService {
    
    private final ZohoDataStoreService dataStoreService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${zoho.enabled:false}")
    private boolean zohoEnabled;
    
    @Value("${zoho.users.table.id}")
    private String usersTableId;
    private static final DateTimeFormatter ZOHO_DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
    
    /**
     * Check if a user exists by email
     */
    public boolean existsByEmail(String email) {
        if (!zohoEnabled) {
            return false;
        }
        
        try {
            JsonNode user = dataStoreService.findByField(usersTableId, "email", email);
            return user != null;
        } catch (Exception e) {
            log.error("Failed to check if user exists by email: {}", email, e);
            throw new RuntimeException("Failed to check user existence", e);
        }
    }
    
    /**
     * Find a user by email
     */
    public UserData findByEmail(String email) {
        if (!zohoEnabled) {
            return null;
        }
        
        try {
            JsonNode user = dataStoreService.findByField(usersTableId, "email", email);
            if (user == null) {
                return null;
            }
            return mapToUserData(user);
        } catch (Exception e) {
            log.error("Failed to find user by email: {}", email, e);
            throw new RuntimeException("Failed to find user", e);
        }
    }
    
    /**
     * Create a new user
     */
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
            
            // Extract the created record from response
            JsonNode dataNode = response.get("data");
            if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                return mapToUserData(dataNode.get(0));
            }
            
            throw new RuntimeException("Failed to create user: invalid response structure");
            
        } catch (Exception e) {
            log.error("Failed to create user: {}", email, e);
            throw new RuntimeException("Failed to create user", e);
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
        user.setRole(Role.valueOf(node.get("role").asText()));
        return user;
    }
    
    /**
     * Simple DTO for user data
     */
    public static class UserData {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String password;
        private Role role;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
    }
}
