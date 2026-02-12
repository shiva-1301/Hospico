package com.hospitalfinder.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Doctor {
    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String qualifications;
    @Getter
    @Setter
    private String specialization;
    @Getter
    @Setter
    private String experience;
    @Getter
    @Setter
    private String biography;

    @JsonBackReference
    @Getter
    @Setter
    private Clinic clinic;
    @Getter
    @Setter
    private String imageUrl;

    @Getter
    @Setter
    private Double fees;
}