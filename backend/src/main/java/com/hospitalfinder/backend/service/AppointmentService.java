package com.hospitalfinder.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hospitalfinder.backend.dto.AppointmentRequestDTO;
import com.hospitalfinder.backend.dto.AppointmentResponseDTO;
import com.hospitalfinder.backend.dto.ClinicResponseDTO;
import com.hospitalfinder.backend.dto.UserData;
import com.hospitalfinder.backend.entity.Appointment;
import com.hospitalfinder.backend.entity.Doctor;
import com.hospitalfinder.backend.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

        private final AppointmentRepository appointmentRepository;
        private final UserStoreService userStoreService;
        private final ClinicService clinicService;
        private final DoctorService doctorService;

        public AppointmentResponseDTO book(AppointmentRequestDTO dto) {
                // Validate user exists
                UserData userData = userStoreService.findById(dto.getUserId());
                if (userData == null) {
                        throw new RuntimeException("User not found");
                }

                // Validate clinic exists
                ClinicResponseDTO clinicDTO = clinicService.getClinicById(dto.getClinicId());
                if (clinicDTO == null) {
                        throw new RuntimeException("Clinic not found");
                }

                // Validate doctor exists
                Doctor doctor = doctorService.findById(dto.getDoctorId());
                if (doctor == null) {
                        throw new RuntimeException("Doctor not found");
                }

                LocalDateTime appointmentTime = LocalDateTime.parse(dto.getAppointmentTime());

                String doctorId = String.valueOf(dto.getDoctorId());
                if (appointmentRepository.existsByDoctorIdAndAppointmentTime(doctorId, appointmentTime)) {
                        throw new RuntimeException("Time slot already booked");
                }

                // Create and save appointment
                Appointment appointment = new Appointment();
                appointment.setUserId(String.valueOf(dto.getUserId()));
                appointment.setClinicId(String.valueOf(dto.getClinicId()));
                appointment.setDoctorId(doctorId);
                appointment.setAppointmentTime(appointmentTime);
                appointment.setStatus("BOOKED");
                appointment.setPatientName(dto.getPatientName());
                appointment.setPatientAge(dto.getPatientAge());
                appointment.setPatientGender(dto.getPatientGender());
                appointment.setPatientPhone(dto.getPatientPhone());
                appointment.setPatientEmail(dto.getPatientEmail());
                appointment.setReason(dto.getReason());

                // Save to MongoDB
                Appointment saved = appointmentRepository.save(appointment);
                log.info("✅ Appointment saved with ID: {}", saved.getId());

                // Return enriched response
                return buildAppointmentResponse(saved);
        }

        public List<AppointmentResponseDTO> getAppointmentsByUser(Long userId) {
                List<Appointment> appointments = appointmentRepository.findByUserId(String.valueOf(userId));
                return appointments.stream()
                                .map(this::buildAppointmentResponse)
                                .collect(Collectors.toList());
        }

        public List<AppointmentResponseDTO> getAppointmentsByClinic(Long clinicId) {
                List<Appointment> appointments = appointmentRepository.findByClinicId(String.valueOf(clinicId));
                return appointments.stream()
                                .map(this::buildAppointmentResponse)
                                .collect(Collectors.toList());
        }

        // For doctor date view
        public List<AppointmentResponseDTO> getAppointmentsByDoctorAndDate(Long doctorId, LocalDate date) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

                List<Appointment> appointments = appointmentRepository.findByDoctorIdAndDate(
                        String.valueOf(doctorId), startOfDay, endOfDay);
                
                return appointments.stream()
                                .map(this::buildAppointmentResponse)
                                .collect(Collectors.toList());
        }

        public void deleteAppointment(String id) {
                appointmentRepository.deleteById(id);
        }

        /**
         * Build an enriched response DTO with full user/clinic/doctor details
         */
        private AppointmentResponseDTO buildAppointmentResponse(Appointment appointment) {
                AppointmentResponseDTO dto = new AppointmentResponseDTO();
                dto.setId(appointment.getId());
                dto.setAppointmentTime(appointment.getAppointmentTime() != null 
                        ? appointment.getAppointmentTime().toString() : null);
                dto.setStatus(appointment.getStatus());
                dto.setPatientName(appointment.getPatientName());
                dto.setPatientAge(appointment.getPatientAge());
                dto.setPatientGender(appointment.getPatientGender());
                dto.setPatientPhone(appointment.getPatientPhone());
                dto.setPatientEmail(appointment.getPatientEmail());
                dto.setReason(appointment.getReason());

                // Enrich with user details
                if (appointment.getUserId() != null) {
                        dto.setUserId(appointment.getUserId());
                        try {
                                Long userIdLong = Long.parseLong(appointment.getUserId());
                                UserData userData = userStoreService.findById(userIdLong);
                                if (userData != null) {
                                        dto.setUserName(userData.getName());
                                }
                        } catch (Exception e) {
                                log.warn("Could not fetch user {}", appointment.getUserId(), e);
                        }
                }

                // Enrich with clinic details
                if (appointment.getClinicId() != null) {
                        dto.setClinicId(appointment.getClinicId());
                        try {
                                Long clinicIdLong = Long.parseLong(appointment.getClinicId());
                                ClinicResponseDTO clinicDTO = clinicService.getClinicById(clinicIdLong);
                                if (clinicDTO != null) {
                                        dto.setClinicName(clinicDTO.getName());
                                }
                        } catch (Exception e) {
                                log.warn("Could not fetch clinic {}", appointment.getClinicId(), e);
                        }
                }

                // Enrich with doctor details
                if (appointment.getDoctorId() != null) {
                        dto.setDoctorId(appointment.getDoctorId());
                        try {
                                Long doctorIdLong = Long.parseLong(appointment.getDoctorId());
                                Doctor doctor = doctorService.findById(doctorIdLong);
                                if (doctor != null) {
                                        dto.setDoctorName(doctor.getName());
                                        dto.setDoctorSpecialization(doctor.getSpecialization());
                                }
                        } catch (Exception e) {
                                log.warn("Could not fetch doctor {}", appointment.getDoctorId(), e);
                        }
                }

                return dto;
        }
}
