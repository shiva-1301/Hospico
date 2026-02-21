package com.hospitalfinder.backend.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.entity.Specialization;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NearbyClinicDTO {
    private Long clinicId;
    private String name;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private List<String> specializations;
    private String phone;
    private String imageUrl;
    private Double distance; // in kilometers (legacy)
    private Double distanceKm; // in kilometers (preferred for clients)
    private Integer estimatedTime; // in minutes (legacy)
    private Integer estimatedWaitMinutes; // in minutes (preferred for clients)

    public NearbyClinicDTO(Clinic clinic, Double distance, Integer estimatedTime) {
        this.clinicId = clinic.getId();
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
        this.imageUrl = clinic.getImageUrl();
        this.distance = distance;
        this.distanceKm = distance;
        this.estimatedTime = estimatedTime;
        this.estimatedWaitMinutes = estimatedTime;
    }
}