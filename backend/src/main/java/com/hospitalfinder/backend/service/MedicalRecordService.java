package com.hospitalfinder.backend.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.entity.MedicalRecord;
import com.hospitalfinder.backend.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordService {

    private static final DateTimeFormatter DATASTORE_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserStoreService userStoreService;
    private final DataStoreService dataStoreService;

    public MedicalRecord uploadFile(MultipartFile file, String category, Long userId) throws IOException {
        UserData userData = resolveUploadUser(userId);
        if (userData == null || userData.getId() == null) {
            throw new IOException("Unable to resolve valid user for medical record upload");
        }

        // Store metadata and file payload using schema-fallback mappings.

        String safeName = truncate(file.getOriginalFilename(), 240);
        String safeType = truncate(file.getContentType(), 100);
        String safeCategory = truncate(category, 50);
        String now = LocalDateTime.now().format(DATASTORE_DATETIME_FORMAT);
        String encodedData = Base64.getEncoder().encodeToString(file.getBytes());

        Map<String, Object> values = new HashMap<>();
        values.put("name", safeName);
        values.put("type", safeType);
        values.put("size", file.getSize());
        values.put("category", safeCategory);
        values.put("upload_date", now);
        values.put("user_id", userData.getId());
        values.put("data", encodedData);

        try {
            JsonNode result = insertMedicalRecordWithFallback(values, userData.getId(), safeName, safeType, safeCategory,
                now, file.getSize(), encodedData);
            MedicalRecord record = new MedicalRecord();
            record.setId(extractId(result));
            record.setName(getText(result, "name", getText(result, "file_name", safeName)));
            record.setType(getText(result, "type", getText(result, "file_type", safeType)));
            record.setSize(result.has("size") ? result.get("size").asLong()
                : result.has("file_size") ? result.get("file_size").asLong() : file.getSize());
            record.setCategory(getText(result, "category", getText(result, "record_category", safeCategory)));
            record.setUploadDate(
                parseUploadDate(result.has("upload_date") ? result.get("upload_date").asText()
                    : result.has("created_at") ? result.get("created_at").asText() : null));

            // Manually link user for return
            User user = new User();
            user.setId(userData.getId());
            user.setName(userData.getName());
            user.setEmail(userData.getEmail());
            record.setUser(user);

            return record;
        } catch (Exception e) {
            log.error("Failed to upload medical record", e);
            throw new IOException("Failed to save record metadata: " + e.getMessage(), e);
        }
    }

    public List<MedicalRecord> getRecordsByUserId(Long userId) {
        try {
            Long effectiveUserId = resolveUserIdForQuery(userId);
            if (effectiveUserId == null) {
                return new ArrayList<>();
            }

            Map<String, MedicalRecord> byId = new LinkedHashMap<>();
            JsonNode quotedResult = dataStoreService
                    .executeQuery("SELECT * FROM medical_records WHERE user_id = '" + effectiveUserId + "'");
            collectRecords(quotedResult, byId, false);

            JsonNode unquotedResult = dataStoreService
                    .executeQuery("SELECT * FROM medical_records WHERE user_id = " + effectiveUserId);
            collectRecords(unquotedResult, byId, false);

            return new ArrayList<>(byId.values());
        } catch (Exception e) {
            log.error("Error fetching medical records", e);
            return new ArrayList<>();
        }
    }

    public Optional<MedicalRecord> getRecordById(Long id) {
        try {
            JsonNode data = dataStoreService.findById("medical_records", id);
            if (data != null && !data.isNull()) {
                JsonNode node = data.has("medical_records") ? data.get("medical_records") : data;
                MedicalRecord record = mapRecordFromNode(node, true);
                if (record != null) {
                    return Optional.of(record);
                }
            }

            // Fallback 1: ZCQL by ROWID
            JsonNode byRowId = dataStoreService.executeQuery("SELECT * FROM medical_records WHERE ROWID = '" + id + "'");
            MedicalRecord record = extractFirstRecord(byRowId, true);
            if (record != null) {
                return Optional.of(record);
            }

            // Fallback 2: Some schemas keep custom id column
            JsonNode byCustomId = dataStoreService.executeQuery("SELECT * FROM medical_records WHERE id = '" + id + "'");
            MedicalRecord customIdRecord = extractFirstRecord(byCustomId, true);
            return customIdRecord == null ? Optional.empty() : Optional.of(customIdRecord);
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
            try {
                return LocalDateTime.parse(value, DATASTORE_DATETIME_FORMAT);
            } catch (Exception ex) {
                log.warn("Failed to parse upload_date: {}", value, ex);
                return LocalDateTime.now();
            }
        }
    }

    private UserData resolveUploadUser(Long requestUserId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName() != null
                    && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
                UserData authUser = userStoreService.findByEmail(authentication.getName());
                if (authUser != null && authUser.getId() != null) {
                    return authUser;
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve upload user from authentication context", e);
        }

        if (requestUserId != null) {
            try {
                return userStoreService.findById(requestUserId);
            } catch (Exception e) {
                log.warn("User lookup failed for id {}", requestUserId, e);
            }
        }
        return null;
    }

    private Long resolveUserIdForQuery(Long requestUserId) {
        UserData userData = resolveUploadUser(requestUserId);
        return userData != null ? userData.getId() : null;
    }

    private Long extractId(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.has("id") && !node.get("id").isNull()) {
            return node.get("id").asLong();
        }
        if (node.has("ROWID") && !node.get("ROWID").isNull()) {
            return node.get("ROWID").asLong();
        }
        return null;
    }

    private MedicalRecord mapRecordFromNode(JsonNode node, boolean includeData) {
        if (node == null || node.isNull()) {
            return null;
        }
        MedicalRecord record = new MedicalRecord();
        record.setId(extractId(node));
        record.setName(getText(node, "name", getText(node, "file_name", getText(node, "report_name",
                "Report-" + (record.getId() != null ? record.getId() : "unknown")))));
        record.setType(getText(node, "type", getText(node, "file_type", getText(node, "mime_type",
                "application/octet-stream"))));
        record.setSize(node.has("size") ? node.get("size").asLong()
            : node.has("file_size") ? node.get("file_size").asLong() : 0L);
        record.setCategory(getText(node, "category", getText(node, "record_category", "Diagnostics")));
        record.setUploadDate(parseUploadDate(getText(node, "upload_date", getText(node, "created_at", null))));

        if (includeData && node.has("data") && !node.get("data").isNull()) {
            record.setData(extractBytes(node.get("data")));
        } else if (includeData && node.has("file_data") && !node.get("file_data").isNull()) {
            record.setData(extractBytes(node.get("file_data")));
        } else if (includeData && node.has("document_data") && !node.get("document_data").isNull()) {
            record.setData(extractBytes(node.get("document_data")));
        } else if (includeData && node.has("report_data") && !node.get("report_data").isNull()) {
            record.setData(extractBytes(node.get("report_data")));
        } else if (includeData && node.has("content") && !node.get("content").isNull()) {
            record.setData(extractBytes(node.get("content")));
        } else if (includeData && node.has("document") && !node.get("document").isNull()) {
            record.setData(extractBytes(node.get("document")));
        } else if (includeData && node.has("base64_data") && !node.get("base64_data").isNull()) {
            record.setData(extractBytes(node.get("base64_data")));
        }

        return record;
    }

    private void collectRecords(JsonNode result, Map<String, MedicalRecord> byId, boolean includeData) {
        if (result == null || !result.isArray()) {
            return;
        }
        for (JsonNode node : result) {
            JsonNode data = node.has("medical_records") ? node.get("medical_records") : node;
            MedicalRecord record = mapRecordFromNode(data, includeData);
            if (record == null) {
                continue;
            }
            String key = record.getId() != null ? String.valueOf(record.getId()) : String.valueOf(byId.size() + 1);
            byId.putIfAbsent(key, record);
        }
    }

    private MedicalRecord extractFirstRecord(JsonNode queryResult, boolean includeData) {
        if (queryResult == null || !queryResult.isArray() || queryResult.isEmpty()) {
            return null;
        }
        JsonNode first = queryResult.get(0);
        JsonNode data = first.has("medical_records") ? first.get("medical_records") : first;
        return mapRecordFromNode(data, includeData);
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

    private JsonNode insertMedicalRecordWithFallback(Map<String, Object> primaryValues, Long userId, String name,
            String type, String category, String uploadDate, long fileSize, String encodedData) {
        List<Map<String, Object>> attempts = new ArrayList<>();
        attempts.add(primaryValues);

        Map<String, Object> altColumns = new HashMap<>();
        altColumns.put("file_name", name);
        altColumns.put("file_type", type);
        altColumns.put("file_size", fileSize);
        altColumns.put("record_category", category);
        altColumns.put("created_at", uploadDate);
        altColumns.put("user_id", userId);
        altColumns.put("data", encodedData);
        attempts.add(altColumns);

        Map<String, Object> reportColumns = new HashMap<>();
        reportColumns.put("report_name", name);
        reportColumns.put("mime_type", type);
        reportColumns.put("file_size", fileSize);
        reportColumns.put("category", category);
        reportColumns.put("created_at", uploadDate);
        reportColumns.put("user_id", userId);
        reportColumns.put("data", encodedData);
        attempts.add(reportColumns);

        Map<String, Object> fileDataColumns = new HashMap<>();
        fileDataColumns.put("name", name);
        fileDataColumns.put("type", type);
        fileDataColumns.put("size", fileSize);
        fileDataColumns.put("category", category);
        fileDataColumns.put("upload_date", uploadDate);
        fileDataColumns.put("user_id", userId);
        fileDataColumns.put("file_data", encodedData);
        attempts.add(fileDataColumns);

        Map<String, Object> documentDataColumns = new HashMap<>();
        documentDataColumns.put("name", name);
        documentDataColumns.put("type", type);
        documentDataColumns.put("size", fileSize);
        documentDataColumns.put("category", category);
        documentDataColumns.put("upload_date", uploadDate);
        documentDataColumns.put("user_id", userId);
        documentDataColumns.put("document_data", encodedData);
        attempts.add(documentDataColumns);

        Map<String, Object> reportDataColumns = new HashMap<>();
        reportDataColumns.put("report_name", name);
        reportDataColumns.put("mime_type", type);
        reportDataColumns.put("file_size", fileSize);
        reportDataColumns.put("category", category);
        reportDataColumns.put("created_at", uploadDate);
        reportDataColumns.put("user_id", userId);
        reportDataColumns.put("report_data", encodedData);
        attempts.add(reportDataColumns);

        Map<String, Object> contentColumns = new HashMap<>();
        contentColumns.put("name", name);
        contentColumns.put("type", type);
        contentColumns.put("size", fileSize);
        contentColumns.put("category", category);
        contentColumns.put("upload_date", uploadDate);
        contentColumns.put("user_id", userId);
        contentColumns.put("content", encodedData);
        attempts.add(contentColumns);

        Map<String, Object> base64DataColumns = new HashMap<>();
        base64DataColumns.put("name", name);
        base64DataColumns.put("type", type);
        base64DataColumns.put("size", fileSize);
        base64DataColumns.put("category", category);
        base64DataColumns.put("upload_date", uploadDate);
        base64DataColumns.put("user_id", userId);
        base64DataColumns.put("base64_data", encodedData);
        attempts.add(base64DataColumns);

        Map<String, Object> metadataOnly = new HashMap<>();
        metadataOnly.put("name", name);
        metadataOnly.put("type", type);
        metadataOnly.put("size", fileSize);
        metadataOnly.put("category", category);
        metadataOnly.put("upload_date", uploadDate);
        metadataOnly.put("user_id", userId);
        attempts.add(metadataOnly);

        Map<String, Object> metadataOnlyAlt = new HashMap<>();
        metadataOnlyAlt.put("file_name", name);
        metadataOnlyAlt.put("file_type", type);
        metadataOnlyAlt.put("file_size", fileSize);
        metadataOnlyAlt.put("record_category", category);
        metadataOnlyAlt.put("created_at", uploadDate);
        metadataOnlyAlt.put("user_id", userId);
        attempts.add(metadataOnlyAlt);

        Map<String, Object> minimalWithName = new HashMap<>();
        minimalWithName.put("user_id", userId);
        minimalWithName.put("name", name);
        attempts.add(minimalWithName);

        Map<String, Object> minimalWithFileName = new HashMap<>();
        minimalWithFileName.put("user_id", userId);
        minimalWithFileName.put("file_name", name);
        attempts.add(minimalWithFileName);

        Map<String, Object> minimalUserOnly = new HashMap<>();
        minimalUserOnly.put("user_id", userId);
        attempts.add(minimalUserOnly);

        RuntimeException lastError = null;
        for (Map<String, Object> attempt : attempts) {
            try {
                return dataStoreService.insertRecord("medical_records", attempt);
            } catch (RuntimeException ex) {
                lastError = ex;
                if (!isSchemaInputError(ex)) {
                    throw ex;
                }
                log.warn("medical_records insert attempt failed with schema/input mismatch. keys={}, error={}",
                        attempt.keySet(), ex.getMessage());
            }
        }

        if (lastError != null) {
            throw new RuntimeException("Failed to insert into medical_records after trying multiple schema mappings: "
                    + lastError.getMessage(), lastError);
        }
        throw new RuntimeException("Failed to insert into medical_records");
    }

    private boolean isSchemaInputError(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("invalid_input") || lower.contains("invalid input value for column name");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
