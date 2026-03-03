package com.hospitalfinder.backend.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalfinder.backend.dto.AppointmentRequestDTO;
import com.hospitalfinder.backend.dto.AppointmentResponseDTO;
import com.hospitalfinder.backend.dto.ChatActionRequest;
import com.hospitalfinder.backend.dto.ChatRequest;
import com.hospitalfinder.backend.dto.ClinicResponseDTO;
import com.hospitalfinder.backend.dto.ClinicSummaryDTO;
import com.hospitalfinder.backend.entity.ChatSession;
import com.hospitalfinder.backend.entity.Doctor;
import com.hospitalfinder.backend.service.AppointmentService;
import com.hospitalfinder.backend.service.ChatSessionService;
import com.hospitalfinder.backend.service.ClinicService;
import com.hospitalfinder.backend.service.DoctorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    @Value("${groq.api.key:}")
    private String apiKey;

    private final ClinicService clinicService;
    private final ChatSessionService chatSessionService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Maximum hospitals to return in symptom-based search
    private static final int MAX_HOSPITAL_RESULTS = 5;

    // Pattern to detect "hospital near X" or "hospitals in X" queries
    private static final Pattern HOSPITAL_QUERY_PATTERN = Pattern.compile(
            "(?:hospitals?|clinics?)\\s+(?:near|in|at|around)\\s+(.+)",
            Pattern.CASE_INSENSITIVE);

    // Keywords that indicate health symptoms (for conditional prompt injection)
    private static final List<String> SYMPTOM_KEYWORDS = Arrays.asList(
            "pain", "ache", "aching", "fever", "cough", "cold", "headache", "stomach",
            "breathing", "breath", "chest", "heart", "skin", "rash", "itch", "itching",
            "swelling", "swollen", "injury", "injured", "blood", "bleeding", "vomit",
            "nausea", "dizziness", "dizzy", "fatigue", "tired", "weakness", "weak",
            "infection", "sore", "throat", "ear", "eye", "vision", "hearing", "joint",
            "bone", "muscle", "back", "neck", "leg", "arm", "hand", "foot", "feet",
            "nose", "allergy", "allergic", "pregnant", "pregnancy", "period", "menstrual",
            "diabetes", "sugar", "pressure", "bp", "anxiety", "depression", "sleep",
            "insomnia", "cancer", "tumor", "lump", "burn", "cut", "wound", "fracture",
            "sprain", "symptom", "symptoms", "problem", "issue", "suffering", "hurts",
            "hurt", "hurting", "uncomfortable", "discomfort", "unwell", "sick", "ill",
            "disease", "condition", "diagnosis", "treatment", "doctor", "specialist");

    // Valid specializations on the platform
    private static final List<String> VALID_SPECIALIZATIONS = Arrays.asList(
            "cardiology", "orthopedics", "pediatrics", "dermatology", "neurology",
            "gynecology", "ent", "general medicine", "surgery", "ophthalmology",
            "pulmonology", "oncology");

    // Symptom analysis prompt — produces JSON for internal parsing
    private static final String SYMPTOM_ANALYSIS_PROMPT = """
            The user is describing health symptoms. Respond ONLY with valid JSON in this exact format:
            {"type":"specialization_match","symptom":"<2-5 word summary>","inferred_issue":"<2-3 word non-diagnostic label>","natural_response":"<1-2 sentence warm, empathetic response about their symptom — no bullets, no templates>","specializations":["<spec1>"],"confidence":"low|medium|high","disclaimer":"This is general guidance, not a diagnosis. Please consult a doctor."}

            RULES:
            - natural_response: Write like a caring assistant. Example: "I'm sorry you're dealing with that. Pain behind the ear is often related to minor inflammation or an ear infection."
            - Do NOT use bullet lists or "Based on your symptoms" template
            - Do NOT use emoji in natural_response
            - symptom: brief summary ("Ear pain", "Chest tightness")
            - inferred_issue: 2-3 words ("Ear infection", "Tension headache")
            - specializations: 1-2 from ONLY: Cardiology, Orthopedics, Pediatrics, Dermatology, Neurology, Gynecology, ENT, General Medicine, Surgery, Ophthalmology, Pulmonology, Oncology
            - If unsure, use "General Medicine"
            - Do NOT diagnose or prescribe
            - Respond ONLY with JSON
            """;

    @jakarta.annotation.PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("CRITICAL: Groq API Key is NOT loaded!");
        } else {
            System.out.println("Groq API Key loaded successfully. Length: " + apiKey.length());
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        // Get the latest user message
        List<ChatRequest.Message> messages = request.getMessages();
        if (messages != null && !messages.isEmpty()) {
            ChatRequest.Message lastMessage = messages.get(messages.size() - 1);
            String content = lastMessage.getContent();

            if (content != null) {
                // Check if it's a hospital search query (explicit)
                Matcher matcher = HOSPITAL_QUERY_PATTERN.matcher(content.trim());
                if (matcher.find()) {
                    String placeName = matcher.group(1).trim();
                    System.out.println("Hospital search detected for place: " + placeName);
                    if (placeName.equalsIgnoreCase("me") || placeName.equalsIgnoreCase("my location")) {
                        if (request.getLatitude() != null && request.getLongitude() != null) {
                            return handleNearbySearch(request.getLatitude(), request.getLongitude());
                        }
                        return returnAsNormalText(
                                "I need access to your location to find hospitals near you. Please enable location services or specify a city name (e.g., 'Hospital in Hyderabad').");
                    }
                    return handleHospitalCitySearch(placeName);
                }
            }
        }

        String latestMessage = messages != null && !messages.isEmpty()
                ? messages.get(messages.size() - 1).getContent()
                : "";

        String lowerMessage = latestMessage == null ? "" : latestMessage.toLowerCase();

        // Detect intent — order matters: booking > doctors > hospitals
        boolean wantsDoctors = lowerMessage.contains("doctor") && (lowerMessage.contains("show") || lowerMessage.contains("list") || lowerMessage.contains("see"));
        boolean wantsHospitals = !wantsDoctors && (lowerMessage.contains("hospitals")
                || lowerMessage.contains("nearby") || lowerMessage.contains("find hospital")
                || (lowerMessage.contains("show") && !lowerMessage.contains("doctor")));

        boolean wantsBooking = (lowerMessage.contains("book") &&
                (lowerMessage.contains("appointment") || lowerMessage.contains("doctor")
                        || lowerMessage.contains("visit")))
                || lowerMessage.equals("book an appointment")
                || lowerMessage.equals("book appointment");

        if (wantsBooking) {
            // Check if we already have a session with symptoms collected
            ChatSession existingSession = resolveSession(request.getSessionId());
            if (existingSession != null && existingSession.getSpecialization() != null) {
                // Symptoms already collected — skip directly to hospital selection
                return showHospitalsFromSession(existingSession, request.getLatitude(), request.getLongitude());
            }
            // No previous symptoms — ask naturally (will be caught by symptom handler next time)
            return returnAsNormalText(
                    "Sure! I'd be happy to help you book an appointment.\n" +
                    "Could you tell me what health issue you're experiencing?");
        }

        if (wantsDoctors) {
            // User wants to see doctors for the currently selected hospital
            ChatSession session = resolveSession(request.getSessionId());
            if (session != null && session.getClinicId() != null) {
                return handleHospitalSelection(session, session.getClinicId());
            }
            // No hospital selected yet — guide them
            if (session != null && session.getSpecialization() != null) {
                return showHospitalsFromSession(session, request.getLatitude(), request.getLongitude());
            }
            return returnAsNormalText("Please select a hospital first, then I can show you the available doctors.");
        }

        if (wantsHospitals) {
            ChatSession session = resolveSession(request.getSessionId());
            if (session != null && session.getSpecialization() != null) {
                return showHospitalsFromSession(session, request.getLatitude(), request.getLongitude());
            }
        }

        // Check if message contains symptom keywords
        boolean containsSymptoms = containsSymptomKeywords(latestMessage);

        // Proceed with AI chat
        String url = "https://api.groq.com/openai/v1/chat/completions";

        System.out.println("Received chat request with "
                + (request.getMessages() != null ? request.getMessages().size() : 0) + " messages.");
        System.out.println("Contains symptoms: " + containsSymptoms);
        System.out.println("Using API Key: "
                + (apiKey != null && apiKey.length() > 5 ? apiKey.substring(0, 5) + "..." : "NULL/EMPTY"));

        // Get language from request (default to English)
        String language = request.getLanguage();
        String languageName = getLanguageName(language != null ? language : "en");

        // 1. Prepare Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Prepare System Message with conditional symptom prompt
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");

        String systemPrompt;
        if (containsSymptoms) {
            // Use symptom analysis prompt
            systemPrompt = SYMPTOM_ANALYSIS_PROMPT;
        } else {
            // Normal healthcare assistant prompt — stateful, conversational
            systemPrompt = "You are Hospico's Health & Booking Assistant. " +
                    "RULES: Keep responses to 2-3 lines max. Be warm and professional. " +
                    "Never show JSON, code, bullet templates, or technical details. " +
                    "Never repeat the same symptom analysis or 'Based on your symptoms' format. " +
                    "If user already described symptoms, do NOT ask again. " +
                    "Do NOT diagnose. Gently suggest consulting a doctor. " +
                    "If user asks about hospitals, tell them to say 'hospital near [city name]'.";
        }

        // Add language instruction if not English
        if (language != null && !language.equals("en") && !containsSymptoms) {
            systemPrompt += " IMPORTANT: You MUST respond in " + languageName
                    + " language. All your responses should be written in " + languageName + ".";
        }
        systemMessage.put("content", systemPrompt);

        // 3. Combine Messages
        List<Object> allMessages = new ArrayList<>();
        allMessages.add(systemMessage);
        if (request.getMessages() != null) {
            allMessages.addAll(request.getMessages());
        }

        // 4. Request Body
        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");
        body.put("messages", allMessages);
        body.put("temperature", containsSymptoms ? 0.1 : 0.3); // Lower temperature for symptom analysis
        body.put("max_tokens", 350);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // 5. Call Groq API
            System.out.println("Sending request to Groq API...");
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            System.out.println("Groq Response Status: " + response.getStatusCode());

            Map<String, Object> responseBody = response.getBody();

            // 6. Extract Content
            if (responseBody != null && responseBody.containsKey("choices")) {
                List choices = (List) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map message = (Map) choice.get("message");
                    String replyContent = (String) message.get("content");

                    // If symptoms were detected, try to parse JSON response
                    if (containsSymptoms && replyContent != null) {
                        return handleSymptomResponse(replyContent, request.getLatitude(), request.getLongitude());
                    }

                    // Normal text response
                    Map<String, Object> result = new HashMap<>();
                    result.put("type", "text");
                    result.put("reply", replyContent);
                    return ResponseEntity.ok(result);
                }
            }
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("type", "text");
            emptyResult.put("reply", "No response from AI (Empty choices)");
            return ResponseEntity.ok(emptyResult);

        } catch (HttpClientErrorException e) {
            System.err.println("Groq API Error: " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Collections.singletonMap("error", "Groq API Error: " + e.getResponseBodyAsString()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Internal Server Error: " + e.getMessage()));
        }
    }

    /**
     * Handle step-by-step actions from chatbot (hospital and doctor selection)
     */
    @PostMapping("/chat/action")
    public ResponseEntity<?> handleChatAction(@RequestBody ChatActionRequest request) {
        try {
            System.out.println("Chat action received: " + request.getAction() + " with value: " + request.getValue());

            ChatSession session = chatSessionService.findBySessionId(request.getSessionId())
                    .orElseGet(() -> {
                        ChatSession newSession = new ChatSession();
                        String sessionId = request.getSessionId() != null && !request.getSessionId().isBlank()
                                ? request.getSessionId()
                                : UUID.randomUUID().toString();
                        newSession.setSessionId(sessionId);
                        newSession.setCreatedAt(LocalDateTime.now());
                        newSession.setExpiresAt(LocalDateTime.now().plusMinutes(30));
                        newSession.setCurrentStep("symptom_classification");
                        return newSession;
                    });

            switch (request.getAction()) {
                case "select_hospital":
                    return handleHospitalSelection(session, request.getValue());
                case "select_doctor":
                    return handleDoctorSelection(session, request.getValue());
                case "select_date":
                    return handleDateSelection(session, request.getValue());
                case "select_time":
                    return handleTimeSelection(session, request.getValue());
                case "confirm_booking":
                    return handleBookingConfirmation(session, request);
                default:
                    return ResponseEntity.badRequest().body(
                            Collections.singletonMap("error", "Unknown action: " + request.getAction()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    Collections.singletonMap("error", "Failed to process action: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> handleHospitalSelection(ChatSession session, String clinicId) {
        Long clinicIdLong;
        try {
            clinicIdLong = Long.parseLong(clinicId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid hospital id"));
        }

        ClinicResponseDTO clinic = clinicService.getClinicById(clinicIdLong);

        session.setClinicId(String.valueOf(clinicIdLong));
        session.setClinicName(clinic.getName());
        session.setCurrentStep("doctor_selection");
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionService.save(session);

        List<Doctor> doctors;
        if (session.getSpecialization() != null && !session.getSpecialization().isBlank()) {
            doctors = doctorService.findByClinicIdAndSpecialization(clinicIdLong, session.getSpecialization());
            // Fallback: if no doctors match the specialization, show all doctors at this hospital
            if (doctors == null || doctors.isEmpty()) {
                doctors = doctorService.findByClinicId(clinicIdLong);
            }
        } else {
            doctors = doctorService.findByClinicId(clinicIdLong);
        }

        List<Map<String, Object>> doctorList = new ArrayList<>();
        for (Doctor doctor : doctors) {
            Map<String, Object> doctorInfo = new HashMap<>();
            doctorInfo.put("id", doctor.getId());
            doctorInfo.put("name", doctor.getName());
            doctorInfo.put("specialization", doctor.getSpecialization());
            doctorInfo.put("qualifications", doctor.getQualifications());
            doctorInfo.put("experience", doctor.getExperience());
            doctorInfo.put("imageUrl", doctor.getImageUrl() != null ? doctor.getImageUrl() : "");
            doctorList.add(doctorInfo);
        }

        // Handle case where hospital has no doctors at all
        if (doctorList.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("step", "hospital_selection");
            response.put("message", "Sorry, " + clinic.getName() + " doesn't have any available doctors right now. Please select a different hospital.");
            response.put("sessionId", session.getSessionId());
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("step", "doctor_selection");
        response.put("message", "Please choose a doctor from " + clinic.getName() + ".");
        response.put("doctors", doctorList);
        response.put("sessionId", session.getSessionId());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleDoctorSelection(ChatSession session, String doctorId) {
        Long doctorIdLong;
        try {
            doctorIdLong = Long.parseLong(doctorId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid doctor id"));
        }

        Doctor doctor = doctorService.findById(doctorIdLong);
        if (doctor == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Doctor not found"));
        }

        session.setDoctorId(String.valueOf(doctorIdLong));
        session.setDoctorName(doctor.getName());
        session.setCurrentStep("date_selection");
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionService.save(session);

        Map<String, Object> response = new HashMap<>();
        response.put("step", "date_selection");
        response.put("message", "Great choice! " + formatDoctorName(doctor.getName())
                + " (" + doctor.getSpecialization() + "). \nWhen would you like to schedule your appointment?");
        response.put("sessionId", session.getSessionId());
        return ResponseEntity.ok(response);
    }

    // ── New Booking Flow Handlers ─────────────────────────────────

    private ResponseEntity<?> handleDateSelection(ChatSession session, String dateValue) {
        LocalDate selectedDate;
        try {
            selectedDate = LocalDate.parse(dateValue);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Collections.singletonMap("error", "Please select a valid date."));
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(
                    Collections.singletonMap("error", "Cannot book appointments in the past. Please select today or a future date."));
        }

        session.setSelectedDate(selectedDate.toString());
        session.setCurrentStep("time_selection");
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionService.save(session);

        // Generate available time slots
        List<String> availableSlots = generateAvailableSlots(session.getDoctorId(), selectedDate);

        Map<String, Object> response = new HashMap<>();
        response.put("step", "time_selection");
        response.put("message", "Please choose a convenient time slot for " + selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")) + ".");
        response.put("availableSlots", availableSlots);
        response.put("sessionId", session.getSessionId());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleTimeSelection(ChatSession session, String timeValue) {
        session.setSelectedTime(timeValue);
        session.setCurrentStep("patient_details");
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionService.save(session);

        // Build appointment summary for confirmation
        Map<String, Object> appointmentDetails = new HashMap<>();
        appointmentDetails.put("hospital", session.getClinicName() != null ? session.getClinicName() : "Selected Hospital");
        appointmentDetails.put("doctor", session.getDoctorName() != null ? formatDoctorName(session.getDoctorName()) : "Selected Doctor");
        appointmentDetails.put("date", session.getSelectedDate());
        appointmentDetails.put("time", timeValue);

        Map<String, Object> response = new HashMap<>();
        response.put("step", "patient_details");
        response.put("message", "Almost there! Please confirm your booking details below.");
        response.put("appointmentDetails", appointmentDetails);
        response.put("sessionId", session.getSessionId());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleBookingConfirmation(ChatSession session, ChatActionRequest request) {
        try {
            // Build the appointment request DTO
            AppointmentRequestDTO dto = new AppointmentRequestDTO();
            dto.setClinicId(Long.parseLong(session.getClinicId()));
            dto.setDoctorId(Long.parseLong(session.getDoctorId()));

            // Combine date + time into ISO datetime
            String dateTime = session.getSelectedDate() + "T" + session.getSelectedTime() + ":00";
            dto.setAppointmentTime(dateTime);

            // Patient details from request
            dto.setPatientName(request.getPatientName());
            dto.setPatientAge(request.getPatientAge());
            dto.setPatientGender(request.getPatientGender());
            dto.setPatientPhone(request.getPatientPhone());
            dto.setPatientEmail(request.getPatientEmail());
            dto.setReason(request.getReason() != null ? request.getReason() : session.getSymptom());

            // Try to get userId from the request value
            if (request.getValue() != null && !request.getValue().isBlank()) {
                try {
                    dto.setUserId(Long.parseLong(request.getValue()));
                } catch (NumberFormatException ignored) {
                    // userId is optional for chatbot booking
                }
            }

            // Call AppointmentService to actually book
            AppointmentResponseDTO booked = appointmentService.book(dto);

            // Update session
            session.setPatientName(request.getPatientName());
            session.setPatientAge(request.getPatientAge());
            session.setPatientGender(request.getPatientGender());
            session.setPatientPhone(request.getPatientPhone());
            session.setPatientEmail(request.getPatientEmail());
            session.setCurrentStep("booking_confirmed");
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionService.save(session);

            // Build confirmation response
            Map<String, Object> appointmentDetails = new HashMap<>();
            appointmentDetails.put("hospital", booked.getClinicName() != null ? booked.getClinicName() : session.getClinicName());
            appointmentDetails.put("doctor", booked.getDoctorName() != null ? formatDoctorName(booked.getDoctorName()) : formatDoctorName(session.getDoctorName()));
            appointmentDetails.put("date", session.getSelectedDate());
            appointmentDetails.put("time", session.getSelectedTime());
            appointmentDetails.put("patient", request.getPatientName());
            appointmentDetails.put("appointmentId", booked.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("step", "booking_confirmed");
            response.put("message", "Your appointment has been successfully booked! " +
                    "You can view it anytime in My Appointments.");
            response.put("appointmentDetails", appointmentDetails);
            response.put("sessionId", session.getSessionId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Booking confirmation failed: " + e.getMessage());
            e.printStackTrace();

            String userMessage = "I'm sorry, something went wrong while booking your appointment. Please try again.";
            if (e.getMessage() != null && e.getMessage().contains("Time slot already booked")) {
                userMessage = "That time slot was just taken. Please go back and choose a different time.";
            } else if (e.getMessage() != null && e.getMessage().contains("User not found")) {
                userMessage = "Please make sure you're logged in before confirming the booking.";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("step", "booking_error");
            response.put("message", userMessage);
            response.put("sessionId", session.getSessionId());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Generate available time slots for a doctor on a specific date.
     * Morning: 9:00-13:00, Afternoon: 14:00-20:00 (14:00-18:00 on Sunday)
     * Filters out already booked slots and past times if date is today.
     */
    private List<String> generateAvailableSlots(String doctorId, LocalDate date) {
        // Fetch existing booked appointments for this doctor on this date
        List<String> bookedTimes = new ArrayList<>();
        try {
            List<AppointmentResponseDTO> existing = appointmentService.getAppointmentsByDoctorAndDate(
                    Long.parseLong(doctorId), date);
            for (AppointmentResponseDTO apt : existing) {
                if (apt.getAppointmentTime() != null && "BOOKED".equals(apt.getStatus())) {
                    String time = apt.getAppointmentTime().substring(11, 16);
                    bookedTimes.add(time);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not fetch booked slots: " + e.getMessage());
        }

        boolean isToday = date.equals(LocalDate.now());
        int currentHour = isToday ? LocalDateTime.now().getHour() : 0;
        int currentMinute = isToday ? LocalDateTime.now().getMinute() : 0;
        boolean isSunday = date.getDayOfWeek().getValue() == 7;

        List<String> slots = new ArrayList<>();

        // Morning session: 9:00 - 13:00
        for (int hour = 9; hour <= 13; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                if (hour == 13 && minute > 0) break;
                if (isToday && (hour < currentHour || (hour == currentHour && minute <= currentMinute))) continue;
                String timeStr = String.format("%02d:%02d", hour, minute);
                if (!bookedTimes.contains(timeStr)) {
                    slots.add(timeStr);
                }
            }
        }

        // Afternoon session: 14:00 - 20:00 (or 18:00 on Sunday)
        int afternoonEnd = isSunday ? 18 : 20;
        for (int hour = 14; hour <= afternoonEnd; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                if (hour == afternoonEnd && minute > 0) break;
                if (isToday && (hour < currentHour || (hour == currentHour && minute <= currentMinute))) continue;
                String timeStr = String.format("%02d:%02d", hour, minute);
                if (!bookedTimes.contains(timeStr)) {
                    slots.add(timeStr);
                }
            }
        }

        return slots;
    }

    private boolean containsSymptomKeywords(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        return SYMPTOM_KEYWORDS.stream().anyMatch(keyword -> lowerMessage.contains(keyword.toLowerCase()));
    }

    private ResponseEntity<?> handleSymptomResponse(String aiResponse, Double userLat, Double userLng) {
        try {
            String jsonContent = extractJson(aiResponse);
            if (jsonContent == null) {
                return returnAsNormalText(aiResponse);
            }

            Map<String, Object> parsed = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {
            });

            if (!"specialization_match".equals(parsed.get("type"))) {
                return returnAsNormalText(aiResponse);
            }

            @SuppressWarnings("unchecked")
            List<String> specializations = (List<String>) parsed.get("specializations");
            if (specializations == null || specializations.isEmpty()) {
                specializations = Arrays.asList("General Medicine");
            }

            List<String> normalizedSpecs = normalizeSpecializations(specializations);
            System.out.println("Normalized specializations: " + normalizedSpecs);

            // Fetch hospitals using Service (which calculates distance if lat/lng are
            // provided)
            List<ClinicSummaryDTO> clinics = clinicService.getFilteredClinics(null, normalizedSpecs, null, userLat,
                    userLng);

            // Sort by distance if user location is available
            if (userLat != null && userLng != null) {
                clinics = clinics.stream()
                        .sorted(Comparator
                                .comparingDouble(c -> c.getDistance() != null ? c.getDistance() : Double.MAX_VALUE))
                        .limit(MAX_HOSPITAL_RESULTS)
                        .collect(Collectors.toList());
            } else {
                clinics = clinics.stream()
                        .limit(MAX_HOSPITAL_RESULTS)
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> hospitalList = new ArrayList<>();
            for (ClinicSummaryDTO clinic : clinics) {
                Map<String, Object> hospital = new HashMap<>();
                hospital.put("id", clinic.getClinicId());
                hospital.put("name", clinic.getName());
                hospital.put("imageUrl", clinic.getImageUrl() != null ? clinic.getImageUrl() : "");
                hospital.put("city", clinic.getCity());
                hospital.put("rating", clinic.getRating() != null ? clinic.getRating() : 0.0);
                hospital.put("address", clinic.getAddress() != null ? clinic.getAddress() : "");
                hospital.put("latitude", clinic.getLatitude());
                hospital.put("longitude", clinic.getLongitude());
                hospital.put("distance", clinic.getDistance());
                hospitalList.add(hospital);
            }

            // Create session to persist symptom data
            ChatSession session = new ChatSession();
            session.setSessionId(UUID.randomUUID().toString());
            session.setSymptom((String) parsed.get("symptom"));
            session.setSpecialization(normalizedSpecs.get(0));
            session.setCurrentStep("symptom_collected");
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            session.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            chatSessionService.save(session);

            Map<String, Object> result = new HashMap<>();
            result.put("type", "symptom_explanation");
            result.put("symptom", parsed.get("symptom"));
            result.put("inferredIssue", parsed.get("inferred_issue"));
            result.put("specializations", specializations);
            result.put("confidence", parsed.get("confidence"));
            result.put("disclaimer", parsed.get("disclaimer") != null
                    ? parsed.get("disclaimer")
                    : "This is general guidance, not a diagnosis. Please consult a doctor.");
            result.put("reply", buildSymptomExplanation(parsed));
            result.put("sessionId", session.getSessionId());
            result.put("step", "symptom_explanation");
            result.put("specialty", normalizedSpecs.get(0));
            result.put("hospitals", hospitalList);
            result.put("hospitalCount", clinics.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("Failed to parse symptom JSON: " + e.getMessage());
            return returnAsNormalText(aiResponse);
        }
    }

    private String extractJson(String text) {
        if (text == null)
            return null;
        text = text.trim();
        if (text.startsWith("{")) {
            int lastBrace = text.lastIndexOf("}");
            if (lastBrace > 0)
                return text.substring(0, lastBrace + 1);
        }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start)
            return text.substring(start, end + 1);
        return null;
    }

    private List<String> normalizeSpecializations(List<String> specializations) {
        return specializations.stream()
                .map(spec -> {
                    String lower = spec.toLowerCase().trim();
                    for (String valid : VALID_SPECIALIZATIONS) {
                        if (valid.equals(lower) || valid.replace(" ", "").equals(lower.replace(" ", "")))
                            return valid;
                    }
                    if (lower.contains("general") || lower.contains("medicine"))
                        return "general medicine";
                    if (lower.equals("ent") || lower.contains("ear") || lower.contains("nose")
                            || lower.contains("throat"))
                        return "ent";
                    return "general medicine";
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private String buildSymptomExplanation(Map<String, Object> parsed) {
        // Prefer the AI-generated natural response
        String naturalResponse = (String) parsed.get("natural_response");
        if (naturalResponse != null && !naturalResponse.isBlank()) {
            return naturalResponse + "\n\nThis is general guidance, not a diagnosis. Please consult a doctor.";
        }

        // Fallback: build a natural response from structured data
        String symptom = (String) parsed.get("symptom");
        String issue = (String) parsed.get("inferred_issue");

        StringBuilder sb = new StringBuilder();
        sb.append("I'm sorry you're dealing with that. ");
        if (symptom != null && issue != null) {
            sb.append(symptom).append(" is often related to ").append(issue.toLowerCase()).append(".");
        } else if (symptom != null) {
            sb.append("Let me help you with your ").append(symptom.toLowerCase()).append(".");
        } else {
            sb.append("Let me help you find the right care.");
        }
        sb.append("\n\nThis is general guidance, not a diagnosis. Please consult a doctor.");
        return sb.toString();
    }

    private ChatSession resolveSession(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            Optional<ChatSession> existing = chatSessionService.findBySessionId(sessionId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Optional<ChatSession> recent = chatSessionService.findLatest();
        return recent.orElse(null);
    }

    private ResponseEntity<?> showHospitalsFromSession(ChatSession session, Double userLat, Double userLng) {
        List<String> specs = session.getSpecialization() != null
                ? List.of(session.getSpecialization())
                : List.of();

        List<ClinicSummaryDTO> clinics = clinicService.getFilteredClinics(null, specs, null, userLat, userLng);

        if (userLat != null && userLng != null) {
            clinics = clinics.stream()
                    .sorted(Comparator
                            .comparingDouble(c -> c.getDistance() != null ? c.getDistance() : Double.MAX_VALUE))
                    .limit(MAX_HOSPITAL_RESULTS)
                    .collect(Collectors.toList());
        } else {
            clinics = clinics.stream()
                    .limit(MAX_HOSPITAL_RESULTS)
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> hospitalList = new ArrayList<>();
        for (ClinicSummaryDTO clinic : clinics) {
            Map<String, Object> hospital = new HashMap<>();
            hospital.put("id", clinic.getClinicId());
            hospital.put("name", clinic.getName());
            hospital.put("imageUrl", clinic.getImageUrl() != null ? clinic.getImageUrl() : "");
            hospital.put("city", clinic.getCity());
            hospital.put("rating", clinic.getRating() != null ? clinic.getRating() : 0.0);
            hospital.put("address", clinic.getAddress() != null ? clinic.getAddress() : "");
            hospital.put("latitude", clinic.getLatitude());
            hospital.put("longitude", clinic.getLongitude());
            hospital.put("distance", clinic.getDistance());
            hospitalList.add(hospital);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "hospitals");
        response.put("step", "hospital_selection");
        response.put("specialty", session.getSpecialization());
        response.put("sessionId", session.getSessionId());
        response.put("hospitals", hospitalList);
        response.put("reply", "Here are " + hospitalList.size() + " hospital(s) that may help:");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> returnAsNormalText(String content) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "text");
        result.put("reply", content);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<?> handleNearbySearch(Double lat, Double lng) {
        List<ClinicSummaryDTO> clinics = clinicService.getFilteredClinics(null, null, null, lat, lng);
        List<ClinicSummaryDTO> sorted = clinics.stream()
                .sorted(Comparator.comparingDouble(c -> c.getDistance() != null ? c.getDistance() : Double.MAX_VALUE))
                .limit(MAX_HOSPITAL_RESULTS)
                .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            return returnAsNormalText("No hospitals found near your current location.");
        }

        List<Map<String, Object>> hospitalList = new ArrayList<>();
        for (ClinicSummaryDTO clinic : sorted) {
            Map<String, Object> hospital = new HashMap<>();
            hospital.put("id", clinic.getClinicId());
            hospital.put("name", clinic.getName());
            hospital.put("imageUrl", clinic.getImageUrl() != null ? clinic.getImageUrl() : "");
            hospital.put("city", clinic.getCity());
            hospital.put("rating", clinic.getRating() != null ? clinic.getRating() : 0.0);
            hospital.put("address", clinic.getAddress() != null ? clinic.getAddress() : "");
            hospital.put("latitude", clinic.getLatitude());
            hospital.put("longitude", clinic.getLongitude());
            hospital.put("distance", clinic.getDistance());
            hospitalList.add(hospital);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "hospitals");
        response.put("hospitals", hospitalList);
        response.put("reply", "Here are the hospitals closest to your location:");
        response.put("step", "hospital_selection");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleHospitalCitySearch(String placeName) {
        // Use ClinicService to filter by city
        List<ClinicSummaryDTO> clinics = clinicService.getFilteredClinics(placeName, null, null, null, null);

        if (clinics.isEmpty()) {
            // Fuzzy search
            List<String> allCities = clinicService.getAllCities();
            List<String> suggestions = allCities.stream()
                    .filter(city -> calculateLevenshteinDistance(placeName.toLowerCase(), city.toLowerCase()) <= 3)
                    .sorted(Comparator.comparingInt(
                            city -> calculateLevenshteinDistance(placeName.toLowerCase(), city.toLowerCase())))
                    .limit(4)
                    .collect(Collectors.toList());

            StringBuilder reply = new StringBuilder("Sorry, couldn't find any hospitals in " + placeName + ".");
            if (!suggestions.isEmpty()) {
                reply.append(" Did you mean: ").append(String.join(", ", suggestions)).append("?");
            } else {
                reply.append(" Try searching for a different city.");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("type", "text");
            response.put("reply", reply.toString());
            return ResponseEntity.ok(response);
        }

        List<ClinicSummaryDTO> limitedClinics = clinics.stream()
                .limit(MAX_HOSPITAL_RESULTS)
                .collect(Collectors.toList());

        List<Map<String, Object>> hospitalList = new ArrayList<>();
        for (ClinicSummaryDTO clinic : limitedClinics) {
            Map<String, Object> hospital = new HashMap<>();
            hospital.put("id", clinic.getClinicId());
            hospital.put("name", clinic.getName());
            hospital.put("imageUrl", clinic.getImageUrl() != null ? clinic.getImageUrl() : "");
            hospital.put("city", clinic.getCity());
            hospital.put("rating", clinic.getRating() != null ? clinic.getRating() : 0.0);
            hospital.put("address", clinic.getAddress() != null ? clinic.getAddress() : "");
            hospital.put("latitude", clinic.getLatitude());
            hospital.put("longitude", clinic.getLongitude());
            hospitalList.add(hospital);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "hospitals");
        response.put("hospitals", hospitalList);
        response.put("reply", "Found " + limitedClinics.size() + " hospital(s) in " + placeName + ":");
        return ResponseEntity.ok(response);
    }

    private String getLanguageName(String langCode) {
        Map<String, String> languageNames = new HashMap<>();
        languageNames.put("en", "English");
        languageNames.put("hi", "Hindi");
        // ... (other languages can be added as needed, keeping it short)
        return languageNames.getOrDefault(langCode, "English");
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    dp[i][j] = j;
                else if (j == 0)
                    dp[i][j] = i;
                else
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1));
            }
        }
        return dp[s1.length()][s2.length()];
    }

    /** Prefix the name with "Dr." only if it doesn't already start with it. */
    private String formatDoctorName(String name) {
        if (name == null) return "Doctor";
        String trimmed = name.trim();
        if (trimmed.startsWith("Dr.") || trimmed.startsWith("Dr ")) {
            return trimmed;
        }
        return "Dr. " + trimmed;
    }
}
