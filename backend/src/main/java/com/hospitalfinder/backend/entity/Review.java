package com.hospitalfinder.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    private Long id;

    private Integer rating;

    private String comment;

    private LocalDateTime createdAt;

    // Relations stored as IDs to keep it lightweight and flexible
    private Long userId;

    private Long hospitalId; // Note: Clinic ID

    private Long doctorId;
}
