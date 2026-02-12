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
        values.put("qualification", doctor.getQualifications()); // Fixed singular/plural
        values.put("specialization", doctor.getSpecialization());
        values.put("experience", doctor.getExperience());
        values.put("about", doctor.getBiography()); // Fixed about -> biography
        values.put("fees", doctor.getFees());
        if (doctor.getClinic() != null) {
            values.put("clinic_id", doctor.getClinic().getId());
        }

        try {
            JsonNode result = dataStoreService.insertRecord("doctors", values);
            return objectMapper.convertValue(result, Doctor.class);
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

    private List<Doctor> fetchDoctors(String query) {
        try {
            JsonNode result = dataStoreService.executeQuery(query);
            List<Doctor> doctors = new ArrayList<>();
            if (result != null && result.isArray()) {
                for (JsonNode node : result) {
                    JsonNode data = node.has("doctors") ? node.get("doctors") : node;
                    Doctor d = objectMapper.convertValue(data, Doctor.class);
                    // Manually populate Clinic stub if needed (usually ID is enough or handled by
                    // caller)
                    if (node.has("clinic_id")) {
                        Clinic c = new Clinic();
                        c.setId(node.get("clinic_id").asLong());
                        d.setClinic(c);
                    }
                    doctors.add(d);
                }
            }
            return doctors;
        } catch (Exception e) {
            log.error("Error fetching doctors", e);
            return new ArrayList<>();
        }
    }
}
