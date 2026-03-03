package com.hospitalfinder.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospitalfinder.backend.dto.AppointmentRequestDTO;
import com.hospitalfinder.backend.dto.AppointmentResponseDTO;
import com.hospitalfinder.backend.dto.ClinicResponseDTO;
import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.entity.Appointment;
import com.hospitalfinder.backend.entity.Doctor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

        private static final DateTimeFormatter DATASTORE_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        private final DataStoreService dataStoreService;
        private final UserStoreService userStoreService;
        private final ClinicService clinicService;
        private final DoctorService doctorService;

        public AppointmentResponseDTO book(AppointmentRequestDTO dto) {
                // Validate user and resolve FK-safe user ROWID
                UserData userData = resolveBookingUser(dto.getUserId());
                if (userData == null) {
                        throw new RuntimeException("User not found");
                }

                // Validate clinic exists
                ClinicResponseDTO clinicDTO = clinicService.getClinicById(dto.getClinicId());
                if (clinicDTO == null) {
                        throw new RuntimeException("Clinic not found");
                }

                // Validate doctor exists
                Doctor doctor = doctorService.findById(dto.getDoctorId());
                if (doctor == null) {
                        throw new RuntimeException("Doctor not found");
                }

                LocalDateTime appointmentTime = LocalDateTime.parse(dto.getAppointmentTime());
                String doctorId = String.valueOf(dto.getDoctorId());

                // Check for time slot conflict
                if (isTimeSlotBooked(doctorId, appointmentTime)) {
                        throw new RuntimeException("Time slot already booked");
                }

                // Insert into CloudScale Data Store using actual table columns
                Map<String, Object> values = new HashMap<>();
                values.put("user_id", userData.getId());
                values.put("clinic_id", dto.getClinicId());
                values.put("doctor_id", dto.getDoctorId());
                values.put("appointment_date", appointmentTime.toLocalDate().toString());
                values.put("time_slot", appointmentTime.toLocalTime().withSecond(0).withNano(0).toString());
                values.put("status", "BOOKED");
                values.put("notes", dto.getReason());
                values.put("created_at", LocalDateTime.now().format(DATASTORE_DATETIME_FORMAT));

                JsonNode createdNode = dataStoreService.insertRecord("appointments", values);
                String appointmentId = extractAppointmentId(createdNode);
                log.info("✅ Appointment saved with ROWID: {}", appointmentId);

                // Build response
                Appointment saved = new Appointment();
                saved.setId(appointmentId);
                saved.setUserId(String.valueOf(userData.getId()));
                saved.setClinicId(String.valueOf(dto.getClinicId()));
                saved.setDoctorId(doctorId);
                saved.setAppointmentTime(appointmentTime);
                saved.setStatus("BOOKED");
                saved.setPatientName(dto.getPatientName());
                saved.setPatientAge(dto.getPatientAge());
                saved.setPatientGender(dto.getPatientGender());
                saved.setPatientPhone(dto.getPatientPhone());
                saved.setPatientEmail(dto.getPatientEmail());
                saved.setReason(dto.getReason());

                return buildAppointmentResponse(saved);
        }

        private UserData resolveBookingUser(Long requestUserId) {
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
                        log.warn("Could not resolve booking user from authentication context", e);
                }

                if (requestUserId != null) {
                        return userStoreService.findById(requestUserId);
                }
                return null;
        }

        public List<AppointmentResponseDTO> getAppointmentsByUser(Long userId) {
                Long resolvedUserId = resolveAuthenticatedUserId();
                Long effectiveUserId = resolvedUserId != null ? resolvedUserId : userId;

                if (effectiveUserId == null) {
                        return new ArrayList<>();
                }

                List<Appointment> appointments = fetchAppointments(
                                "SELECT * FROM appointments WHERE user_id = '" + effectiveUserId + "'");

                if (appointments.isEmpty()) {
                        appointments = fetchAppointments(
                                        "SELECT * FROM appointments WHERE user_id = " + effectiveUserId);
                }

                if (appointments.isEmpty() && userId != null && !userId.equals(effectiveUserId)) {
                        appointments = fetchAppointments(
                                        "SELECT * FROM appointments WHERE user_id = '" + userId + "'");
                }

                Map<String, Appointment> uniqueById = new LinkedHashMap<>();
                for (Appointment appointment : appointments) {
                        if (appointment.getId() != null) {
                                uniqueById.putIfAbsent(appointment.getId(), appointment);
                        }
                }

                return uniqueById.values().stream()
                                .map(this::buildAppointmentResponse)
                                .collect(Collectors.toList());
        }

        private Long resolveAuthenticatedUserId() {
                try {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        if (authentication != null && authentication.getName() != null
                                        && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
                                UserData authUser = userStoreService.findByEmail(authentication.getName());
                                if (authUser != null) {
                                        return authUser.getId();
                                }
                        }
                } catch (Exception e) {
                        log.warn("Could not resolve authenticated user id for appointment lookup", e);
                }
                return null;
        }

        public List<AppointmentResponseDTO> getAppointmentsByClinic(Long clinicId) {
                // Fetch all, filter by clinic_id client-side
                List<Appointment> all = fetchAppointments("SELECT * FROM appointments");
                return all.stream()
                                .filter(a -> String.valueOf(clinicId).equals(a.getClinicId()))
                                .map(this::buildAppointmentResponse)
                                .collect(Collectors.toList());
        }

        public List<AppointmentResponseDTO> getAppointmentsByDoctorAndDate(Long doctorId, LocalDate date) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

                // Fetch all, filter by doctor + date range client-side
                List<Appointment> all = fetchAppointments("SELECT * FROM appointments");
                return all.stream()
                                .filter(a -> String.valueOf(doctorId).equals(a.getDoctorId()))
                                .filter(a -> a.getAppointmentTime() != null
                                                && !a.getAppointmentTime().isBefore(startOfDay)
                                                && a.getAppointmentTime().isBefore(endOfDay)
                                                && "BOOKED".equals(a.getStatus()))
                                .map(this::buildAppointmentResponse)
                                .collect(Collectors.toList());
        }

        public void deleteAppointment(String id) {
                try {
                        dataStoreService.executeQuery("DELETE FROM appointments WHERE ROWID = '" + id + "'");
                } catch (Exception e) {
                        log.error("Failed to delete appointment {}", id, e);
                        throw new RuntimeException("Failed to delete appointment", e);
                }
        }

        public AppointmentResponseDTO updateStatus(String id, String status) {
                try {
                        Map<String, Object> data = new HashMap<>();
                        data.put("status", status);
                        dataStoreService.updateRecord("appointments", Long.parseLong(id), data);
                        return fetchAppointments("SELECT * FROM appointments WHERE ROWID = '" + id + "'").stream()
                                        .map(this::buildAppointmentResponse)
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException("Appointment not found after update"));
                } catch (Exception e) {
                        log.error("Failed to update appointment status {}", id, e);
                        throw new RuntimeException("Failed to update appointment status", e);
                }
        }

        // ── Private Helpers ──────────────────────────────────────────

        private boolean isTimeSlotBooked(String doctorId, LocalDateTime appointmentTime) {
                List<Appointment> all = fetchAppointments("SELECT * FROM appointments");
                return all.stream()
                                .anyMatch(a -> doctorId.equals(a.getDoctorId())
                                                && appointmentTime.equals(a.getAppointmentTime())
                                                && "BOOKED".equals(a.getStatus()));
        }

        private List<Appointment> fetchAppointments(String query) {
                try {
                        JsonNode result = dataStoreService.executeQuery(query);
                        List<Appointment> appointments = new ArrayList<>();
                        if (result != null && result.isArray()) {
                                for (JsonNode node : result) {
                                        JsonNode data = node.has("appointments") ? node.get("appointments") : node;
                                        appointments.add(mapToAppointment(data));
                                }
                        }
                        return appointments;
                } catch (Exception e) {
                        log.error("Error fetching appointments", e);
                        return new ArrayList<>();
                }
        }

        private Appointment mapToAppointment(JsonNode node) {
                Appointment a = new Appointment();
                String id = getTextOrNull(node, "id");
                if (id == null) {
                        id = getTextOrNull(node, "ROWID");
                }
                a.setId(id);
                a.setUserId(getTextOrNull(node, "user_id"));
                a.setClinicId(getTextOrNull(node, "clinic_id"));
                a.setDoctorId(getTextOrNull(node, "doctor_id"));
                a.setStatus(getTextOrNull(node, "status"));
                a.setReason(getTextOrNull(node, "notes"));

                String appointmentDate = getTextOrNull(node, "appointment_date");
                String timeSlot = getTextOrNull(node, "time_slot");
                if (appointmentDate != null && timeSlot != null) {
                        try {
                                LocalDate date = LocalDate.parse(appointmentDate);
                                LocalTime time = LocalTime.parse(timeSlot.length() == 5 ? timeSlot + ":00" : timeSlot);
                                a.setAppointmentTime(LocalDateTime.of(date, time));
                        } catch (Exception e) {
                                log.warn("Could not parse appointment_date/time_slot: {}/{}", appointmentDate,
                                                timeSlot);
                        }
                }
                return a;
        }

        private String extractAppointmentId(JsonNode node) {
                if (node == null || node.isNull()) {
                        return null;
                }
                if (node.has("ROWID") && !node.get("ROWID").isNull()) {
                        return node.get("ROWID").asText();
                }
                if (node.has("id") && !node.get("id").isNull()) {
                        return node.get("id").asText();
                }
                return null;
        }

        private String getTextOrNull(JsonNode node, String field) {
                return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
        }

        private AppointmentResponseDTO buildAppointmentResponse(Appointment appointment) {
                AppointmentResponseDTO dto = new AppointmentResponseDTO();
                dto.setId(appointment.getId());
                dto.setAppointmentTime(appointment.getAppointmentTime() != null
                                ? appointment.getAppointmentTime().toString()
                                : null);
                dto.setStatus(appointment.getStatus());
                dto.setPatientName(appointment.getPatientName());
                dto.setPatientAge(appointment.getPatientAge());
                dto.setPatientGender(appointment.getPatientGender());
                dto.setPatientPhone(appointment.getPatientPhone());
                dto.setPatientEmail(appointment.getPatientEmail());
                dto.setReason(appointment.getReason());

                // Enrich with user details
                if (appointment.getUserId() != null) {
                        dto.setUserId(appointment.getUserId());
                        try {
                                UserData userData = userStoreService.findById(Long.parseLong(appointment.getUserId()));
                                if (userData != null) {
                                        dto.setUserName(userData.getName());
                                        if (dto.getPatientName() == null || dto.getPatientName().isBlank()) {
                                                dto.setPatientName(userData.getName());
                                        }
                                        if (dto.getPatientEmail() == null || dto.getPatientEmail().isBlank()) {
                                                dto.setPatientEmail(userData.getEmail());
                                        }
                                        if (dto.getPatientPhone() == null || dto.getPatientPhone().isBlank()) {
                                                dto.setPatientPhone(userData.getPhone());
                                        }
                                        if (dto.getPatientAge() == null) {
                                                dto.setPatientAge(userData.getAge());
                                        }
                                        if (dto.getPatientGender() == null || dto.getPatientGender().isBlank()) {
                                                dto.setPatientGender(userData.getGender());
                                        }
                                }
                        } catch (Exception e) {
                                log.warn("Could not fetch user {}", appointment.getUserId(), e);
                        }
                }

                // Enrich with clinic details
                if (appointment.getClinicId() != null) {
                        dto.setClinicId(appointment.getClinicId());
                        try {
                                ClinicResponseDTO clinicDTO = clinicService.getClinicById(Long.parseLong(appointment.getClinicId()));
                                if (clinicDTO != null) {
                                        dto.setClinicName(clinicDTO.getName());
                                }
                        } catch (Exception e) {
                                log.warn("Could not fetch clinic {}", appointment.getClinicId(), e);
                        }
                }

                // Enrich with doctor details
                if (appointment.getDoctorId() != null) {
                        dto.setDoctorId(appointment.getDoctorId());
                        try {
                                Doctor doctor = doctorService.findById(Long.parseLong(appointment.getDoctorId()));
                                if (doctor != null) {
                                        dto.setDoctorName(doctor.getName());
                                        dto.setDoctorSpecialization(doctor.getSpecialization());
                                }
                        } catch (Exception e) {
                                log.warn("Could not fetch doctor {}", appointment.getDoctorId(), e);
                        }
                }

                return dto;
        }
}
