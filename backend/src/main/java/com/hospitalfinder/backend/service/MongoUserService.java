package com.hospitalfinder.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.entity.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "data.store.provider", havingValue = "mongo")
@RequiredArgsConstructor
@Slf4j
public class MongoUserService implements UserStoreService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    private final MongoTemplate mongoTemplate;
    private final MongoSequenceService sequenceService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean existsByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        return mongoTemplate.exists(query, "users");
    }

    @Override
    public UserData findByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        Document doc = mongoTemplate.findOne(query, Document.class, "users");
        if (doc == null) {
            log.info("User not found for email: {}", email);
            return null;
        }
        return mapToUserData(doc);
    }

    @Override
    public UserData findById(Long id) {
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("id").is(id),
                Criteria.where("_id").is(id)));
        Document doc = mongoTemplate.findOne(query, Document.class, "users");
        return doc != null ? mapToUserData(doc) : null;
    }

    @Override
    public UserData createUser(String name, String email, String phone, String password, Role role) {
        Long id = sequenceService.getNextSequence("users_id");

        Document doc = new Document();
        doc.put("id", id);
        doc.put("_id", id);
        doc.put("ROWID", id);
        doc.put("name", name);
        doc.put("email", email);
        doc.put("phone", phone);
        doc.put("password", passwordEncoder.encode(password));
        doc.put("role", role.name());
        doc.put("created_at", LocalDateTime.now().format(DATE_FORMAT));
        doc.put("updated_at", LocalDateTime.now().format(DATE_FORMAT));

        mongoTemplate.insert(doc, "users");
        return mapToUserData(doc);
    }

    @Override
    public UserData updateUser(String email, String name, String phone, Integer age, String gender, String password) {
        Query query = new Query(Criteria.where("email").is(email));
        Update update = new Update();
        boolean hasUpdate = false;

        if (name != null) {
            update.set("name", name);
            hasUpdate = true;
        }
        if (phone != null) {
            update.set("phone", phone);
            hasUpdate = true;
        }
        if (age != null) {
            update.set("age", age);
            hasUpdate = true;
        }
        if (gender != null) {
            update.set("gender", gender);
            hasUpdate = true;
        }
        if (password != null && !password.isBlank()) {
            update.set("password", passwordEncoder.encode(password));
            hasUpdate = true;
        }

        if (!hasUpdate) {
            Document existing = mongoTemplate.findOne(query, Document.class, "users");
            return existing != null ? mapToUserData(existing) : null;
        }

        update.set("updated_at", LocalDateTime.now().format(DATE_FORMAT));
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);
        Document updated = mongoTemplate.findAndModify(query, update, options, Document.class, "users");
        return updated != null ? mapToUserData(updated) : null;
    }

    private UserData mapToUserData(Document doc) {
        UserData user = new UserData();
        Object idValue = doc.get("id") != null ? doc.get("id") : doc.get("_id");
        if (idValue instanceof Number number) {
            user.setId(number.longValue());
        }
        user.setName(doc.getString("name"));
        user.setEmail(doc.getString("email"));
        user.setPhone(doc.getString("phone"));
        user.setPassword(doc.getString("password"));
        if (doc.get("age") instanceof Number number) {
            user.setAge(number.intValue());
        }
        user.setGender(doc.getString("gender"));
        String roleValue = doc.getString("role");
        if (roleValue != null) {
            user.setRole(Role.valueOf(roleValue));
        }
        return user;
    }
}
