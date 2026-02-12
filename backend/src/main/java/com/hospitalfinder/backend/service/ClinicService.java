package com.hospitalfinder.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.dto.ClinicRequestDTO;
import com.hospitalfinder.backend.dto.ClinicResponseDTO;
import com.hospitalfinder.backend.dto.ClinicSummaryDTO;
import com.hospitalfinder.backend.dto.NearbyClinicDTO;
import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.entity.Doctor;
import com.hospitalfinder.backend.entity.Specialization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicService {

    private final DataStoreService dataStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ClinicSummaryDTO> getFilteredClinics(String city, List<String> specializations, String search,
            Double lat, Double lng) {

        List<Clinic> clinics = fetchClinics("SELECT * FROM clinics");
        populateSpecializations(clinics);

        if (city != null && !city.isBlank()) {
            clinics = clinics.stream()
                    .filter(c -> c.getCity() != null && c.getCity().equalsIgnoreCase(city))
                    .collect(Collectors.toList());
        }

        List<String> normalizedSpecs = specializations == null ? List.of()
                : specializations.stream()
                        .filter(spec -> spec != null && !spec.isBlank())
                        .map(spec -> spec.toLowerCase())
                        .collect(Collectors.toList());

        if (!normalizedSpecs.isEmpty()) {
            clinics = clinics.stream()
                    .filter(clinic -> getMatchCount(clinic, normalizedSpecs) > 0)
                    .sorted((a, b) -> Integer.compare(
                            getMatchCount(b, normalizedSpecs),
                            getMatchCount(a, normalizedSpecs)))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            clinics = clinics.stream()
                    .filter(clinic -> clinic.getName().toLowerCase().contains(searchLower) ||
                            (clinic.getAddress() != null && clinic.getAddress().toLowerCase().contains(searchLower)) ||
                            (clinic.getCity() != null && clinic.getCity().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }

        return clinics.stream()
                .map(clinic -> {
                    Double distance = null;
                    Integer estimatedTime = null;
                    if (lat != null && lng != null && clinic.getLatitude() != null && clinic.getLongitude() != null) {
                        distance = calculateDistance(lat, lng, clinic.getLatitude(), clinic.getLongitude());
                        double speed = (distance < 5) ? 20.0 : (distance < 20) ? 30.0 : 40.0;
                        estimatedTime = (int) Math.round(distance / speed * 60);
                    }
                    return new ClinicSummaryDTO(clinic, distance, estimatedTime);
                })
                .collect(Collectors.toList());
    }

    public List<NearbyClinicDTO> getNearbyClinics(double lat, double lng, String city, String specialization) {
        List<Clinic> clinics = fetchClinics("SELECT * FROM clinics");
        populateSpecializations(clinics);

        if (city != null && !city.isEmpty()) {
            clinics = clinics.stream()
                    .filter(c -> c.getCity() != null && c.getCity().toLowerCase().contains(city.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (specialization != null && !specialization.isEmpty()) {
            clinics = clinics.stream()
                    .filter(c -> c.getSpecializations().stream()
                            .anyMatch(spec -> spec.getSpecialization().toLowerCase()
                                    .contains(specialization.toLowerCase())))
                    .collect(Collectors.toList());
        }

        return clinics.stream()
                .map(clinic -> {
                    Double distance = calculateDistance(lat, lng, clinic.getLatitude(), clinic.getLongitude());
                    if (distance > 5.0)
                        return null;

                    double speed = (distance < 5) ? 20.0 : (distance < 20) ? 30.0 : 40.0;
                    int estimatedTime = (int) Math.round(distance / speed * 60);

                    return new NearbyClinicDTO(clinic, distance, estimatedTime);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    public List<NearbyClinicDTO> getAllClinicsSortedByDistance(double lat, double lng, String city, List<String> spec,
            String search) {
        List<Clinic> clinics = fetchClinics("SELECT * FROM clinics");
        populateSpecializations(clinics);

        if (city != null && !city.isEmpty()) {
            clinics = clinics.stream()
                    .filter(clinic -> clinic.getCity() != null &&
                            clinic.getCity().equalsIgnoreCase(city))
                    .collect(Collectors.toList());
        }

        List<String> normalizedSpecs = spec == null ? List.of()
                : spec.stream().map(String::toLowerCase).collect(Collectors.toList());

        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            clinics = clinics.stream()
                    .filter(clinic -> clinic.getName().toLowerCase().contains(searchLower) ||
                            (clinic.getAddress() != null && clinic.getAddress().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }

        class ClinicDist {
            Clinic c;
            double d;
            int t;
            int m;

            ClinicDist(Clinic c, double d, int t, int m) {
                this.c = c;
                this.d = d;
                this.t = t;
                this.m = m;
            }
        }

        return clinics.stream()
                .map(c -> {
                    double dist = calculateDistance(lat, lng, c.getLatitude(), c.getLongitude());
                    double speed = (dist < 5) ? 20.0 : (dist < 20) ? 30.0 : 40.0;
                    int time = (int) Math.round(dist / speed * 60);
                    int match = getMatchCount(c, normalizedSpecs);
                    return new ClinicDist(c, dist, time, match);
                })
                .filter(cd -> normalizedSpecs.isEmpty() || cd.m > 0)
                .sorted((a, b) -> {
                    if (!normalizedSpecs.isEmpty()) {
                        int cmp = Integer.compare(b.m, a.m);
                        if (cmp != 0)
                            return cmp;
                    }
                    return Double.compare(a.d, b.d);
                })
                .map(cd -> new NearbyClinicDTO(cd.c, cd.d, cd.t))
                .collect(Collectors.toList());
    }

    public List<String> getAllCities() {
        return fetchClinics("SELECT distinct city FROM clinics").stream()
                .map(Clinic::getCity)
                .distinct()
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.toList());
    }

    // Helper to fetch clinics
    private List<Clinic> fetchClinics(String query) {
        try {
            JsonNode result = dataStoreService.executeQuery(query);
            List<Clinic> clinics = new ArrayList<>();
            if (result != null && result.isArray()) {
                for (JsonNode node : result) {
                    JsonNode data = node.has("clinics") ? node.get("clinics") : node;
                    clinics.add(mapToClinic(data));
                }
            }
            return clinics;
        } catch (Exception e) {
            log.error("Error fetching clinics", e);
            return new ArrayList<>();
        }
    }

    private Clinic mapToClinic(JsonNode node) {
        return objectMapper.convertValue(node, Clinic.class);
    }

    private void populateSpecializations(List<Clinic> clinics) {
        if (clinics.isEmpty())
            return;

        try {
            JsonNode specsResult = dataStoreService.executeQuery("SELECT * FROM specializations");
            List<Specialization> allSpecs = new ArrayList<>();
            if (specsResult != null && specsResult.isArray()) {
                for (JsonNode node : specsResult) {
                    JsonNode data = node.has("specializations") ? node.get("specializations") : node;
                    allSpecs.add(objectMapper.convertValue(data, Specialization.class));
                }
            }
            Map<Long, Specialization> specMap = allSpecs.stream()
                    .collect(Collectors.toMap(Specialization::getId, s -> s));

            JsonNode mappingResult = dataStoreService.executeQuery("SELECT * FROM clinic_specializations");
            if (mappingResult != null && mappingResult.isArray()) {
                for (JsonNode node : mappingResult) {
                    JsonNode data = node.has("clinic_specializations") ? node.get("clinic_specializations") : node;
                    Long clinicId = data.get("clinic_id").asLong();
                    Long specId = data.get("specializations_id").asLong();

                    clinics.stream().filter(c -> c.getId().equals(clinicId)).findFirst()
                            .ifPresent(c -> {
                                Specialization s = specMap.get(specId);
                                if (s != null)
                                    c.getSpecializations().add(s);
                            });
                }
            }
        } catch (Exception e) {
            log.error("Error populating specializations", e);
        }
    }

    public ClinicResponseDTO createClinic(ClinicRequestDTO request) {
        Map<String, Object> values = new HashMap<>();
        if (request.getName() != null) {
            values.put("name", request.getName());
        }
        if (request.getAddress() != null) {
            values.put("address", request.getAddress());
        }
        if (request.getCity() != null) {
            values.put("city", request.getCity());
        }
        if (request.getLatitude() != null) {
            values.put("latitude", request.getLatitude());
        }
        if (request.getLongitude() != null) {
            values.put("longitude", request.getLongitude());
        }
        if (request.getPhone() != null) {
            values.put("phone", request.getPhone());
        }
        if (request.getWebsite() != null) {
            values.put("website", request.getWebsite());
        }
        if (request.getTimings() != null) {
            values.put("timings", request.getTimings());
        }
        if (request.getRating() != null) {
            values.put("rating", request.getRating());
        }
        if (request.getReviews() != null) {
            values.put("reviews", request.getReviews());
        }
        if (request.getImageUrl() != null) {
            values.put("imageUrl", request.getImageUrl());
        }

        JsonNode createdNode = dataStoreService.insertRecord("clinics", values);
        Clinic clinic = mapToClinic(createdNode);
        Long clinicId = clinic.getId();
        if (clinicId == null) {
            clinicId = extractId(createdNode);
            clinic.setId(clinicId);
        }

        List<Long> specIds = request.getSpecializationIds();
        List<String> specNames = request.getSpecializations();

        if ((specIds == null || specIds.isEmpty()) && specNames != null && !specNames.isEmpty()) {
            specIds = new ArrayList<>();
            Map<String, Long> nameToId = loadSpecializationNameMap();
            for (String name : specNames) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                Long id = nameToId.get(name.toLowerCase());
                if (id == null) {
                    id = createSpecialization(name);
                    if (id != null) {
                        nameToId.put(name.toLowerCase(), id);
                    }
                }
                if (id != null) {
                    specIds.add(id);
                }
            }
        }

        if (clinicId != null && specIds != null && !specIds.isEmpty()) {
            for (Long specId : specIds) {
                if (specId == null) {
                    continue;
                }
                Map<String, Object> mapping = new HashMap<>();
                mapping.put("clinic_id", clinicId);
                mapping.put("specializations_id", specId);
                dataStoreService.insertRecord("clinic_specializations", mapping);
            }
        }

        populateSpecializations(Collections.singletonList(clinic));
        return new ClinicResponseDTO(clinic);
    }

    private Map<String, Long> loadSpecializationNameMap() {
        Map<String, Long> map = new HashMap<>();
        JsonNode specsResult = dataStoreService.executeQuery("SELECT * FROM specializations");
        if (specsResult != null && specsResult.isArray()) {
            for (JsonNode node : specsResult) {
                JsonNode data = node.has("specializations") ? node.get("specializations") : node;
                Specialization spec = objectMapper.convertValue(data, Specialization.class);
                if (spec.getSpecialization() != null && spec.getId() != null) {
                    map.put(spec.getSpecialization().toLowerCase(), spec.getId());
                } else if (spec.getSpecialization() != null) {
                    Long id = extractId(data);
                    if (id != null) {
                        map.put(spec.getSpecialization().toLowerCase(), id);
                    }
                }
            }
        }
        return map;
    }

    private Long createSpecialization(String name) {
        Map<String, Object> values = new HashMap<>();
        values.put("specialization", name);
        JsonNode created = dataStoreService.insertRecord("specializations", values);
        return extractId(created);
    }

    private Long extractId(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.has("id")) {
            return node.get("id").asLong();
        }
        if (node.has("ROWID")) {
            return node.get("ROWID").asLong();
        }
        return null;
    }

    public ClinicResponseDTO getClinicById(Long id) {
        Clinic clinic = fetchClinics("SELECT * FROM clinics WHERE ROWID = '" + id + "'").stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Clinic not found"));
        populateSpecializations(Collections.singletonList(clinic));
        clinic.setDoctors(fetchDoctors(id));
        return new ClinicResponseDTO(clinic);
    }

    public void deleteClinic(Long id) {
        try {
            dataStoreService.executeQuery("DELETE FROM clinics WHERE ROWID = '" + id + "'");
        } catch (Exception e) {
            log.error("Failed to delete clinic", e);
            throw new RuntimeException("Failed to delete clinic", e);
        }
    }

    private List<Doctor> fetchDoctors(Long clinicId) {
        try {
            String query = "SELECT * FROM doctors WHERE clinic_id = '" + clinicId + "'";
            JsonNode result = dataStoreService.executeQuery(query);
            List<Doctor> doctors = new ArrayList<>();
            if (result != null && result.isArray()) {
                for (JsonNode node : result) {
                    JsonNode data = node.has("doctors") ? node.get("doctors") : node;
                    doctors.add(objectMapper.convertValue(data, Doctor.class));
                }
            }
            return doctors;
        } catch (Exception e) {
            log.error("Error fetching doctors", e);
            return new ArrayList<>();
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    private int getMatchCount(Clinic clinic, List<String> normalizedSpecs) {
        if (normalizedSpecs == null || normalizedSpecs.isEmpty())
            return 0;
        return (int) clinic.getSpecializations().stream()
                .map(Specialization::getSpecialization)
                .filter(spec -> spec != null && !spec.isBlank())
                .map(String::toLowerCase)
                .filter(normalizedSpecs::contains)
                .count();
    }
}
