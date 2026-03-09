package com.hospitalfinder.backend.dto;

import lombok.Data;

@Data
public class DoctorDTO {
    private String name;
    private String email;
    private String phone;
    private String password;
    private String qualifications;
    private String specialization;
    private String experience;
    private String biography;
    private Double fees;
    private String imageUrl;
    private String clinicId;
}
