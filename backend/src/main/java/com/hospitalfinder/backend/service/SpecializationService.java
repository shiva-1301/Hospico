package com.hospitalfinder.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.entity.Specialization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecializationService {

    private final DataStoreService dataStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Specialization> getAllSpecializations() {
        try {
            JsonNode result = dataStoreService.executeQuery("SELECT * FROM specializations");
            List<Specialization> list = new ArrayList<>();
            if (result != null && result.isArray()) {
                for (JsonNode node : result) {
                    JsonNode data = node.has("specializations") ? node.get("specializations") : node;
                    list.add(objectMapper.convertValue(data, Specialization.class));
                }
            }
            return list;
        } catch (Exception e) {
            log.error("Error fetching specializations", e);
            return new ArrayList<>();
        }
    }

    public int seedSpecializations() {
        try {
            // Clear existing specializations
            dataStoreService.executeQuery("DELETE FROM specializations WHERE id > 0");

            // Standard specializations list
            String[] specializations = {
                "Anesthesiology", "Cardiology", "Dermatology", "Emergency Medicine",
                "Endocrinology", "Family Medicine", "Gastroenterology", "General Surgery",
                "Geriatrics", "Gynecology", "Hematology", "Infectious Disease",
                "Internal Medicine", "Nephrology", "Neurology", "Neurosurgery",
                "Obstetrics", "Oncology", "Ophthalmology", "Orthopedics",
                "Otolaryngology (ENT)", "Pathology", "Pediatrics", "Physical Medicine",
                "Plastic Surgery", "Psychiatry", "Pulmonology", "Radiology",
                "Rheumatology", "Urology", "Dentist", "General Physician"
            };

            // Insert each specialization
            int count = 0;
            for (int i = 0; i < specializations.length; i++) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", (long) (i + 1));
                data.put("specialization", specializations[i]);
                dataStoreService.insertRecord("specializations", data);
                count++;
            }

            log.info("Successfully seeded {} specializations", count);
            return count;
        } catch (Exception e) {
            log.error("Error seeding specializations", e);
            return 0;
        }
    }
}
