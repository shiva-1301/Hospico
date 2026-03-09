package com.hospitalfinder.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospitalfinder.backend.entity.DoctorLeaveRequest;
import com.hospitalfinder.backend.entity.LeaveStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorLeaveService {

    private static final DateTimeFormatter DATASTORE_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataStoreService dataStoreService;

    public DoctorLeaveRequest createLeave(DoctorLeaveRequest req) {
        Map<String, Object> values = new HashMap<>();
        values.put("doctor_id", req.getDoctorId());
        values.put("start_date", req.getStartDate().toString());
        values.put("end_date", req.getEndDate().toString());
        values.put("reason", req.getReason());
        values.put("status", LeaveStatus.PENDING.name());
        values.put("created_at", LocalDateTime.now().format(DATASTORE_DATETIME_FORMAT));

        JsonNode created = dataStoreService.insertRecord("doctor_leave_requests", values);
        if (created != null && created.has("ROWID")) {
            req.setId(created.get("ROWID").asLong());
        }
        req.setStatus(LeaveStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());
        return req;
    }

    public List<DoctorLeaveRequest> getByDoctorId(String doctorId) {
        return fetchLeaves("SELECT * FROM doctor_leave_requests WHERE doctor_id = '" + doctorId + "'");
    }

    public List<DoctorLeaveRequest> getByStatus(LeaveStatus status) {
        return fetchLeaves("SELECT * FROM doctor_leave_requests WHERE status = '" + status.name() + "'");
    }

    public DoctorLeaveRequest updateStatus(Long id, LeaveStatus status) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status.name());
        dataStoreService.updateRecord("doctor_leave_requests", id, data);

        List<DoctorLeaveRequest> result = fetchLeaves(
                "SELECT * FROM doctor_leave_requests WHERE ROWID = '" + id + "'");
        return result.stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Leave request not found: " + id));
    }

    // ── Private helpers ──────────────────────────────────────────

    private List<DoctorLeaveRequest> fetchLeaves(String query) {
        List<DoctorLeaveRequest> list = new ArrayList<>();
        try {
            JsonNode result = dataStoreService.executeQuery(query);
            if (result != null && result.isArray()) {
                for (JsonNode node : result) {
                    JsonNode data = node.has("doctor_leave_requests") ? node.get("doctor_leave_requests") : node;
                    list.add(mapToLeave(data));
                }
            }
        } catch (Exception e) {
            log.error("Error fetching leave requests", e);
        }
        return list;
    }

    private DoctorLeaveRequest mapToLeave(JsonNode node) {
        DoctorLeaveRequest r = new DoctorLeaveRequest();
        String rowId = getText(node, "ROWID");
        if (rowId == null) rowId = getText(node, "id");
        if (rowId != null) {
            try { r.setId(Long.parseLong(rowId)); } catch (NumberFormatException ignored) {}
        }
        r.setDoctorId(getText(node, "doctor_id"));
        r.setReason(getText(node, "reason"));

        String startDate = getText(node, "start_date");
        if (startDate != null) {
            try { r.setStartDate(java.time.LocalDate.parse(startDate.substring(0, 10))); } catch (Exception ignored) {}
        }
        String endDate = getText(node, "end_date");
        if (endDate != null) {
            try { r.setEndDate(java.time.LocalDate.parse(endDate.substring(0, 10))); } catch (Exception ignored) {}
        }

        String status = getText(node, "status");
        if (status != null) {
            try { r.setStatus(LeaveStatus.valueOf(status)); } catch (Exception e) { r.setStatus(LeaveStatus.PENDING); }
        }

        String createdAt = getText(node, "created_at");
        if (createdAt != null) {
            try { r.setCreatedAt(LocalDateTime.parse(createdAt, DATASTORE_DATETIME_FORMAT)); } catch (Exception ignored) {}
        }
        return r;
    }

    private String getText(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }
}
