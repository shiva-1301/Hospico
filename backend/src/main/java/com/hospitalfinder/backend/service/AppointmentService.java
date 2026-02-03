package com.hospitalfinder.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.dto.AppointmentRequestDTO;
import com.hospitalfinder.backend.dto.AppointmentResponseDTO;
import com.hospitalfinder.backend.dto.ClinicResponseDTO;
import com.hospitalfinder.backend.entity.Appointment;
import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.entity.Doctor;
import com.hospitalfinder.backend.entity.User;
import com.hospitalfinder.backend.service.ZohoUserService.UserData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

        private final ZohoUserService zohoUserService;
        private final ClinicService clinicService;
        private final DoctorService doctorService;
        private final ZohoDataStoreService dataStoreService;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public AppointmentResponseDTO book(AppointmentRequestDTO dto) {

                UserData userData = zohoUserService.findById(dto.getUserId());
                if (userData == null) {
                        throw new RuntimeException("User not found");
                }

                ClinicResponseDTO clinicDTO = clinicService.getClinicById(dto.getClinicId());
                Clinic clinic = new Clinic();
                clinic.setId(clinicDTO.getClinicId());
                clinic.setName(clinicDTO.getName());
                clinic.setAddress(clinicDTO.getAddress());
                clinic.setCity(clinicDTO.getCity());

                Doctor doctor = doctorService.findById(dto.getDoctorId());
                if (doctor == null) {
                        throw new RuntimeException("Doctor not found");
                }

                LocalDateTime slot = LocalDateTime.parse(dto.getAppointmentTime());

                if (isSlotBooked(doctor.getId(), slot)) {
                        throw new RuntimeException("Time slot already booked!");
                }

                Map<String, Object> values = new HashMap<>();
                values.put("user_id", userData.getId());
                values.put("clinic_id", clinic.getId());
                values.put("doctor_id", doctor.getId());
                values.put("appointment_time", slot.toString());
                values.put("status", "BOOKED");

                values.put("patient_name", dto.getPatientName());
                values.put("patient_age", dto.getPatientAge());
                values.put("patient_gender", dto.getPatientGender());
                values.put("patient_phone", dto.getPatientPhone());
                values.put("patient_email", dto.getPatientEmail());

                try {
                        JsonNode result = dataStoreService.insertRecord("appointments", values);
                        Appointment appointment = mapToAppointment(result);
                        return new AppointmentResponseDTO(appointment);
                } catch (Exception e) {
                        log.error("Failed to book appointment", e);
                        throw new RuntimeException("Failed to book appointment", e);
                }
        }

        public List<AppointmentResponseDTO> getAppointmentsByUser(Long userId) {
                String query = "SELECT * FROM appointments WHERE user_id = '" + userId + "'";
                return fetchAppointments(query).stream().map(AppointmentResponseDTO::new).collect(Collectors.toList());
        }

        public List<AppointmentResponseDTO> getAppointmentsByClinic(Long clinicId) {
                String query = "SELECT * FROM appointments WHERE clinic_id = '" + clinicId + "'";
                return fetchAppointments(query).stream().map(AppointmentResponseDTO::new).collect(Collectors.toList());
        }

        // For doctor date view
        public List<AppointmentResponseDTO> getAppointmentsByDoctorAndDate(Long doctorId, LocalDate date) {
                // Query all for doctor, then filter by date in memory (simplest due to string
                // date format variations)
                // Or use ZCQL "starts with" or similar if supported.
                String query = "SELECT * FROM appointments WHERE doctor_id = '" + doctorId + "'";
                List<Appointment> all = fetchAppointments(query);

                return all.stream()
                                .filter(a -> a.getAppointmentTime().toLocalDate().equals(date))
                                .filter(a -> "BOOKED".equalsIgnoreCase(a.getStatus()))
                                .map(AppointmentResponseDTO::new)
                                .collect(Collectors.toList());
        }

        public void deleteAppointment(Long id) {
                try {
                        dataStoreService.executeZCQL("DELETE FROM appointments WHERE ROWID = '" + id + "'");
                } catch (Exception e) {
                        log.error("Failed to delete appointment", e);
                        throw new RuntimeException("Failed to delete appointment", e);
                }
        }

        private List<Appointment> fetchAppointments(String query) {
                try {
                        JsonNode result = dataStoreService.executeZCQL(query);
                        List<Appointment> list = new ArrayList<>();
                        if (result != null && result.isArray()) {
                                for (JsonNode node : result) {
                                        JsonNode data = node.has("appointments") ? node.get("appointments") : node;
                                        list.add(mapToAppointment(data));
                                }
                        }
                        return list;
                } catch (Exception e) {
                        log.error("Error fetching appointments", e);
                        return new ArrayList<>();
                }
        }

        private Appointment mapToAppointment(JsonNode node) {
                Appointment apt = objectMapper.convertValue(node, Appointment.class);
                // Manually resolve relationships if they are just IDs in the JSON
                // If json has "user_id" but apt needs User object, we need to set it.
                // POJO might have private User user; but JSON has user_id.
                // We might need to fetch User/Clinic/Doctor to populate fully?
                // For now, assume IDs are enough or we lazy load if critical?
                // Or update POJO to have userId etc fields as well.
                // Let's populate minimal objects logic here

                if (node.has("user_id")) {
                        User u = new User();
                        u.setId(node.get("user_id").asLong());
                        apt.setUser(u);
                }
                if (node.has("clinic_id")) {
                        Clinic c = new Clinic();
                        c.setId(node.get("clinic_id").asLong());
                        apt.setClinic(c);
                }
                if (node.has("doctor_id")) {
                        Doctor d = new Doctor();
                        d.setId(node.get("doctor_id").asLong());
                        apt.setDoctor(d);
                }
                return apt;
        }

        private boolean isSlotBooked(Long doctorId, LocalDateTime slot) {
                String checkQuery = "SELECT ROWID FROM appointments WHERE doctor_id = '" + doctorId
                                + "' AND appointment_time = '" + slot.toString() + "'";
                JsonNode checkResult = dataStoreService.executeZCQL(checkQuery);
                return checkResult != null && checkResult.isArray() && checkResult.size() > 0;
        }
}
