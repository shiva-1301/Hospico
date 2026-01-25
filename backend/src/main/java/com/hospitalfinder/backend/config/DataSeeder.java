package com.hospitalfinder.backend.config;

import java.util.List;
import java.util.Random;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;

import org.springframework.beans.factory.annotation.Value;
import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.entity.Doctor;
import com.hospitalfinder.backend.entity.Specialization;
import com.hospitalfinder.backend.repository.ClinicRepository;
import com.hospitalfinder.backend.repository.DoctorRepository;

@Component
public class DataSeeder {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;

    @Value("${zoho.enabled:false}")
    private boolean zohoEnabled;

    public DataSeeder(ClinicRepository clinicRepository, DoctorRepository doctorRepository) {
        this.clinicRepository = clinicRepository;
        this.doctorRepository = doctorRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (zohoEnabled) {
            System.out.println("Zoho enabled - skipping DataSeeder (Postgres)");
            return;
        }
        fixDoctorNames();
    }

    @Async
    public void fixDoctorNames() {
        List<Doctor> allDoctors = doctorRepository.findAll();
        if (allDoctors.isEmpty()) {
            System.out.println("No doctors found. Please ensure clinics are seeded or manually inserted.");
            // Fallback: If no doctors exist, we might want to generate them for existing
            // clinics.
            seedMissingDoctors();
            return;
        }

        Random random = new Random();
        String[] firstNames = {
                "Srinivas", "Ravi", "Lakshmi", "Priya", "Rahul", "Anjali", "Vikram", "Sneha",
                "Rajesh", "Kavita", "Suresh", "Meera", "Manoj", "Divya", "Arjun", "Deepa",
                "Vijay", "Swathi", "Kiran", "Nitya", "Mahesh", "Padma", "Gopal", "Anita"
        };
        String[] lastNames = {
                "Rao", "Reddy", "Latha", "Kumar", "Sharma", "Gupta", "Singh", "Mehta", "Patil",
                "Nair", "Prasad", "Varma", "Chowdary", "Naidu", "Babu", "Raju", "Murthy", "Sastry"
        };

        for (Doctor doc : allDoctors) {
            String name = doc.getName();
            String spec = doc.getSpecialization();

            // Check if name is generic (contains "Specialist" or equals specialization
            // name)
            if (name.contains("Specialist") || name.equalsIgnoreCase(spec)
                    || name.equalsIgnoreCase(spec + " Specialist")) {
                String newName = "Dr. " + firstNames[random.nextInt(firstNames.length)] + " "
                        + lastNames[random.nextInt(lastNames.length)];
                doc.setName(newName);
                doc.setBiography("Experienced " + spec.toLowerCase() + " specialist with over "
                        + (10 + random.nextInt(15)) + " years of practice.");
                doctorRepository.save(doc);
                System.out.println("Renamed " + name + " to " + newName);
            }
        }

        // Ensure all clinics, including new ones, have doctors
        seedMissingDoctors();
    }

    private void seedMissingDoctors() {
        var clinics = clinicRepository.findAllWithSpecializations();
        Random random = new Random();
        String[] firstNames = {
                "Srinivas", "Ravi", "Lakshmi", "Priya", "Rahul", "Anjali", "Vikram", "Sneha",
                "Rajesh", "Kavita", "Suresh", "Meera", "Manoj", "Divya", "Arjun", "Deepa"
        };
        String[] lastNames = {
                "Rao", "Reddy", "Latha", "Kumar", "Sharma", "Gupta", "Singh", "Mehta", "Patil",
                "Nair", "Prasad", "Varma", "Chowdary", "Naidu"
        };

        for (Clinic clinic : clinics) {
            var specs = clinic.getSpecializations();
            if (specs == null)
                continue;

            for (Specialization spec : specs) {
                String specName = spec.getSpecialization();
                var existing = doctorRepository.findByClinicIdAndSpecializationIgnoreCase(clinic.getId(), specName);
                if (existing != null && !existing.isEmpty())
                    continue;

                Doctor doc = new Doctor();
                doc.setClinic(clinic);
                doc.setSpecialization(specName);
                String newName = "Dr. " + firstNames[random.nextInt(firstNames.length)] + " "
                        + lastNames[random.nextInt(lastNames.length)];
                doc.setName(newName);
                doc.setQualifications("MBBS, MD");
                doc.setExperience((10 + random.nextInt(15)) + " years");
                doc.setBiography("Experienced " + specName.toLowerCase() + " specialist with over "
                        + doc.getExperience() + " of practice.");
                doctorRepository.save(doc);
            }
        }
    }
}