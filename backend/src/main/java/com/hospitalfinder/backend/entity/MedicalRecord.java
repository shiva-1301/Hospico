package com.hospitalfinder.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {

    private Long id;

    private String name;

    private String type;

    private long size;

    private String category; // Diagnostics, Scanning, Prescriptions, Bills

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private byte[] data;

    private LocalDateTime uploadDate;

    // Assuming there is a User entity. Linking to it.
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;
}
