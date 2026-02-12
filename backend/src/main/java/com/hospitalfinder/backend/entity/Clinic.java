package com.hospitalfinder.backend.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Clinic {
    @Getter
    @Setter
    private Long id;
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
    private Collection<Specialization> specializations = new ArrayList<>();
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
    private List<Doctor> doctors = new ArrayList<>();
    @Getter
    @Setter
    private String imageUrl;
}