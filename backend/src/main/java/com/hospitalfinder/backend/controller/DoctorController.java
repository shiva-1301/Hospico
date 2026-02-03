package com.hospitalfinder.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.dto.ClinicResponseDTO;
import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.entity.Doctor;
import com.hospitalfinder.backend.service.ClinicService;
import com.hospitalfinder.backend.service.DoctorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final ClinicService clinicService;

    @PostMapping("/clinics/{clinicId}/doctors")
    public ResponseEntity<?> addDoctorToClinic(@PathVariable Long clinicId, @RequestBody Doctor doctor) {
        try {
            ClinicResponseDTO clinicDTO = clinicService.getClinicById(clinicId);
            Clinic clinic = new Clinic();
            clinic.setId(clinicDTO.getClinicId()); // Populate minimal needed
            doctor.setClinic(clinic);

            Doctor savedDoctor = doctorService.save(doctor);
            return ResponseEntity.ok(savedDoctor);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Clinic not found");
        }
    }

    @GetMapping("/clinics/{clinicId}/doctors")
    public ResponseEntity<?> getDoctorsByClinicAndSpecialization(
            @PathVariable Long clinicId,
            @RequestParam(required = false) String specialization) {
        List<Doctor> doctors;
        if (specialization != null && !specialization.isEmpty()) {
            doctors = doctorService.findByClinicIdAndSpecialization(clinicId, specialization);
        } else {
            doctors = doctorService.findByClinicId(clinicId);
        }
        return ResponseEntity.ok(doctors);
    }

    @DeleteMapping("/doctors/{doctorId}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long doctorId) {
        doctorService.deleteDoctor(doctorId);
        return ResponseEntity.ok("Doctor deleted successfully");
    }
}