package com.hospitalfinder.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.hospitalfinder.backend.entity.ChatSession;
import com.hospitalfinder.backend.repository.ChatSessionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;

    public Optional<ChatSession> findBySessionId(String sessionId) {
        return chatSessionRepository.findBySessionId(sessionId);
    }

    public Optional<ChatSession> findLatest() {
        return chatSessionRepository.findFirstByOrderByCreatedAtDesc();
    }

    public ChatSession save(ChatSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }
}
