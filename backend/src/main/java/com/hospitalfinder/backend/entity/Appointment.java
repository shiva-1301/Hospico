package com.hospitalfinder.backend.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;

    // Store IDs instead of full objects to match MongoDB structure
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("clinic_id")
    private String clinicId;

    @JsonProperty("doctor_id")
    private String doctorId;

    @JsonProperty("appointment_time")
    private LocalDateTime appointmentTime;

    private String status; // BOOKED, CANCELLED

    // NEW: patient details (for someone else)
    @JsonProperty("patient_name")
    private String patientName;
    
    @JsonProperty("patient_age")
    private Integer patientAge;
    
    @JsonProperty("patient_gender")
    private String patientGender;
    
    @JsonProperty("patient_phone")
    private String patientPhone;
    
    @JsonProperty("patient_email")
    private String patientEmail;
    
    private String reason;
}
