package com.hospitalfinder.backend.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.entity.ChatSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final DataStoreService dataStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<ChatSession> findBySessionId(String sessionId) {
        try {
            JsonNode result = dataStoreService.findByField("chat_sessions", "session_id", sessionId);
            if (result != null) {
                return Optional.of(mapToChatSession(result));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error finding chat session by sessionId: {}", sessionId, e);
            return Optional.empty();
        }
    }

    public Optional<ChatSession> findLatest() {
        try {
            JsonNode result = dataStoreService.executeQuery("SELECT * FROM chat_sessions");
            if (result != null && result.isArray() && result.size() > 0) {
                // Find the most recent by created_at
                JsonNode latest = null;
                String latestTime = "";
                for (JsonNode node : result) {
                    JsonNode data = node.has("chat_sessions") ? node.get("chat_sessions") : node;
                    String createdAt = data.has("created_at") && !data.get("created_at").isNull()
                            ? data.get("created_at").asText()
                            : "";
                    if (createdAt.compareTo(latestTime) > 0) {
                        latestTime = createdAt;
                        latest = data;
                    }
                }
                if (latest != null) {
                    return Optional.of(mapToChatSession(latest));
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error finding latest chat session", e);
            return Optional.empty();
        }
    }

    public ChatSession save(ChatSession session) {
        session.setUpdatedAt(LocalDateTime.now());

        try {
            // If session has a sessionId, try to find existing
            if (session.getSessionId() != null) {
                JsonNode existing = dataStoreService.findByField("chat_sessions", "session_id", session.getSessionId());
                if (existing != null) {
                    // Update existing session
                    Long rowId = existing.has("ROWID") ? existing.get("ROWID").asLong()
                            : (existing.has("_id") ? existing.get("_id").asLong() : null);
                    if (rowId != null) {
                        Map<String, Object> updates = sessionToMap(session);
                        dataStoreService.updateRecord("chat_sessions", rowId, updates);
                        log.debug("Updated chat session: {}", session.getSessionId());
                        return session;
                    }
                }
            }

            // Insert new session
            if (session.getSessionId() == null) {
                session.setSessionId(UUID.randomUUID().toString());
            }
            if (session.getCreatedAt() == null) {
                session.setCreatedAt(LocalDateTime.now());
            }

            Map<String, Object> values = sessionToMap(session);
            dataStoreService.insertRecord("chat_sessions", values);
            log.debug("Created new chat session: {}", session.getSessionId());
            return session;
        } catch (Exception e) {
            log.error("Failed to save chat session", e);
            throw new RuntimeException("Failed to save chat session", e);
        }
    }

    // ── Private Helpers ──────────────────────────────────────────

    private Map<String, Object> sessionToMap(ChatSession session) {
        Map<String, Object> map = new HashMap<>();
        map.put("session_id", session.getSessionId());
        if (session.getUserId() != null)
            map.put("user_id", session.getUserId());
        if (session.getCurrentStep() != null)
            map.put("current_step", session.getCurrentStep());
        if (session.getSymptom() != null)
            map.put("symptom", session.getSymptom());
        if (session.getSpecialization() != null)
            map.put("specialization", session.getSpecialization());
        if (session.getClinicId() != null)
            map.put("clinic_id", session.getClinicId());
        if (session.getClinicName() != null)
            map.put("clinic_name", session.getClinicName());
        if (session.getDoctorId() != null)
            map.put("doctor_id", session.getDoctorId());
        if (session.getDoctorName() != null)
            map.put("doctor_name", session.getDoctorName());
        if (session.getSelectedDate() != null)
            map.put("selected_date", session.getSelectedDate());
        if (session.getSelectedTime() != null)
            map.put("selected_time", session.getSelectedTime());
        if (session.getPatientName() != null)
            map.put("patient_name", session.getPatientName());
        if (session.getPatientAge() != null)
            map.put("patient_age", session.getPatientAge());
        if (session.getPatientGender() != null)
            map.put("patient_gender", session.getPatientGender());
        if (session.getPatientPhone() != null)
            map.put("patient_phone", session.getPatientPhone());
        if (session.getPatientEmail() != null)
            map.put("patient_email", session.getPatientEmail());
        if (session.getCreatedAt() != null)
            map.put("created_at", session.getCreatedAt().toString());
        if (session.getUpdatedAt() != null)
            map.put("updated_at", session.getUpdatedAt().toString());
        if (session.getExpiresAt() != null)
            map.put("expires_at", session.getExpiresAt().toString());
        return map;
    }

    private ChatSession mapToChatSession(JsonNode node) {
        ChatSession session = new ChatSession();
        session.setId(getTextOrNull(node, "_id"));
        if (session.getId() == null)
            session.setId(getTextOrNull(node, "ROWID"));
        session.setSessionId(getTextOrNull(node, "session_id"));
        session.setUserId(getTextOrNull(node, "user_id"));
        session.setCurrentStep(getTextOrNull(node, "current_step"));
        session.setSymptom(getTextOrNull(node, "symptom"));
        session.setSpecialization(getTextOrNull(node, "specialization"));
        session.setClinicId(getTextOrNull(node, "clinic_id"));
        session.setClinicName(getTextOrNull(node, "clinic_name"));
        session.setDoctorId(getTextOrNull(node, "doctor_id"));
        session.setDoctorName(getTextOrNull(node, "doctor_name"));
        session.setSelectedDate(getTextOrNull(node, "selected_date"));
        session.setSelectedTime(getTextOrNull(node, "selected_time"));
        session.setPatientName(getTextOrNull(node, "patient_name"));
        session.setPatientGender(getTextOrNull(node, "patient_gender"));
        session.setPatientPhone(getTextOrNull(node, "patient_phone"));
        session.setPatientEmail(getTextOrNull(node, "patient_email"));

        if (node.has("patient_age") && !node.get("patient_age").isNull()) {
            session.setPatientAge(node.get("patient_age").asInt());
        }

        String createdAt = getTextOrNull(node, "created_at");
        if (createdAt != null) {
            try {
                session.setCreatedAt(LocalDateTime.parse(createdAt));
            } catch (Exception ignored) {
            }
        }
        String updatedAt = getTextOrNull(node, "updated_at");
        if (updatedAt != null) {
            try {
                session.setUpdatedAt(LocalDateTime.parse(updatedAt));
            } catch (Exception ignored) {
            }
        }
        String expiresAt = getTextOrNull(node, "expires_at");
        if (expiresAt != null) {
            try {
                session.setExpiresAt(LocalDateTime.parse(expiresAt));
            } catch (Exception ignored) {
            }
        }

        return session;
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }
}
