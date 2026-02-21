package com.hospitalfinder.backend.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.entity.Specialization;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicSummaryDTO {
    private String clinicId;
    private String name;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private List<String> specializations;
    private Double rating;
    private Integer reviews;
    private String imageUrl;
    private Double distance; // in kilometers
    private Double distanceKm; // in kilometers (aliased)
    private Integer estimatedTime; // in minutes
    private Integer estimatedWaitMinutes; // in minutes (aliased)

    public ClinicSummaryDTO(Clinic clinic) {
        this(clinic, null, null);
    }

    public ClinicSummaryDTO(Clinic clinic, Double distance, Integer estimatedTime) {
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
        this.rating = clinic.getRating();
        this.reviews = clinic.getReviews();
        this.imageUrl = clinic.getImageUrl();
        this.distance = distance;
        this.distanceKm = distance;
        this.estimatedTime = estimatedTime;
        this.estimatedWaitMinutes = estimatedTime;
    }
}
