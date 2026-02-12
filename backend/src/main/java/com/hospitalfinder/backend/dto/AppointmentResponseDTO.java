package com.hospitalfinder.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentResponseDTO {

    private String id;
    private String clinicId;
    private String clinicName;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private String userId;
    private String userName;
    private String appointmentTime;
    private String status;

    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String patientEmail;
    private String patientPhone;
    private String reason;

}
