package com.hospitalfinder.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class ChatActionRequest {
    
    private String sessionId;
    private String action;  // select_hospital, select_doctor, select_date, select_time, confirm_booking
    private String value;  // The selected value (hospitalId, doctorId, date, time, etc.)
    
    // Optional: For booking confirmation
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String patientPhone;
    private String patientEmail;
    private String reason;
}
