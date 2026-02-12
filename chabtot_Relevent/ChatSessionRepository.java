package com.hospitalfinder.backend.repository;

import com.hospitalfinder.backend.entity.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    
    Optional<ChatSession> findBySessionId(String sessionId);
    
    Optional<ChatSession> findFirstByOrderByCreatedAtDesc();
    
    void deleteByExpiresAtBefore(LocalDateTime now);
}
