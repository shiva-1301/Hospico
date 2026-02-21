package com.hospitalfinder.backend.service;

import java.util.ArrayList;
import java.util.List;

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
}
