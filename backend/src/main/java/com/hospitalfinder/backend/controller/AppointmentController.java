package com.hospitalfinder.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.dto.AppointmentRequestDTO;
import com.hospitalfinder.backend.dto.AppointmentResponseDTO;
import com.hospitalfinder.backend.service.AppointmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<?> bookAppointment(@RequestBody AppointmentRequestDTO dto) {
        try {
            AppointmentResponseDTO response = appointmentService.book(dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByUser(userId));
    }

    @GetMapping("/clinic/{clinicId}")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsByClinic(@PathVariable Long clinicId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByClinic(clinicId));
    }

    @GetMapping("/doctor/{doctorId}/date/{date}")
    public ResponseEntity<?> getAppointmentsByDoctorAndDate(@PathVariable Long doctorId, @PathVariable String date) {
        try {
            return ResponseEntity
                    .ok(appointmentService.getAppointmentsByDoctorAndDate(doctorId, LocalDate.parse(date)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(@PathVariable String id, @RequestParam String status) {
        try {
            AppointmentResponseDTO response = appointmentService.updateStatus(id, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update status: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable String id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.ok("Appointment deleted successfully");
    }
}
