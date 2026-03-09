package com.hospitalfinder.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalfinder.backend.entity.DoctorLeaveRequest;
import com.hospitalfinder.backend.entity.LeaveStatus;
import com.hospitalfinder.backend.service.DoctorLeaveService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctor-leaves")
@RequiredArgsConstructor
public class DoctorLeaveController {

    private final DoctorLeaveService doctorLeaveService;

    /** Doctor submits a leave request */
    @PostMapping("/request")
    public ResponseEntity<?> requestLeave(@RequestBody DoctorLeaveRequest request) {
        try {
            DoctorLeaveRequest saved = doctorLeaveService.createLeave(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to submit leave request: " + e.getMessage());
        }
    }

    /** Doctor views their own leave history */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<DoctorLeaveRequest>> getByDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(doctorLeaveService.getByDoctorId(doctorId));
    }

    /** Hospital owner views all pending requests */
    @GetMapping("/pending")
    public ResponseEntity<List<DoctorLeaveRequest>> getPending() {
        return ResponseEntity.ok(doctorLeaveService.getByStatus(LeaveStatus.PENDING));
    }

    /** Hospital owner approves or rejects a request */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam LeaveStatus status) {
        try {
            DoctorLeaveRequest updated = doctorLeaveService.updateStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update leave status: " + e.getMessage());
        }
    }
}
