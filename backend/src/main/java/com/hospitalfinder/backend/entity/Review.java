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
public class Review {

    @JsonProperty("id")
    @JsonAlias("ROWID")
    private Long id;

    private Integer rating;

    private String comment;

    private LocalDateTime createdAt;

    // Relations stored as IDs to keep it lightweight and flexible
    private Long userId;

    private Long hospitalId; // Note: Clinic ID

    private Long doctorId;
}
