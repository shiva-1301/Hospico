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
import com.hospitalfinder.backend.dto.ChatActionRequest;
import com.hospitalfinder.backend.dto.ChatRequest;
import com.hospitalfinder.backend.dto.ClinicResponseDTO;
import com.hospitalfinder.backend.dto.ClinicSummaryDTO;
import com.hospitalfinder.backend.entity.ChatSession;
import com.hospitalfinder.backend.entity.Doctor;
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

    // Symptom analysis prompt (only injected when symptoms detected)
    private static final String SYMPTOM_ANALYSIS_PROMPT = """
            IMPORTANT: The user is describing health symptoms. You must respond ONLY with valid JSON in this exact format:
            {"type":"specialization_match","symptom":"<brief symptom summary>","inferred_issue":"<simple non-diagnostic explanation>","common_causes":["<cause1>","<cause2>","<cause3>"],"specializations":["<spec1>","<spec2>"],"confidence":"low|medium|high","disclaimer":"This is not a medical diagnosis. Please consult a qualified doctor."}

            RULES:
            - symptom: 2-5 word summary (e.g., "Slight headache", "Ear pain")
            - inferred_issue: 2-3 words max, non-diagnostic (e.g., "Tension headache")
            - common_causes: 3 SPECIFIC causes for THIS symptom
            - Choose 1-3 specializations ONLY from: Cardiology, Orthopedics, Pediatrics, Dermatology, Neurology, Gynecology, ENT, General Medicine, Surgery, Ophthalmology, Pulmonology, Oncology
            - If unsure, include "General Medicine"
            - Do NOT diagnose or prescribe
            - Use simple, non-alarming language
            - Respond ONLY with the JSON, nothing else
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
        boolean wantsHospitals = lowerMessage.contains("show") || lowerMessage.contains("hospitals")
                || lowerMessage.contains("nearby") || lowerMessage.contains("find hospital");

        boolean wantsBooking = lowerMessage.contains("book") &&
                (lowerMessage.contains("appointment") || lowerMessage.contains("doctor")
                        || lowerMessage.contains("visit"));

        if (wantsBooking) {
            return returnAsNormalText(
                    "📅 Appointment booking feature will be available soon!\n\n" +
                            "For now, you can:\n" +
                            "→ Find nearby hospitals\n" +
                            "→ View doctor information\n" +
                            "→ Contact hospitals directly\n\n" +
                            "Is there anything else I can help you with?");
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
            // Normal healthcare assistant prompt
            systemPrompt = "You are a helpful healthcare assistant. Maintain conversational context. Provide general possible causes for symptoms. Limit responses to 4-6 lines. Do NOT diagnose. Always advise consulting a doctor. If user asks about hospitals near a place, tell them to use the format 'hospital near [city name]' for better results.";
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

        Map<String, Object> response = new HashMap<>();
        response.put("step", "doctor_selection");
        response.put("message", "Great! Please select a doctor from " + clinic.getName());
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
        session.setCurrentStep("doctor_selected");
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionService.save(session);

        Map<String, Object> doctorDetails = new HashMap<>();
        doctorDetails.put("id", doctor.getId());
        doctorDetails.put("name", doctor.getName());
        doctorDetails.put("specialization", doctor.getSpecialization());
        doctorDetails.put("qualifications", doctor.getQualifications());
        doctorDetails.put("experience", doctor.getExperience());
        doctorDetails.put("imageUrl", doctor.getImageUrl() != null ? doctor.getImageUrl() : "");

        Map<String, Object> response = new HashMap<>();
        response.put("step", "doctor_selected");
        response.put("message", "Here is Dr. " + doctor.getName()
                + "'s information. You can contact the hospital directly to book an appointment.");
        response.put("doctor", doctorDetails);
        response.put("sessionId", session.getSessionId());
        return ResponseEntity.ok(response);
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
            session.setCurrentStep("symptom_explanation");
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
                    : "This is not a medical diagnosis. Please consult a qualified doctor.");
            result.put("reply", buildSymptomExplanation(parsed));
            result.put("sessionId", session.getSessionId());
            result.put("step", "symptom_explanation");
            result.put("specialty", normalizedSpecs.get(0));
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
        String symptom = (String) parsed.get("symptom");
        String issue = (String) parsed.get("inferred_issue");

        @SuppressWarnings("unchecked")
        List<String> causes = (List<String>) parsed.get("common_causes");
        if (causes == null || causes.isEmpty()) {
            causes = Arrays.asList(
                    "Minor irritation or inflammation",
                    "Stress or lifestyle factors",
                    "Underlying medical conditions");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Based on your symptoms (").append(symptom != null ? symptom : "described issue").append("), ");
        sb.append("this could be related to ").append(issue != null ? issue : "a health concern").append(".\n\n");
        sb.append("Common causes may include:\n");
        for (String cause : causes) {
            sb.append("• ").append(cause).append("\n");
        }
        sb.append("\n💡 If you'd like, I can:\n");
        sb.append("→ Show nearby hospitals");
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
}
