package com.hospitalfinder.backend.controller;

import com.hospitalfinder.backend.entity.Review;
import com.hospitalfinder.backend.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody Review review) {
        try {
            Review savedReview = reviewService.saveReview(review);
            return ResponseEntity.ok(savedReview);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to submit review");
        }
    }

    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<Review>> getHospitalReviews(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(reviewService.getReviewsByHospital(hospitalId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Review>> getUserReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getReviewsByUserId(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(@PathVariable Long id, @RequestBody java.util.Map<String, Object> data) {
        try {
            Review updated = reviewService.updateReview(id, data);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update review: " + e.getMessage());
        }
    }
}
