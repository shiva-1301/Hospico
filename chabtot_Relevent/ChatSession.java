package com.hospitalfinder.backend.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "chat_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ChatSession {

    @Id
    private String id;

    private String userId;  // Optional - for logged-in users
    private String sessionId;  // Unique session identifier

    // Conversation state
    private String currentStep;  // symptom_classification, hospital_selection, doctor_selection, date_selection, time_selection, booking_confirmation
    
    // Collected data through conversation
    private String symptom;
    private String specialization;
    private String clinicId;
    private String clinicName;
    private String doctorId;
    private String doctorName;
    private String selectedDate;  // ISO format YYYY-MM-DD
    private String selectedTime;  // HH:mm format
    
    // Patient details (will be collected or auto-filled)
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String patientPhone;
    private String patientEmail;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;  // Sessions expire after 30 minutes
}
