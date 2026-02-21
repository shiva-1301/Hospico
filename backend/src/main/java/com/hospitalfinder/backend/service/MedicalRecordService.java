package com.hospitalfinder.backend.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.entity.MedicalRecord;
import com.hospitalfinder.backend.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordService {

    private final UserStoreService userStoreService;
    private final DataStoreService dataStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MedicalRecord uploadFile(MultipartFile file, String category, Long userId) throws IOException {
        UserData userData = null;
        try {
            userData = userStoreService.findById(userId);
            if (userData == null) {
                log.warn("User not found with id: {}. Proceeding with upload metadata only.", userId);
            }
        } catch (Exception e) {
            log.warn("User lookup failed for id {}. Proceeding with upload metadata only.", userId, e);
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
        values.put("data", file.getBytes());

        try {
            JsonNode result = dataStoreService.insertRecord("medical_records", values);
            MedicalRecord record = new MedicalRecord();
            record.setId(result.has("id") ? result.get("id").asLong()
                    : result.has("ROWID") ? result.get("ROWID").asLong() : null);
            record.setName(getText(result, "name", file.getOriginalFilename()));
            record.setType(getText(result, "type", file.getContentType()));
            record.setSize(result.has("size") ? result.get("size").asLong() : file.getSize());
            record.setCategory(getText(result, "category", category));
            record.setUploadDate(
                    parseUploadDate(result.has("upload_date") ? result.get("upload_date").asText() : null));

            // Manually link user for return
            if (userData != null) {
                User user = new User();
                user.setId(userData.getId());
                user.setName(userData.getName());
                user.setEmail(userData.getEmail());
                record.setUser(user);
            }

            return record;
        } catch (Exception e) {
            log.error("Failed to upload medical record", e);
            throw new IOException("Failed to save record metadata", e);
        }
    }

    public List<MedicalRecord> getRecordsByUserId(Long userId) {
        try {
            String query = "SELECT id, name, type, size, category, upload_date, user_id FROM medical_records WHERE user_id = '"
                    + userId + "'";
            JsonNode result = dataStoreService.executeQuery(query);
            List<MedicalRecord> records = new ArrayList<>();
            if (result != null && result.isArray()) {
                for (JsonNode node : result) {
                    JsonNode data = node.has("medical_records") ? node.get("medical_records") : node;
                    MedicalRecord record = mapRecordFromNode(data, false);
                    if (record != null) {
                        records.add(record);
                    }
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
            JsonNode data = dataStoreService.findById("medical_records", id);
            if (data == null || data.isNull()) {
                return Optional.empty();
            }
            JsonNode node = data.has("medical_records") ? data.get("medical_records") : data;
            MedicalRecord record = mapRecordFromNode(node, true);
            return record == null ? Optional.empty() : Optional.of(record);
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
            dataStoreService.executeQuery("DELETE FROM medical_records WHERE ROWID = '" + id + "'");
        } catch (Exception e) {
            log.error("Failed to delete record", e);
            throw new RuntimeException("Failed to delete record", e);
        }
    }

    public MedicalRecord updateMedicalRecord(Long id, Map<String, Object> data) {
        try {
            JsonNode result = dataStoreService.updateRecord("medical_records", id, data);
            JsonNode rowData = result.has("medical_records") ? result.get("medical_records") : result;
            return mapRecordFromNode(rowData, false);
        } catch (Exception e) {
            log.error("Failed to update medical record {}", id, e);
            throw new RuntimeException("Failed to update medical record", e);
        }
    }

    private String getText(JsonNode node, String field, String fallback) {
        if (node == null || field == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        return value.asText();
    }

    private LocalDateTime parseUploadDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            log.warn("Failed to parse upload_date: {}", value, e);
            return LocalDateTime.now();
        }
    }

    private MedicalRecord mapRecordFromNode(JsonNode node, boolean includeData) {
        if (node == null || node.isNull()) {
            return null;
        }
        MedicalRecord record = new MedicalRecord();
        record.setId(node.has("id") ? node.get("id").asLong()
                : node.has("ROWID") ? node.get("ROWID").asLong() : null);
        record.setName(getText(node, "name", null));
        record.setType(getText(node, "type", null));
        record.setSize(node.has("size") ? node.get("size").asLong() : 0L);
        record.setCategory(getText(node, "category", null));
        record.setUploadDate(parseUploadDate(getText(node, "upload_date", null)));

        if (includeData && node.has("data") && !node.get("data").isNull()) {
            record.setData(extractBytes(node.get("data")));
        }

        return record;
    }

    private byte[] extractBytes(JsonNode node) {
        try {
            if (node.isObject()) {
                JsonNode binary = node.get("$binary");
                if (binary != null) {
                    if (binary.isTextual()) {
                        return Base64.getDecoder().decode(binary.asText());
                    }
                    if (binary.isObject()) {
                        JsonNode base64Node = binary.get("base64");
                        if (base64Node != null && base64Node.isTextual()) {
                            return Base64.getDecoder().decode(base64Node.asText());
                        }
                    }
                }

                JsonNode dataNode = node.get("data");
                if (dataNode != null && dataNode.isTextual()) {
                    return Base64.getDecoder().decode(dataNode.asText());
                }
                if (dataNode != null && dataNode.isObject()) {
                    JsonNode base64Node = dataNode.get("base64");
                    if (base64Node != null && base64Node.isTextual()) {
                        return Base64.getDecoder().decode(base64Node.asText());
                    }
                }
            }
            if (node.isBinary()) {
                return node.binaryValue();
            }
            if (node.isTextual()) {
                return Base64.getDecoder().decode(node.asText());
            }
            if (node.isArray()) {
                byte[] bytes = new byte[node.size()];
                for (int i = 0; i < node.size(); i++) {
                    bytes[i] = (byte) node.get(i).asInt();
                }
                return bytes;
            }
        } catch (Exception e) {
            log.warn("Failed to parse data bytes", e);
        }
        return null;
    }
}
