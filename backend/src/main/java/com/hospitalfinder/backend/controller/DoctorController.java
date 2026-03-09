package com.hospitalfinder.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.dto.ClinicResponseDTO;
import com.hospitalfinder.backend.dto.DoctorDTO;
import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.entity.Doctor;
import com.hospitalfinder.backend.entity.Role;
import com.hospitalfinder.backend.service.ClinicService;
import com.hospitalfinder.backend.service.DoctorService;
import com.hospitalfinder.backend.service.UserStoreService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DoctorController {

    private static final String DOCTOR_EMAIL_DOMAIN = "@hospiico.com";

    private final DoctorService doctorService;
    private final ClinicService clinicService;
    private final UserStoreService userStoreService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/doctors")
    public ResponseEntity<?> addDoctor(@RequestBody DoctorDTO doctorDTO) {
        try {
            if (doctorDTO.getEmail() == null || doctorDTO.getEmail().isBlank()
                    || doctorDTO.getPassword() == null || doctorDTO.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body("Doctor email and password are required");
            }

            if (!isDoctorDomainEmail(doctorDTO.getEmail())) {
                return ResponseEntity.badRequest().body("Doctor email must end with @hospiico.com");
            }

            if (doctorDTO.getClinicId() == null || doctorDTO.getClinicId().isBlank()) {
                return ResponseEntity.badRequest().body("clinicId is required");
            }

            Long clinicId;
            try {
                clinicId = Long.valueOf(doctorDTO.getClinicId());
            } catch (NumberFormatException ex) {
                return ResponseEntity.badRequest().body("clinicId must be numeric");
            }

            // Validate clinic exists
            ClinicResponseDTO clinicDTO = clinicService.getClinicById(clinicId);
            if (clinicDTO == null) {
                return ResponseEntity.badRequest().body("Clinic not found");
            }

            if (userStoreService.existsByEmail(doctorDTO.getEmail())) {
                UserData existingUser = userStoreService.findByEmail(doctorDTO.getEmail());
                if (existingUser == null) {
                    return ResponseEntity.badRequest().body("Unable to fetch existing doctor account");
                }
                if (existingUser.getRole() != Role.DOCTOR) {
                    return ResponseEntity.badRequest().body("Existing account is not a doctor account");
                }
                if (existingUser.getPassword() == null
                        || !passwordEncoder.matches(doctorDTO.getPassword(), existingUser.getPassword())) {
                    return ResponseEntity.badRequest().body("Invalid doctor credentials");
                }

                boolean alreadyInClinic = doctorService.findByClinicId(clinicId).stream()
                        .anyMatch(d -> d.getName() != null
                                && existingUser.getName() != null
                                && d.getName().equalsIgnoreCase(existingUser.getName()));
                if (alreadyInClinic) {
                    return ResponseEntity.badRequest().body("Doctor already added to this hospital");
                }

                Doctor existingDoctorToAttach = new Doctor();
                existingDoctorToAttach.setName(firstNonBlank(doctorDTO.getName(), existingUser.getName()));
                existingDoctorToAttach.setQualifications(doctorDTO.getQualifications());
                existingDoctorToAttach.setSpecialization(firstNonBlank(doctorDTO.getSpecialization(), "General Medicine"));
                existingDoctorToAttach.setExperience(doctorDTO.getExperience());
                existingDoctorToAttach.setBiography(doctorDTO.getBiography());
                existingDoctorToAttach.setFees(doctorDTO.getFees());
                existingDoctorToAttach.setImageUrl(doctorDTO.getImageUrl());

                Clinic clinic = new Clinic();
                clinic.setId(clinicId);
                existingDoctorToAttach.setClinic(clinic);

                Doctor savedDoctor = doctorService.save(existingDoctorToAttach);
                return ResponseEntity.ok(savedDoctor);
            }

            if (doctorDTO.getPhone() == null || doctorDTO.getPhone().isBlank()) {
                return ResponseEntity.badRequest().body("Phone is required for new doctor account");
            }

            Doctor doctor = new Doctor();
            doctor.setName(doctorDTO.getName());
            doctor.setQualifications(doctorDTO.getQualifications());
            doctor.setSpecialization(doctorDTO.getSpecialization());
            doctor.setExperience(doctorDTO.getExperience());
            doctor.setBiography(doctorDTO.getBiography());
            doctor.setFees(doctorDTO.getFees());
            doctor.setImageUrl(doctorDTO.getImageUrl());

            Clinic clinic = new Clinic();
            clinic.setId(clinicId);
            doctor.setClinic(clinic);

            Doctor savedDoctor = doctorService.save(doctor);

            UserData doctorUser = userStoreService.createUser(
                    firstNonBlank(doctorDTO.getName(), "Doctor"),
                    doctorDTO.getEmail(),
                    doctorDTO.getPhone(),
                    doctorDTO.getPassword(),
                    Role.DOCTOR);

            if (doctorUser == null) {
                if (savedDoctor != null && savedDoctor.getId() != null) {
                    try {
                        doctorService.deleteDoctor(savedDoctor.getId());
                    } catch (Exception ignored) {
                    }
                }
                return ResponseEntity.badRequest().body("Failed to create doctor login account");
            }

            return ResponseEntity.ok(savedDoctor);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add doctor: " + e.getMessage());
        }
    }

    @PostMapping("/clinics/{clinicId}/doctors")
    public ResponseEntity<?> addDoctorToClinic(@PathVariable Long clinicId, @RequestBody Doctor doctor) {
        try {
            ClinicResponseDTO clinicDTO = clinicService.getClinicById(clinicId);
            Clinic clinic = new Clinic();
            if (clinicDTO.getClinicId() != null) {
                clinic.setId(Long.valueOf(clinicDTO.getClinicId()));
            }
            doctor.setClinic(clinic);

            Doctor savedDoctor = doctorService.save(doctor);
            return ResponseEntity.ok(savedDoctor);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add doctor to clinic: " + e.getMessage());
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

    @PutMapping("/doctors/{doctorId}")
    public ResponseEntity<?> updateDoctor(@PathVariable Long doctorId,
            @RequestBody java.util.Map<String, Object> data) {
        try {
            Doctor updated = doctorService.updateDoctor(doctorId, data);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update doctor: " + e.getMessage());
        }
    }

    private boolean isDoctorDomainEmail(String email) {
        return email != null && email.trim().toLowerCase().endsWith(DOCTOR_EMAIL_DOMAIN);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}