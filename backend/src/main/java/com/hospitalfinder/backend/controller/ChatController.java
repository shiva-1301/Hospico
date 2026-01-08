package com.hospitalfinder.backend.controller;

import com.hospitalfinder.backend.dto.ChatRequest;
import com.hospitalfinder.backend.entity.Clinic;
import com.hospitalfinder.backend.repository.ClinicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "*") // Allow all origins - already secured by CorsConfig
public class ChatController {

    @Value("${groq.api.key:}")
    private String apiKey;

    @Autowired
    private ClinicRepository clinicRepository;

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

    /**
     * Check if the message contains symptom-related keywords
     */
    private boolean containsSymptomKeywords(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        return SYMPTOM_KEYWORDS.stream().anyMatch(keyword -> lowerMessage.contains(keyword.toLowerCase()));
    }

    /**
     * Handle symptom-based AI response - parse JSON safely and fetch hospitals
     */
    private ResponseEntity<?> handleSymptomResponse(String aiResponse, Double userLat, Double userLng) {
        try {
            // Clean the response - extract JSON if wrapped in text
            String jsonContent = extractJson(aiResponse);
            if (jsonContent == null) {
                // Couldn't extract JSON, return as normal text
                return returnAsNormalText(aiResponse);
            }

            // Parse JSON safely
            Map<String, Object> parsed = objectMapper.readValue(jsonContent,
                    new TypeReference<Map<String, Object>>() {
                    });

            // Validate it has the expected type
            if (!"specialization_match".equals(parsed.get("type"))) {
                return returnAsNormalText(aiResponse);
            }

            // Extract specializations and normalize
            @SuppressWarnings("unchecked")
            List<String> specializations = (List<String>) parsed.get("specializations");
            if (specializations == null || specializations.isEmpty()) {
                specializations = Arrays.asList("General Medicine");
            }

            // Normalize specializations for DB query
            List<String> normalizedSpecs = normalizeSpecializations(specializations);
            System.out.println("Normalized specializations: " + normalizedSpecs);

            // Fetch hospitals by specializations
            List<Clinic> clinics = clinicRepository.findBySpecializationsIn(normalizedSpecs);

            // Sort by distance if user location is available
            List<Clinic> sortedClinics;
            if (userLat != null && userLng != null) {
                System.out.println("Sorting hospitals by distance from user location: " + userLat + ", " + userLng);
                sortedClinics = clinics.stream()
                        .sorted((c1, c2) -> {
                            double dist1 = calculateDistance(userLat, userLng,
                                    c1.getLatitude() != null ? c1.getLatitude() : 0,
                                    c1.getLongitude() != null ? c1.getLongitude() : 0);
                            double dist2 = calculateDistance(userLat, userLng,
                                    c2.getLatitude() != null ? c2.getLatitude() : 0,
                                    c2.getLongitude() != null ? c2.getLongitude() : 0);
                            return Double.compare(dist1, dist2);
                        })
                        .limit(MAX_HOSPITAL_RESULTS)
                        .collect(Collectors.toList());
            } else {
                // Fallback: sort by rating if no location
                sortedClinics = clinics.stream()
                        .limit(MAX_HOSPITAL_RESULTS)
                        .collect(Collectors.toList());
            }

            // Build hospital cards with distance info
            List<Map<String, Object>> hospitalList = new ArrayList<>();
            for (Clinic clinic : sortedClinics) {
                Map<String, Object> hospital = new HashMap<>();
                hospital.put("id", clinic.getId());
                hospital.put("name", clinic.getName());
                hospital.put("imageUrl", clinic.getImageUrl() != null ? clinic.getImageUrl() : "");
                hospital.put("city", clinic.getCity());
                hospital.put("rating", clinic.getRating() != null ? clinic.getRating() : 0.0);
                hospital.put("address", clinic.getAddress() != null ? clinic.getAddress() : "");
                hospital.put("latitude", clinic.getLatitude());
                hospital.put("longitude", clinic.getLongitude());

                // Add distance if location is available
                if (userLat != null && userLng != null && clinic.getLatitude() != null
                        && clinic.getLongitude() != null) {
                    double distance = calculateDistance(userLat, userLng, clinic.getLatitude(), clinic.getLongitude());
                    hospital.put("distance", Math.round(distance * 10.0) / 10.0); // Round to 1 decimal
                }

                hospitalList.add(hospital);
            }

            // Build response
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
            result.put("reply", buildSymptomReplyMessage(parsed, sortedClinics.size()));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("Failed to parse symptom JSON: " + e.getMessage());
            // Fall back to normal text response
            return returnAsNormalText(aiResponse);
        }
    }

    /**
     * Extract JSON from possibly wrapped text
     */
    private String extractJson(String text) {
        if (text == null)
            return null;
        text = text.trim();

        // If it starts with {, assume it's JSON
        if (text.startsWith("{")) {
            int lastBrace = text.lastIndexOf("}");
            if (lastBrace > 0) {
                return text.substring(0, lastBrace + 1);
            }
        }

        // Try to find JSON in the text
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return null;
    }

    /**
     * Normalize specialization names to match database values
     */
    private List<String> normalizeSpecializations(List<String> specializations) {
        return specializations.stream()
                .map(spec -> {
                    String lower = spec.toLowerCase().trim();
                    // Find matching valid specialization
                    for (String valid : VALID_SPECIALIZATIONS) {
                        if (valid.equals(lower) ||
                                valid.replace(" ", "").equals(lower.replace(" ", ""))) {
                            return valid;
                        }
                    }
                    // Special case handling
                    if (lower.contains("general") || lower.contains("medicine")) {
                        return "general medicine";
                    }
                    if (lower.equals("ent") || lower.contains("ear") ||
                            lower.contains("nose") || lower.contains("throat")) {
                        return "ent";
                    }
                    // Default to general medicine if unrecognized
                    return "general medicine";
                })
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Build friendly reply message for symptom match
     */
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

    /**
     * Return AI response as normal text (fallback)
     */
    private ResponseEntity<?> returnAsNormalText(String content) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "text");
        result.put("reply", content);
        return ResponseEntity.ok(result);
    }

    /**
     * Handle explicit hospital city search
     */
    private ResponseEntity<?> handleHospitalCitySearch(String placeName) {
        List<Clinic> clinics = clinicRepository.findByCityIgnoreCase(placeName);

        if (clinics.isEmpty()) {
            // Fuzzy search for city names
            List<String> allCities = clinicRepository.findAllDistinctCities();
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

        // Limit results
        List<Clinic> limitedClinics = clinics.stream()
                .limit(MAX_HOSPITAL_RESULTS)
                .collect(Collectors.toList());

        // Build hospital cards response
        List<Map<String, Object>> hospitalList = new ArrayList<>();
        for (Clinic clinic : limitedClinics) {
            Map<String, Object> hospital = new HashMap<>();
            hospital.put("id", clinic.getId());
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

    // Helper method to map language codes to full language names
    private String getLanguageName(String langCode) {
        Map<String, String> languageNames = new HashMap<>();
        languageNames.put("en", "English");
        languageNames.put("hi", "Hindi");
        languageNames.put("te", "Telugu");
        languageNames.put("ta", "Tamil");
        languageNames.put("kn", "Kannada");
        languageNames.put("ml", "Malayalam");
        languageNames.put("mr", "Marathi");
        languageNames.put("gu", "Gujarati");
        languageNames.put("bn", "Bengali");
        languageNames.put("pa", "Punjabi");
        languageNames.put("or", "Odia");
        languageNames.put("as", "Assamese");
        languageNames.put("ur", "Urdu");
        languageNames.put("kok", "Konkani");
        languageNames.put("ks", "Kashmiri");
        languageNames.put("mni", "Manipuri");

        return languageNames.getOrDefault(langCode, "English");
    }

    /**
     * Calculate distance between two points using Haversine formula
     * 
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Earth radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * Calculate Levenshtein distance for fuzzy string matching
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1));
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }
}




