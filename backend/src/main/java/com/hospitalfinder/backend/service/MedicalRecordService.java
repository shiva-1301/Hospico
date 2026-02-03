package com.hospitalfinder.backend.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.entity.MedicalRecord;
import com.hospitalfinder.backend.entity.User;
import com.hospitalfinder.backend.service.ZohoUserService.UserData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordService {

    private final ZohoUserService zohoUserService;
    private final ZohoDataStoreService dataStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MedicalRecord uploadFile(MultipartFile file, String category, Long userId) throws IOException {
        UserData userData = zohoUserService.findById(userId);
        if (userData == null) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        // Store metadata in Zoho Data Store
        // Note: Binary data storage in Data Store columns is limited/inappropriate.
        // Ideally use Zoho File Store or Cloud Storage.
        // For migration POC, we store metadata only.

        Map<String, Object> values = new HashMap<>();
        values.put("name", file.getOriginalFilename());
        values.put("type", file.getContentType());
        values.put("size", file.getSize());
        values.put("category", category);
        values.put("upload_date", LocalDateTime.now().toString());
        values.put("user_id", userId);

        // Skip 'data' field for now to avoid size limits

        try {
            JsonNode result = dataStoreService.insertRecord("medical_records", values);
            MedicalRecord record = objectMapper.convertValue(result, MedicalRecord.class);

            // Manually link user for return
            User user = new User();
            user.setId(userData.getId());
            user.setName(userData.getName());
            user.setEmail(userData.getEmail());
            record.setUser(user);

            return record;
        } catch (Exception e) {
            log.error("Failed to upload medical record", e);
            throw new IOException("Failed to save record metadata", e);
        }
    }

    public List<MedicalRecord> getRecordsByUserId(Long userId) {
        try {
            String query = "SELECT * FROM medical_records WHERE user_id = '" + userId + "'";
            JsonNode result = dataStoreService.executeZCQL(query);
            List<MedicalRecord> records = new ArrayList<>();
            if (result != null && result.isArray()) {
                for (JsonNode node : result) {
                    JsonNode data = node.has("medical_records") ? node.get("medical_records") : node;
                    records.add(objectMapper.convertValue(data, MedicalRecord.class));
                }
            }
            return records;
        } catch (Exception e) {
            log.error("Error fetching medical records", e);
            return new ArrayList<>();
        }
    }

    public Optional<MedicalRecord> getRecordById(Long id) {
        try {
            String query = "SELECT * FROM medical_records WHERE ROWID = '" + id + "'";
            JsonNode result = dataStoreService.executeZCQL(query);
            if (result != null && result.isArray() && result.size() > 0) {
                JsonNode node = result.get(0);
                JsonNode data = node.has("medical_records") ? node.get("medical_records") : node;
                return Optional.of(objectMapper.convertValue(data, MedicalRecord.class));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching record by id", e);
            return Optional.empty();
        }
    }

    public void deleteRecord(Long id) {
        // Need tableId for deleteRecord API. Can fetch via config or just assume name
        // if allowed?
        // DataStoreService.deleteRecord uses URL with TableID.
        // I need to use ZCQL DELETE or find Table ID.
        // Assuming I know the Table ID or can look it up.
        // For now, I'll log that delete is tricky without ID, or use ZCQL DELETE if
        // supported.
        // ZCQL DELETE works: DELETE FROM table WHERE ...
        try {
            dataStoreService.executeZCQL("DELETE FROM medical_records WHERE ROWID = '" + id + "'");
        } catch (Exception e) {
            log.error("Failed to delete record", e);
            throw new RuntimeException("Failed to delete record", e);
        }
    }
}
