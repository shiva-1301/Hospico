package com.hospitalfinder.backend.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.entity.Doctor;
import com.hospitalfinder.backend.entity.Specialization;

import lombok.Getter;
import lombok.Setter;

public class ClinicResponseDTO {
    @Getter
    @Setter
    private String clinicId;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String address;
    @Getter
    @Setter
    private String city;
    @Getter
    @Setter
    private Double latitude;
    @Getter
    @Setter
    private Double longitude;
    @Getter
    @Setter
    private List<String> specializations;
    @Getter
    @Setter
    private String phone;
    @Getter
    @Setter
    private String website;
    @Getter
    @Setter
    private String timings;
    @Getter
    @Setter
    private Double rating;
    @Getter
    @Setter
    private Integer reviews;
    @Getter
    @Setter
    private String imageUrl;
    @Getter
    @Setter
    private List<Doctor> doctors = new ArrayList<>();

    // Constructor
    public ClinicResponseDTO(Clinic clinic) {
        this.clinicId = clinic.getId() != null ? clinic.getId().toString() : null;
        this.name = clinic.getName();
        this.address = clinic.getAddress();
        this.city = clinic.getCity();
        this.longitude = clinic.getLongitude();
        this.latitude = clinic.getLatitude();
        this.specializations = clinic.getSpecializations()
                .stream()
                .map(Specialization::getName)
                .collect(Collectors.toList());
        this.phone = clinic.getPhone();
        this.website = clinic.getWebsite();
        this.timings = clinic.getTimings();
        this.rating = clinic.getRating();
        this.reviews = clinic.getReviews();
        this.imageUrl = clinic.getImageUrl();
        this.doctors = clinic.getDoctors();
    }

}
