package com.hospitalfinder.backend.service;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public interface DataStoreService {
    JsonNode insertRecord(String tableName, Map<String, Object> data);

    JsonNode executeQuery(String query);

    JsonNode updateRecord(String tableName, Long rowId, Map<String, Object> data);

    void deleteRecord(String tableName, Long rowId);

    JsonNode findById(String tableName, Long rowId);

    JsonNode findByField(String tableName, String fieldName, String fieldValue);
}
