package com.hospitalfinder.backend.controller;

import com.hospitalfinder.backend.entity.MedicalRecord;
import com.hospitalfinder.backend.service.MedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam("userId") Long userId) {
        try {
            MedicalRecord record = medicalRecordService.uploadFile(file, category, userId);
            // Return record without data to save bandwidth
            record.setData(null);
            return ResponseEntity.ok(record);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MedicalRecord>> getUserRecords(@PathVariable Long userId) {
        List<MedicalRecord> records = medicalRecordService.getRecordsByUserId(userId);
        // Don't send file data in list view, it's too heavy
        records.forEach(r -> r.setData(null));
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
        return medicalRecordService.getRecordById(id)
                .map(record -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getName() + "\"")
                        .contentType(MediaType.parseMediaType(record.getType()))
                        .body(record.getData()))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        medicalRecordService.deleteRecord(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMedicalRecord(@PathVariable Long id,
            @RequestBody java.util.Map<String, Object> data) {
        try {
            MedicalRecord updated = medicalRecordService.updateMedicalRecord(id, data);
            updated.setData(null);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update medical record: " + e.getMessage());
        }
    }
}
