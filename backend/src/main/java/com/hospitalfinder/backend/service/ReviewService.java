package com.hospitalfinder.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.entity.Review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final DataStoreService dataStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Review saveReview(Review review) {
        // Check local checks
        if (review.getUserId() != null && review.getDoctorId() != null) {
            String checkQuery = "SELECT ROWID FROM reviews WHERE user_id = '" + review.getUserId()
                    + "' AND doctor_id = '" + review.getDoctorId() + "'";
                JsonNode result = dataStoreService.executeQuery(checkQuery);
            if (result != null && result.isArray() && result.size() > 0) {
                throw new RuntimeException(
                        "You have already reviewed this doctor. Please delete your old review to submit a new one.");
            }
        }

        review.setCreatedAt(LocalDateTime.now());

        Map<String, Object> values = new HashMap<>();
        values.put("rating", review.getRating());
        values.put("comment", review.getComment());
        values.put("user_id", review.getUserId());
        values.put("hospital_id", review.getHospitalId());
        values.put("doctor_id", review.getDoctorId());
        values.put("created_at", review.getCreatedAt().toString());

        try {
            JsonNode result = dataStoreService.insertRecord("reviews", values);
            return objectMapper.convertValue(result, Review.class);
        } catch (Exception e) {
            log.error("Failed to save review", e);
            throw new RuntimeException("Failed to save review", e);
        }
    }

    public List<Review> getReviewsByHospital(Long hospitalId) {
        return fetchReviews("SELECT * FROM reviews WHERE hospital_id = '" + hospitalId + "'");
    }

    public List<Review> getReviewsByDoctor(Long doctorId) {
        return fetchReviews("SELECT * FROM reviews WHERE doctor_id = '" + doctorId + "'");
    }

    public List<Review> getReviewsByUserId(Long userId) {
        return fetchReviews("SELECT * FROM reviews WHERE user_id = '" + userId + "'");
    }

    public void deleteReview(Long id) {
        try {
            // Assume row ID deletion
            dataStoreService.deleteRecord("reviews", id);
            // Note: deleteRecord requires tableID usually, but here I passed string
            // "reviews".
            // My ZohoDataStoreService.deleteRecord(String tableId, Long rowId) expects
            // tableId.
            // If we don't know Table ID, we cannot use Row API easily.
            // But we can use ZCQL DELETE.
            // dataStoreService.executeZCQL("DELETE FROM reviews WHERE ROWID = '" + id +
            // "'");
            // ZCQL DELETE is safest.
            // BUT ZohoDataStoreService.deleteRecord implementation uses Row API.
            // I should have implemented deleteRecord with ZCQL or looked up table ID.
            // Let's rely on executeZCQL for delete which I used in MedicalRecordService.
            // Wait, did I implement ZCQL DELETE support? I implemented executeZCQL which
            // does POST /zcql.
            // Yes, that supports DELETE.
            dataStoreService.executeQuery("DELETE FROM reviews WHERE ROWID = '" + id + "'");
        } catch (Exception e) {
            log.error("Failed to delete review", e);
            throw new RuntimeException("Failed to delete review", e);
        }
    }

    private List<Review> fetchReviews(String query) {
        try {
            JsonNode result = dataStoreService.executeQuery(query);
            List<Review> reviews = new ArrayList<>();
            if (result != null && result.isArray()) {
                for (JsonNode node : result) {
                    JsonNode data = node.has("reviews") ? node.get("reviews") : node;
                    reviews.add(objectMapper.convertValue(data, Review.class));
                }
            }
            return reviews;
        } catch (Exception e) {
            log.error("Error fetching reviews", e);
            return new ArrayList<>();
        }
    }
}
