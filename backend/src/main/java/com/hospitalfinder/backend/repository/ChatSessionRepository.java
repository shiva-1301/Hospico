package com.hospitalfinder.backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hospitalfinder.backend.entity.ChatSession;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    
    Optional<ChatSession> findBySessionId(String sessionId);
    
    Optional<ChatSession> findByUserIdAndCurrentStep(String userId, String currentStep);
    
    Optional<ChatSession> findFirstByOrderByCreatedAtDesc();
}
