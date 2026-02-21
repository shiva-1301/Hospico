package com.hospitalfinder.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Doctor {
    @Getter
    @Setter
    @JsonProperty("id")
    @JsonAlias("ROWID")
    @JsonSerialize(using = ToStringSerializer.class)
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