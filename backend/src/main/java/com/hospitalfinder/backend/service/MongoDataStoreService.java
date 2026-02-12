package com.hospitalfinder.backend.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.types.Binary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "data.store.provider", havingValue = "mongo")
@RequiredArgsConstructor
@Slf4j
public class MongoDataStoreService implements DataStoreService {
    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "^SELECT\\s+(.*?)\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.+))?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_PATTERN = Pattern.compile(
            "^DELETE\\s+FROM\\s+(\\w+)\\s+WHERE\\s+(.+)$",
            Pattern.CASE_INSENSITIVE);

    private final MongoTemplate mongoTemplate;
    private final MongoSequenceService sequenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode insertRecord(String tableName, Map<String, Object> data) {
        Document doc = new Document();
        if (data != null) {
            doc.putAll(data);
        }

        Long id = extractLong(doc.get("id"));
        if (id == null) {
            id = extractLong(doc.get("ROWID"));
        }
        String sequenceName = tableName + "_id";
        if (id != null) {
            sequenceService.ensureSequenceAtLeast(sequenceName, id);
        } else {
            Long maxId = findMaxId(tableName);
            if (maxId != null) {
                sequenceService.ensureSequenceAtLeast(sequenceName, maxId);
            }
            id = sequenceService.getNextSequence(sequenceName);
        }

        doc.put("id", id);
        doc.put("ROWID", id);
        doc.put("_id", id);

        mongoTemplate.insert(doc, tableName);
        return objectMapper.valueToTree(doc);
    }

    private Long findMaxId(String tableName) {
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "id")).limit(1);
        Document doc = mongoTemplate.findOne(query, Document.class, tableName);
        if (doc == null) {
            return null;
        }
        Object idValue = doc.get("id") != null ? doc.get("id") : doc.get("_id");
        return extractLong(idValue);
    }

    @Override
    public JsonNode executeQuery(String query) {
        if (query == null || query.isBlank()) {
            return objectMapper.createArrayNode();
        }

        String trimmed = query.trim();
        Matcher deleteMatcher = DELETE_PATTERN.matcher(trimmed);
        if (deleteMatcher.matches()) {
            String table = deleteMatcher.group(1);
            String whereClause = deleteMatcher.group(2);
            deleteByWhere(table, whereClause);
            return objectMapper.createArrayNode();
        }

        Matcher selectMatcher = SELECT_PATTERN.matcher(trimmed);
        if (!selectMatcher.matches()) {
            throw new IllegalArgumentException("Unsupported query format: " + query);
        }

        String selectPart = selectMatcher.group(1).trim();
        String table = selectMatcher.group(2).trim();
        String whereClause = selectMatcher.group(3);

        boolean distinct = selectPart.toUpperCase(Locale.ROOT).startsWith("DISTINCT ");
        if (distinct) {
            String field = selectPart.substring("DISTINCT ".length()).trim();
            return distinctField(table, field, whereClause);
        }

        Query mongoQuery = buildQuery(whereClause);
        List<Document> results = mongoTemplate.find(mongoQuery, Document.class, table);
        return toArrayNode(results, selectPart);
    }

    @Override
    public void deleteRecord(String tableName, Long rowId) {
        if (rowId == null) {
            return;
        }
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("id").is(rowId),
                Criteria.where("_id").is(rowId));
        mongoTemplate.remove(new Query(criteria), tableName);
    }

    @Override
    public JsonNode updateRecord(String tableName, Long rowId, Map<String, Object> data) {
        if (rowId == null || data == null || data.isEmpty()) {
            return null;
        }
        Update update = new Update();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            update.set(entry.getKey(), entry.getValue());
        }
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("id").is(rowId),
                Criteria.where("_id").is(rowId));
        Query query = new Query(criteria);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);
        Document updated = mongoTemplate.findAndModify(query, update, options, Document.class, tableName);
        return updated != null ? objectMapper.valueToTree(normalizeDocument(updated)) : null;
    }

    @Override
    public JsonNode findById(String tableName, Long rowId) {
        if (rowId == null) {
            return null;
        }
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("id").is(rowId),
                Criteria.where("_id").is(rowId));
        Document doc = mongoTemplate.findOne(new Query(criteria), Document.class, tableName);
        return doc != null ? objectMapper.valueToTree(normalizeDocument(doc)) : null;
    }

    @Override
    public JsonNode findByField(String tableName, String fieldName, String fieldValue) {
        String normalizedField = normalizeField(fieldName);
        Object value = normalizeValue(fieldValue);
        Document doc = mongoTemplate.findOne(new Query(Criteria.where(normalizedField).is(value)), Document.class,
                tableName);
        return doc != null ? objectMapper.valueToTree(normalizeDocument(doc)) : null;
    }

    private void deleteByWhere(String table, String whereClause) {
        Query query = buildQuery(whereClause);
        mongoTemplate.remove(query, table);
    }

    private JsonNode distinctField(String table, String field, String whereClause) {
        Query query = buildQuery(whereClause);
        List<Object> values = mongoTemplate.findDistinct(query, normalizeField(field), table, Object.class);
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (Object value : values) {
            Document doc = new Document();
            doc.put(field, value);
            arrayNode.add(objectMapper.valueToTree(doc));
        }
        return arrayNode;
    }

    private Query buildQuery(String whereClause) {
        Query query = new Query();
        Criteria criteria = buildCriteria(whereClause);
        if (criteria != null) {
            query.addCriteria(criteria);
        }
        return query;
    }

    private Criteria buildCriteria(String whereClause) {
        if (whereClause == null || whereClause.isBlank()) {
            return null;
        }

        String[] parts = whereClause.split("(?i)\\s+AND\\s+");
        List<Criteria> criteriaList = new ArrayList<>();
        for (String part : parts) {
            String[] kv = part.split("=");
            if (kv.length != 2) {
                continue;
            }
            String field = normalizeField(kv[0].trim());
            String rawValue = kv[1].trim();
            Object value = normalizeValue(rawValue);
            criteriaList.add(Criteria.where(field).is(value));
        }

        if (criteriaList.isEmpty()) {
            return null;
        }
        if (criteriaList.size() == 1) {
            return criteriaList.get(0);
        }
        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    private ArrayNode toArrayNode(List<Document> results, String selectPart) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        List<String> fields = parseSelectFields(selectPart);
        for (Document doc : results) {
            Document normalized = normalizeDocument(doc);
            if (!fields.isEmpty()) {
                Document filtered = new Document();
                for (String field : fields) {
                    String normalizedField = normalizeField(field);
                    filtered.put(field, normalized.get(normalizedField));
                }
                arrayNode.add(objectMapper.valueToTree(filtered));
            } else {
                arrayNode.add(objectMapper.valueToTree(normalized));
            }
        }
        return arrayNode;
    }

    private List<String> parseSelectFields(String selectPart) {
        String trimmed = selectPart.trim();
        if ("*".equals(trimmed)) {
            return List.of();
        }
        String[] rawFields = trimmed.split(",");
        List<String> fields = new ArrayList<>();
        for (String field : rawFields) {
            String cleaned = field.trim();
            if (!cleaned.isEmpty()) {
                fields.add(cleaned);
            }
        }
        return fields;
    }

    private String normalizeField(String field) {
        if (field == null) {
            return null;
        }
        String trimmed = field.trim();
        if ("ROWID".equalsIgnoreCase(trimmed)) {
            return "id";
        }
        return trimmed;
    }

    private Object normalizeValue(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        Long asLong = extractLong(trimmed);
        return asLong != null ? asLong : trimmed;
    }

    private Long extractLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Document normalizeDocument(Document doc) {
        Object idValue = doc.get("id");
        if (idValue == null && doc.get("_id") != null) {
            idValue = doc.get("_id");
            doc.put("id", idValue);
        }
        if (idValue != null) {
            doc.put("ROWID", idValue);
        }
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Binary binary) {
                doc.put(entry.getKey(), Base64.getEncoder().encodeToString(binary.getData()));
            }
        }
        return doc;
    }
}
