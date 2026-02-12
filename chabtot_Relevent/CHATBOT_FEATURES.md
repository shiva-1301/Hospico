# HospiCo HealthMate Chatbot - Feature Report

## Overview

The HealthMate Bot is an AI-powered health assistant integrated into the HospiCo healthcare platform. It provides intelligent symptom analysis, hospital recommendations, doctor selection, appointment booking, and voice interaction features - all within a conversational interface.

---

## Core Features

### 1. **AI-Powered Symptom Analysis & Health Assistance**
- **How it works**: Uses Gemini AI (Google's LLM) to analyze user-described symptoms
- **Two-Step Symptom Flow** (NEW):
  1. **Explanation First**: When symptoms detected, provides calm explanation of possible causes
  2. **User Choice**: Offers options to show hospitals or book appointment
  3. **No Forced Hospital Display**: Gives user control over next steps

- **Capabilities**:
  - Analyzes symptoms and suggests medical conditions
  - Provides helpful, non-alarming explanations of possible causes
  - Recommends appropriate medical specialties (ENT, Cardiology, Neurology, etc.)
  - Finds hospitals with matching specialties (only when user chooses)
  - Shows hospitals sorted by distance from user location
  - Provides health guidance and information
  - Maintains conversation history for contextual responses
  - Stores symptom data in session for deferred hospital lookup

- **Backend API**: `POST /api/chat`
  - Analyzes symptoms using Gemini AI
  - Maps conditions to medical specialties
  - **NEW**: Returns explanation with options instead of immediate hospital list
  - Stores symptom/specialty data in ChatSession
  - Detects when user wants hospitals ("show hospitals", "book appointment")
  - Retrieves stored session data to show relevant hospitals

- **Symptom Explanation Response**:
  ```json
  {
    "reply": "Based on your symptoms (Ear pain), this could be related to Ear infection.\n\nCommon causes may include:\n• Minor irritation or inflammation\n• Stress or lifestyle factors\n• Underlying medical conditions\n\n💡 If you'd like, I can:\n→ Show nearby hospitals\n→ Help book an appointment",
    "type": "symptom_explanation",
    "step": "symptom_explanation",
    "sessionId": "abc-123",
    "specialty": "ENT",
    "hospitalCount": 5
  }
  ```

- **Hospital Display Response** (when user chooses):
  ```json
  {
    "reply": "I recommend consulting a ENT specialist. Here are 5 hospital(s) that may help:",
    "type": "hospitals",
    "step": "hospital_selection",
    "specialty": "ENT",
    "hospitals": [...],
    "sessionId": "abc-123"
  }
  ```

- **Disclaimer**: "⚠️ This is not a medical diagnosis. Please consult a qualified doctor."

### 2. **Location-Based Hospital Search**
- **How it works**: Natural language queries to find nearby hospitals
- **Supported Queries**:
  - "hospitals near me"
  - "hospitals in [city name]"
  - "show me hospitals in hyderabad"
  - "find hospitals nearby"
- **Backend Processing**:
  - Extracts location from user query
  - Searches database using city/area names
  - Returns hospitals within specified radius
  - Sorts by distance from user coordinates
- **Backend API**: `POST /api/chat`
  - Pattern matching for location queries
  - Geocoding support for city names
  - Distance calculation using Haversine formula
- **Response**: Returns hospital cards with distance, ratings, and location

### 3. **Complete Appointment Booking Flow**
- **Multi-Step Conversation Process**:

#### Step 1: Hospital Selection
- Displays hospital cards after symptom analysis or search
- Shows: Name, rating, distance, address, image
- User clicks "Select Hospital" button
- **Backend**: Retrieves hospital details from database

#### Step 2: Doctor Selection
- Shows available doctors for selected specialty at chosen hospital
- Displays: Doctor name, specialty, qualifications (MBBS, MD, etc.)
- Includes doctor profile images
- **Backend API**: Fetches doctors filtered by hospital and specialty
- **Function**: `getDoctorsByHospitalAndSpecialty()`

#### Step 3: Date Selection
- Calendar interface for appointment date
- Validates future dates only
- Default: Tomorrow's date
- **Frontend**: Date picker component with validation

#### Step 4: Time Slot Selection
- Shows available time slots (30-minute intervals)
- Range: 09:00 - 20:00
- Visual slot buttons for easy selection
- **Backend**: Checks doctor availability
- **Function**: `getAvailableTimeSlots(doctorId, date)`

#### Step 5: Patient Details Form
- **Required Fields**:
  - Name
  - Age
  - Gender (Male/Female/Other)
  - Phone number
  - Email address
- **Optional**: Reason for visit / additional notes
- Auto-fills from user profile if logged in
- **Frontend**: Form validation before submission

#### Step 6: Booking Confirmation
- Reviews all booking details
- User confirms booking
- **Backend API**: `POST /api/appointments`
  - Creates appointment record
  - Links patient, doctor, hospital
  - Stores date, time, status
- **Success Response**:
  ```json
  {
    "reply": "✅ Booking Confirmed!",
    "type": "booking_success",
    "appointmentId": 123,
    "bookingDetails": {...}
  }
  ```

### 4. **Voice Input (Speech-to-Text)**
- Microphone button for hands-free input
- Uses Web Speech Recognition API
- Supports English language input
- Visual feedback with pulsing red microphone when active
- **Implementation**: `webkitSpeechRecognition` or `SpeechRecognition`

### 5. **Voice Output (Text-to-Speech)**
- Speaker icon below each bot message
- Reads bot responses aloud
- Multi-language support based on Google Translate selection
- **Supported Languages**:
  - English (en-US)
  - Hindi (hi-IN)
  - Telugu (te-IN)
  - Tamil (ta-IN)
  - Kannada (kn-IN)
  - Malayalam (ml-IN)
  - Marathi (mr-IN)
  - Gujarati (gu-IN)
  - Bengali (bn-IN)
  - Punjabi (pa-IN)
  - Odia (or-IN)
  - Assamese (as-IN)
  - Urdu (ur-IN)
- **Implementation**: `window.speechSynthesis.speak()`

---

## UI/UX Features

### 5. **Modern Chat Interface**
- Floating chat widget (bottom-right corner)
- Glassmorphic design with backdrop blur
- Dark mode support
- Smooth Framer Motion animations
- Responsive design (mobile-friendly)
- Fullscreen on mobile, popup window on desktop (380x600px)

### 6. **Embed Mode**
- Can be embedded in external websites via `/embed` route
- Opens automatically in embed mode
- No toggle button (always open)
- Works inside iframes

### 7. **Authentication Gating**
- Chatbot requires user login
- Shows "Restricted Access" popup for unauthenticated users
- Prompts to Login or Sign Up
- Auto-hides after 5 seconds

---

## Technical Implementation

### Backend APIs & Functions

#### 1. Chat Controller (`ChatController.java`)
**Main Endpoint**: `POST /api/chat`

**Request Body**:
```json
{
  "messages": [
    { "role": "user", "content": "I have ear pain" }
  ],
  "language": "en",
  "userLat": 17.385044,
  "userLng": 78.486671
}
```

**Key Functions**:
- `chat()` - Main chat handler with user intent detection
- `handleSymptomResponse()` - AI symptom analysis using Gemini
- `buildSymptomExplanation()` - **NEW**: Creates helpful symptom explanations
- `showHospitalsFromSession()` - **NEW**: Retrieves hospitals from stored session
- `extractSymptoms()` - Extract symptoms from text
- `mapToSpecialty()` - Maps conditions to medical specialties
- `findNearbyHospitals()` - Location-based hospital search
- `calculateDistance()` - Haversine formula for distance calculation

**NEW Logic Flow**:
1. Detects if message contains symptoms
2. If yes → Returns explanation with options (stores data in session)
3. Detects if user wants hospitals ("show", "book", etc.)
4. If yes → Retrieves recent session and displays hospitals
5. Otherwise → Normal AI chat response

**Response Types**:
1. **Symptom Analysis**:
   ```json
   {
     "reply": "Based on your symptoms...",
     "type": "hospitals",
     "step": "hospital_selection",
     "specialty": "ENT",
     "hospitals": [
       {
         "id": 1,
         "name": "ANSH Hospital",
         "city": "Hyderabad",
         "rating": 4.3,
         "distance": 18.4,
         "specialties": ["ENT", "Cardiology"]
       }
     ]
   }
   ```

2.**NEW**: Stores symptom data in session for deferred lookup
- Provides relevant follow-up suggestions

### 2. **Natural Language Understanding**
- Understands varied symptom descriptions
- Recognizes location queries in multiple formats
- **NEW**: Detects user intent ("show hospitals", "book appointment")
- Handles casual conversation naturally

### 3. **Smart Specialty Mapping**
- AI maps symptoms to medical specialties
- Handles complex multi-symptom scenarios
- Suggests multiple specialties when appropriate

### 4. **Distance-Based Recommendations**
- Calculates real-time distance from user location
- Sorts hospitals by proximity
- Shows distance in kilometers

### 5. **Validation & Error Handling**
- Validates appointment dates (future only)
- Checks doctor availability
- Validates patient information format
- Provides helpful error messages

### 6. **Patient-Centric Symptom Flow** (NEW)
- Explains symptoms before overwhelming with hospital options
- Gives user control over next steps
- Provides calm, non-alarming explanations
- Stores context for seamless continuation
- Respects user autonomy in healthcare decisiondations**
- Calculates real-time distance from user location
- Sorts hospitals by proximity
- Shows distance in kilometers

### 5. **Validation & Error Handling**
- Validates appointment dates (future only)
- Checks doctor availability
- Validates patient information format
- Provides helpful error messages

---

## Current Limitations

1. **Persistent chat history** - Messages reset on page reload (no database storage)
2. **Typing indicators** - Only shows "Thinking..." loader
3. **File/image upload** - Text-only input (no prescription image analysis)
4. **Speech recognition** - English only for voice input
5. **Offline support** - Requires internet connection
6. **Payment integration** - No online payment for appointments
7. **Real-time availability** - Time slots not synced with actual bookings
   {
     "reply": "I can help you find hospitals...",
     "type": "text"
   }
   ```

#### 2. Hospital Service (`HospitalService.java`)
**Functions**:
- `findBySpecialtiesContaining()` - Get hospitals by specialty
- `findByCityContainingIgnoreCase()` - Search by city
- `calculateDistance(lat1, lng1, lat2, lng2)` - Distance calculation

#### 3. Doctor Service (`DoctorService.java`)
**Functions**:
- `getDoctorsByHospitalAndSpecialty(hospitalId, specialty)` - Filter doctors
- `getDoctorsByHospital(hospitalId)` - All doctors at hospital
- `getAvailableTimeSlots(doctorId, date)` - Check availability

#### 4. Appointment Service (`AppointmentService.java`)
**Endpoint**: `POST /api/appointments`
**Functions**:
- `createAppointment()` - Book appointment
- `getPatientAppointments(patientId)` - User's bookings
- `cancelAppointment(appointmentId)` - Cancel booking

**Request Body**:
```json
{
  "hospitalId": 1,
  "doctorId": 5,
  "patientName": "Shiva",
  "patientAge": 21,
  "patientGender": "Male",
  "patientPhone": "9375897240",
  "patientEmail": "shiva@hospico.com",
  "appointmentDate": "2026-02-25",
  "appointmentTime": "16:00",
  "reasonForVisit": "Ear pain consultation"
}
```

### Frontend Implementation

#### State Management
- **React useState**: Chat messages, conversation flow, selected items
- **Redux**: Authentication state, user profile
- **useRef**: Speech recognition instance
- **Local State Variables**:
  - `messages` - Chat history
  - `selectedHospital` - Current hospital selection
  SYMPTOM_EXPLANATION: 'symptom_explanation',  // NEW
  - `selectedDoctor` - Current doctor selection
  - `selectedDate` - Appointment date
  - `selectedTime` - Time slot
  - `currentStep` - Conversation flow step

#### Conversation Flow Steps
```javascript
const STEPS = {
  INITIAL: 'initial_query',
  HOSPITAL_SELECTION: 'hospital_selection',
  DOCTOR_SELECTION: 'doctor_selection',
  DATE_SELECTION: 'date_selection',
  TIME_SELECTION: 'time_selection',
  PATIENT_DETAILS: 'patient_details',
  BOOKING_CONFIRMATION: 'booking_confirmation'
};
```

#### Key Functions (`ChatWidget.tsx`)
- `handleSendMessage()` - Send user message to backend
- `handleHospitalSelect()` - Process hospital selection
- `handleDoctorSelect()` - Process doctor selection
- `handleDateSelect()` - Process date selection
- `handleTimeSelect()` - Process time slot selection
- `handleBookingSubmit()` - Submit appointment booking
- `startListening()` - Activate voice input
- `stopListening()` - Stop voice recognition
- `speakMessage()` - Text-to-speech output

### Database Schema

**Tables Used**:
- `hospitals` - Hospital records with location, specialties
- `doctors` - Doctor profiles with specialty, qualifications
- `appointments` - Booking records
- `patients` - Patient information
3. ✅ **Symptom Explanation Flow** - **NEW**: Explains causes before showing hospitals
4. ✅ **User Choice Options** - **NEW**: Buttons for show hospitals/book appointment
5. ✅ **Session-Based Context** - **NEW**: Stores symptom data for deferred lookup
6. ✅ **Hospital Search** - Location and specialty-based search
7. ✅ **Doctor Selection** - Browse and select doctors
8. ✅ **Voice Input/Output** - Speech recognition and text-to-speech
9. ✅ **Multi-step Conversations** - Guided booking workflow
10. ✅ **Specialty Mapping** - Smart specialty recommendations
- `@google/generative-ai` - Gemini AI integration
- `axios` - HTTP requests
- `react-redux` - State management
- Web Speech API - Voice features

---

## Component Structure

```
ChaCompleted Features ✅

1. ✅ **Appointment Booking** - Complete booking flow from chat
2. ✅ **Symptom Analysis** - AI-powered symptom checking
3. ✅ **Hospital Search** - Location and specialty-based search
4. ✅ **Doctor Selection** - Browse and select doctors
5. ✅ **Voice Input/Output** - Speech recognition and text-to-speech
6. ✅ **Multi-step Conversations** - Guided booking workflow
7. ✅ **Specialty Mapping** - Smart specialty recommendations

## Future Enhancement Ideas

1. **Prescription Upload & Analysis** - Upload and AI-analyze prescriptions
2. **Live Doctors

### Frontend
- **Main Component**: `hospico-front (NEW Flow)
```
User: "I'm having ear pain"
Bot: Analyzes symptoms → Provides explanation:
     "Based on your symptoms (Ear pain), this could be related to Ear infection.
     
     Common causes may include:
     • Minor irritation or inflammation
     • Stress or lifestyle factors
     • Underlying medical conditions
     
     💡 If you'd like, I can:
     → Show nearby hospitals
     → Help book an appointment"
     
     [Shows 2 buttons: "🏥 Show nearby hospitals" | "📅 Book an appointment"]

User: Clicks "Show nearby hospitals" (or types "show hospitals")
Bot: Shows 2 hospitals with ENT specialists sorted by distance

### Backend
- **Chat Controller**: `backend/src/main/java/com/hospitalfinder/backend/controller/ChatController.java`
- **Hospital Service**: `backend/src/main/java/com/hospitalfinder/backend/service/HospitalService.java`
- **Doctor Service**: `backend/src/main/java/com/hospitalfinder/backend/service/DoctorService.java`
- **Appointment Service**: `backend/src/main/java/com/hospitalfinder/backend/service/AppointmentService.java`

### Configuration
- **Gemini API**: Configured in `application.yml`
- **Database**: PostgreSQL via Spring Data JPA
- **Base URL**: `http://localhost:8080/api`

---

## Usage Examples

### Example 1: Symptom-Based Booking
```
User: "I'm having ear pain"
Bot: Analyzes symptoms → Suggests ENT specialist → Shows 2 hospitals
User: Selects "ANSH Hospital"
Bot: Shows ENT doctors at ANSH Hospital
User: Selects "Dr. Kavita Chowdary"
Bot: Shows date picker
User: Selects "25-02-2026"
Bot: Shows time slots
User: Selects "16:00"
Bot: Shows patient details form
User: Fills form and confirms
Bot: "✅ Appointment booked successfully!"
```

### Example 2: Location-Based Search
```
User: "hospitals near me"
Bot: Shows 5 nearby hospitals with distance
User: Clicks hospital card
→ Navigates to hospital profile page
```

### Example 3: City Search
```
User: "show hospitals in hyderabad"
Bot: "Found 5 hospitals in hyderabad" + hospital cards
```

---

*Last Updated: February 4, 2026ck access to ambulance booking
│   └── Voice output button
├── Input Area
│   ├── Text input
│   ├── Microphone button
│   └── Send button
└── Auth Prompt (overlay for unauthenticated users)
```

---

## Current Limitations

1. **No persistent chat history** - Messages reset on page reload
2. **No typing indicators** - Only "Thinking..." loader
3. **No file/image upload** - Text-only input
4. **No appointment booking via chat** - Must navigate to hospital page
5. **Speech recognition** - English only (input)
6. **Offline support** - None (requires internet)

---

## Future Enhancement Ideas

1. **Appointment Booking** - Book appointments directly from chat
2. **Symptom Checker** - Interactive symptom questionnaire
3. **Prescription Upload** - Upload and analyze prescriptions
4. **Doctor Chat** - Connect with real doctors
5. **Persistent History** - Save chat history per user
6. **Rich Media** - Support images, documents
7. **Push Notifications** - Appointment reminders via chat
8. **Multi-turn Flows** - Guided conversations for complex tasks

---

## File Locations

### Frontend
- **Main Component**: `hospico-frontend-main/src/components/ChatWidget.tsx` (~700 lines)
- **Styles**: Inline Tailwind CSS + Framer Motion
- **Routes**: Accessible on all pages + `/embed` route

### Backend
- **Chat Controller**: `backend/src/main/java/com/hospitalfinder/backend/controller/ChatController.java`
- **Hospital Service**: `backend/src/main/java/com/hospitalfinder/backend/service/HospitalService.java`
- **Doctor Service**: `backend/src/main/java/com/hospitalfinder/backend/service/DoctorService.java`
- **Appointment Service**: `backend/src/main/java/com/hospitalfinder/backend/service/AppointmentService.java`

### Configuration
- **Gemini API**: Configured in `application.yml`
- **Database**: PostgreSQL via Spring Data JPA
- **Base URL**: `http://localhost:8080/api`

---

## Recent Updates (February 4, 2026)

### New Symptom Explanation Feature
- **Problem Solved**: Previously, the chatbot would immediately show hospital cards when symptoms were mentioned, which could overwhelm users or feel pushy.
- **Solution**: Implemented a two-step flow where the bot first explains possible causes of symptoms in a calm, helpful manner, then offers choices via buttons.
- **User Benefit**: Gives users control and information before making healthcare decisions.
- **Technical Changes**:
  - Added `symptom_explanation` step in conversation flow
  - Created `buildSymptomExplanation()` method for calm explanations
  - Added `showHospitalsFromSession()` to retrieve hospitals on-demand
  - Frontend displays action buttons when `step === 'symptom_explanation'`
  - Backend detects user intent from follow-up messages
  - Fixed React error by converting Specialization objects to string arrays

---

*Last Updated: February 4, 2026*
