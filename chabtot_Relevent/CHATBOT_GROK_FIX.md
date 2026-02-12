# Chatbot Grok API Integration - Dynamic Symptom Causes

## Problem
The chatbot was showing static, generic causes for ALL symptoms instead of dynamic, symptom-specific information from the Grok API:
- **Issue**: All symptoms showed "null" for disease relationships
- **Issue**: Common causes were hardcoded: "Minor irritation", "Stress", "Underlying conditions"
- **Result**: Poor user experience - no personalization based on actual symptoms

## Solution Implemented
Integrated Grok API to provide dynamic, symptom-specific information:

### New Method: `getSymptomDetailsFromGrok(String symptom)`
- **Location**: [ChatController.java](backend/src/main/java/com/hospitalfinder/backend/controller/ChatController.java#L414)
- **Purpose**: Calls Grok API with user's symptom to get:
  - `inferred_issue`: What the symptom could be related to (e.g., "Ear infection", "Allergic reaction")
  - `common_causes`: 3 specific, symptom-related causes
  
### Updated Method: `buildSymptomExplanation(Map<String, Object> parsed)`
- **Location**: [ChatController.java](backend/src/main/java/com/hospitalfinder/backend/controller/ChatController.java#L500)
- **Changes**:
  - Calls `getSymptomDetailsFromGrok()` to fetch dynamic data
  - Uses Grok API data instead of hardcoded text
  - Falls back to defaults if API call fails
  - Formats response with dynamic causes

## Response Format (After Fix)
```
Based on your symptoms (slight headache), this could be related to Tension headache.

Common causes may include:
• Stress and tension in neck/shoulder muscles
• Dehydration
• Poor posture or prolonged screen time

💡 If you'd like, I can:
→ Show nearby hospitals
→ Help book an appointment
```

## API Flow
1. User describes symptom: "I have slight headache"
2. System detects symptom keywords and calls Grok API
3. Grok AI returns:
   - Symptom: "slight headache"
   - Inferred issue: "Tension headache" (dynamic)
   - Common causes: ["Stress...", "Dehydration", "Poor posture"] (dynamic)
4. Frontend displays dynamic response instead of static text

## Error Handling
- If Grok API call fails, falls back to generic causes:
  - "Minor irritation or inflammation"
  - "Stress or lifestyle factors"
  - "Underlying medical conditions"

## Grok API Configuration
- **Model**: mixtral-8x7b-32768
- **Temperature**: 0.1 (low for consistency)
- **Max Tokens**: 200
- **Endpoint**: https://api.groq.com/openai/v1/chat/completions
- **Auth**: Bearer token from GROQ_API_KEY environment variable

## Testing
Build the backend:
```bash
cd backend
mvn clean compile
```

The code compiles successfully without errors.

## Benefits
✅ **Personalized responses** - Each symptom gets specific causes
✅ **Real-time data** - Uses Grok API for current information
✅ **Better UX** - Users see relevant information
✅ **Resilient** - Falls back gracefully if API fails
✅ **Consistent format** - Maintains established response structure
