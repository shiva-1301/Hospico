package com.hospitalfinder.backend.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    private String id;

    private String userId;
    private String sessionId;

    // Conversation state
    private String currentStep;

    // Collected data through conversation
    private String symptom;
    private String specialization;
    private String clinicId;
    private String clinicName;
    private String doctorId;
    private String doctorName;
    private String selectedDate;
    private String selectedTime;

    // Patient details
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String patientPhone;
    private String patientEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
}
