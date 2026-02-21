package com.hospitalfinder.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        private final DataStoreService dataStoreService;
        private final UserStoreService userStoreService;
        private final ClinicService clinicService;
        private final DoctorService doctorService;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public AppointmentResponseDTO book(AppointmentRequestDTO dto) {
                // Validate user exists
                UserData userData = userStoreService.findById(dto.getUserId());
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

                // Generate unique ID
                String appointmentId = UUID.randomUUID().toString();

                // Insert into CloudScale NoSQL
                Map<String, Object> values = new HashMap<>();
                values.put("id", appointmentId);
                values.put("user_id", String.valueOf(dto.getUserId()));
                values.put("clinic_id", String.valueOf(dto.getClinicId()));
                values.put("doctor_id", doctorId);
                values.put("appointment_time", appointmentTime.toString());
                values.put("status", "BOOKED");
                values.put("patient_name", dto.getPatientName());
                values.put("patient_age", dto.getPatientAge());
                values.put("patient_gender", dto.getPatientGender());
                values.put("patient_phone", dto.getPatientPhone());
                values.put("patient_email", dto.getPatientEmail());
                values.put("reason", dto.getReason());

                dataStoreService.insertRecord("appointments", values);
                log.info("✅ Appointment saved with ID: {}", appointmentId);

                // Build response
                Appointment saved = new Appointment();
                saved.setId(appointmentId);
                saved.setUserId(String.valueOf(dto.getUserId()));
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

        public List<AppointmentResponseDTO> getAppointmentsByUser(Long userId) {
                List<Appointment> appointments = fetchAppointments(
                                "SELECT * FROM appointments WHERE user_id = '" + userId + "'");
                return appointments.stream()
                                .map(this::buildAppointmentResponse)
                                .collect(Collectors.toList());
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
                        dataStoreService.executeQuery("DELETE FROM appointments WHERE id = '" + id + "'");
                } catch (Exception e) {
                        log.error("Failed to delete appointment {}", id, e);
                        throw new RuntimeException("Failed to delete appointment", e);
                }
        }

        public AppointmentResponseDTO updateStatus(String id, String status) {
                try {
                        Map<String, Object> data = new HashMap<>();
                        data.put("status", status);
                        // Query the ROWID first because updateRecord needs it
                        JsonNode result = dataStoreService
                                        .executeQuery("SELECT ROWID FROM appointments WHERE id = '" + id + "'");
                        if (result != null && result.isArray() && result.size() > 0) {
                                JsonNode rowNode = result.get(0).has("appointments") ? result.get(0).get("appointments")
                                                : result.get(0);
                                Long rowId = rowNode.get("ROWID").asLong();
                                dataStoreService.updateRecord("appointments", rowId, data);
                        } else {
                                throw new RuntimeException("Appointment not found");
                        }
                        return fetchAppointments("SELECT * FROM appointments WHERE id = '" + id + "'").stream()
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
                a.setId(getTextOrNull(node, "id"));
                a.setUserId(getTextOrNull(node, "user_id"));
                a.setClinicId(getTextOrNull(node, "clinic_id"));
                a.setDoctorId(getTextOrNull(node, "doctor_id"));
                a.setStatus(getTextOrNull(node, "status"));
                a.setPatientName(getTextOrNull(node, "patient_name"));
                a.setPatientGender(getTextOrNull(node, "patient_gender"));
                a.setPatientPhone(getTextOrNull(node, "patient_phone"));
                a.setPatientEmail(getTextOrNull(node, "patient_email"));
                a.setReason(getTextOrNull(node, "reason"));

                if (node.has("patient_age") && !node.get("patient_age").isNull()) {
                        a.setPatientAge(node.get("patient_age").asInt());
                }
                if (node.has("appointment_time") && !node.get("appointment_time").isNull()) {
                        try {
                                a.setAppointmentTime(LocalDateTime.parse(node.get("appointment_time").asText()));
                        } catch (Exception e) {
                                log.warn("Could not parse appointment_time: {}", node.get("appointment_time").asText());
                        }
                }
                return a;
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
                                Long userIdLong = Long.parseLong(appointment.getUserId());
                                UserData userData = userStoreService.findById(userIdLong);
                                if (userData != null) {
                                        dto.setUserName(userData.getName());
                                }
                        } catch (Exception e) {
                                log.warn("Could not fetch user {}", appointment.getUserId(), e);
                        }
                }

                // Enrich with clinic details
                if (appointment.getClinicId() != null) {
                        dto.setClinicId(appointment.getClinicId());
                        try {
                                Long clinicIdLong = Long.parseLong(appointment.getClinicId());
                                ClinicResponseDTO clinicDTO = clinicService.getClinicById(clinicIdLong);
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
                                Long doctorIdLong = Long.parseLong(appointment.getDoctorId());
                                Doctor doctor = doctorService.findById(doctorIdLong);
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
