package com.hospitalfinder.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.entity.Specialization;
import com.hospitalfinder.backend.service.SpecializationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/specializations")
@RequiredArgsConstructor
public class SpecializationController {

    private final SpecializationService specializationService;

    @GetMapping
    public ResponseEntity<List<Specialization>> getAllSpecializations() {
        return ResponseEntity.ok(specializationService.getAllSpecializations());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Specialization> updateSpecialization(@PathVariable Long id,
            @RequestBody java.util.Map<String, Object> data) {
        return ResponseEntity.ok(specializationService.updateSpecialization(id, data));
    }
}
