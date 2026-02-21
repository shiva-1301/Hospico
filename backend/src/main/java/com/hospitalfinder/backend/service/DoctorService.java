package com.hospitalfinder.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.entity.Doctor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DataStoreService dataStoreService;
    private final SpecializationService specializationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Doctor findById(Long id) {
        try {
            // Check if ID is likely a Zoho Row ID (Long)
            String query = "SELECT * FROM doctors WHERE ROWID = '" + id + "'";
            JsonNode result = dataStoreService.executeQuery(query);

            if (result != null && result.isArray() && result.size() > 0) {
                JsonNode node = result.get(0);
                JsonNode data = node.has("doctors") ? node.get("doctors") : node;
                return objectMapper.convertValue(data, Doctor.class);
            }
            return null;
        } catch (Exception e) {
            log.error("Error finding doctor by id: {}", id, e);
            return null;
        }
    }

    public Doctor save(Doctor doctor) {
        Map<String, Object> values = new HashMap<>();
        values.put("name", doctor.getName());

        // Resolve specialization name to ID
        if (doctor.getSpecialization() != null) {
            var spec = specializationService.getSpecializationByName(doctor.getSpecialization());
            if (spec != null) {
                values.put("specialization_id", spec.getId());
            } else {
                log.warn("Specialization '{}' not found, skipping specialization_id", doctor.getSpecialization());
            }
        }

        if (doctor.getClinic() != null) {
            values.put("clinic_id", doctor.getClinic().getId());
        }

        // Columns like qualifications, experience, biography, fees are UNKNOWN to the
        // schema
        // and cause INVALID_INPUT. We must omit them for now.

        try {
            JsonNode result = dataStoreService.insertRecord("doctors", values);
            return mapToDoctor(result);
        } catch (Exception e) {
            log.error("Failed to save doctor", e);
            throw new RuntimeException("Failed to save doctor", e);
        }
    }

    public List<Doctor> findByClinicId(Long clinicId) {
        String query = "SELECT * FROM doctors WHERE clinic_id = '" + clinicId + "'";
        return fetchDoctors(query);
    }

    public List<Doctor> findByClinicIdAndSpecialization(Long clinicId, String specialization) {
        String query = "SELECT * FROM doctors WHERE clinic_id = '" + clinicId + "'";
        // Perform client-side filter for case-insensitive specialization to be safe
        List<Doctor> all = fetchDoctors(query);
        return all.stream()
                .filter(d -> d.getSpecialization() != null && d.getSpecialization().equalsIgnoreCase(specialization))
                .collect(Collectors.toList());
    }

    public void deleteDoctor(Long id) {
        try {
            dataStoreService.executeQuery("DELETE FROM doctors WHERE ROWID = '" + id + "'");
        } catch (Exception e) {
            log.error("Failed to delete doctor", e);
            throw new RuntimeException("Failed to delete doctor", e);
        }
    }

    public Doctor updateDoctor(Long id, Map<String, Object> data) {
        try {
            JsonNode result = dataStoreService.updateRecord("doctors", id, data);
            JsonNode rowData = result.has("doctors") ? result.get("doctors") : result;
            return objectMapper.convertValue(rowData, Doctor.class);
        } catch (Exception e) {
            log.error("Failed to update doctor {}", id, e);
            throw new RuntimeException("Failed to update doctor", e);
        }
    }

    private List<Doctor> fetchDoctors(String query) {
        try {
            JsonNode result = dataStoreService.executeQuery(query);
            List<Doctor> doctors = new ArrayList<>();
            if (result != null && result.isArray()) {
                for (JsonNode node : result) {
                    doctors.add(mapToDoctor(node));
                }
            }
            return doctors;
        } catch (Exception e) {
            log.error("Error fetching doctors", e);
            return new ArrayList<>();
        }
    }

    private Doctor mapToDoctor(JsonNode node) {
        JsonNode data = node.has("doctors") ? node.get("doctors") : node;
        Doctor d = objectMapper.convertValue(data, Doctor.class);

        // Map specialization_id back to specialization name
        if (data.has("specialization_id") && !data.get("specialization_id").isNull()) {
            Long specId = data.get("specialization_id").asLong();
            List<com.hospitalfinder.backend.entity.Specialization> specs = specializationService
                    .getAllSpecializations();
            specs.stream()
                    .filter(s -> s.getId().equals(specId))
                    .findFirst()
                    .ifPresent(s -> d.setSpecialization(s.getName()));
        }

        if (data.has("clinic_id") && !data.get("clinic_id").isNull()) {
            Clinic c = new Clinic();
            c.setId(data.get("clinic_id").asLong());
            d.setClinic(c);
        }
        return d;
    }
}
