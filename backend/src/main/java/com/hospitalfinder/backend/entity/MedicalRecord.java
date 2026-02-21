package com.hospitalfinder.backend.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedicalRecord {

    @JsonProperty("id")
    @JsonAlias("ROWID")
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
