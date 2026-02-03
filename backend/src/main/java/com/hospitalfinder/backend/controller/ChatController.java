package com.hospitalfinder.backend.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.hospitalfinder.backend.dto.ChatRequest;
import com.hospitalfinder.backend.dto.ClinicSummaryDTO;
import com.hospitalfinder.backend.service.ClinicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "*")
@RequiredArgsConstructor
public class ChatController {

    @Value("${groq.api.key:}")
    private String apiKey;

    private final ClinicService clinicService;

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
            {"type":"specialization_match","symptom":"<brief symptom summary>","inferred_issue":"<simple non-diagnostic explanation>","specializations":["<spec1>","<spec2>"],"confidence":"low|medium|high","disclaimer":"This is not a medical diagnosis. Please consult a qualified doctor."}

            RULES:
            - Choose 1-3 specializations ONLY from: Cardiology, Orthopedics, Pediatrics, Dermatology, Neurology, Gynecology, ENT, General Medicine, Surgery, Ophthalmology, Pulmonology, Oncology
            - If unsure, include "General Medicine"
            - Do NOT diagnose or prescribe
            - Use simple, non-alarming language
            - Keep "inferred_issue" brief and general
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
                    return handleHospitalCitySearch(placeName);
                }
            }
        }

        // Check if message contains symptom keywords
        boolean containsSymptoms = containsSymptomKeywords(
                messages != null && !messages.isEmpty()
                        ? messages.get(messages.size() - 1).getContent()
                        : "");

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

            Map<String, Object> result = new HashMap<>();
            result.put("type", "specialization_match");
            result.put("symptom", parsed.get("symptom"));
            result.put("inferredIssue", parsed.get("inferred_issue"));
            result.put("specializations", specializations);
            result.put("confidence", parsed.get("confidence"));
            result.put("disclaimer", parsed.get("disclaimer") != null
                    ? parsed.get("disclaimer")
                    : "This is not a medical diagnosis. Please consult a qualified doctor.");
            result.put("hospitals", hospitalList);
            result.put("reply", buildSymptomReplyMessage(parsed, clinics.size()));

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

    private String buildSymptomReplyMessage(Map<String, Object> parsed, int hospitalCount) {
        String symptom = (String) parsed.get("symptom");
        String issue = (String) parsed.get("inferred_issue");
        @SuppressWarnings("unchecked")
        List<String> specs = (List<String>) parsed.get("specializations");

        StringBuilder sb = new StringBuilder();
        sb.append("Based on your symptoms (").append(symptom != null ? symptom : "described issue").append("), ");
        sb.append("this could be related to ").append(issue != null ? issue : "a health concern").append(". ");
        sb.append("I recommend consulting a ").append(String.join(" or ", specs)).append(" specialist. ");

        if (hospitalCount > 0) {
            sb.append("Here are ").append(hospitalCount).append(" hospital(s) that may help:");
        } else {
            sb.append("Unfortunately, no matching hospitals were found in our database.");
        }
        return sb.toString();
    }

    private ResponseEntity<?> returnAsNormalText(String content) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "text");
        result.put("reply", content);
        return ResponseEntity.ok(result);
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
