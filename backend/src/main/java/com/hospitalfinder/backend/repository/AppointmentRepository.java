package com.hospitalfinder.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.hospitalfinder.backend.entity.Appointment;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    
    // Find all appointments for a user
    List<Appointment> findByUserId(String userId);
    
    // Find appointments by doctor and appointment date
    @Query("{ 'doctorId': ?0, 'appointmentTime': { $gte: ?1, $lt: ?2 }, 'status': 'BOOKED' }")
    List<Appointment> findByDoctorIdAndDate(String doctorId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    
    // Check if a time slot is already booked
    boolean existsByDoctorIdAndAppointmentTime(String doctorId, LocalDateTime appointmentTime);
    
    // Find appointments by status
    List<Appointment> findByStatus(String status);
    
    // Find appointments by clinic
    List<Appointment> findByClinicId(String clinicId);
}
