# Integration Guide

This document explains how to integrate the Hospiico Chatbot into the main application.

## Import Resolution

The ChatWidget component expects certain utilities and contexts from the main Hospiico application. When using as a **submodule**, configure path aliases in your main app's `tsconfig.json`:

### TypeScript Path Configuration

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@hospiico/frontend/*": ["src/*"],
      "@hospiico/chatbot/*": ["hospico-frontend-main/src/features/chatbot/*"]
    }
  }
}
```

### Import Aliases Explained

| Alias | Maps To | Purpose |
|-------|---------|---------|
| `@hospiico/frontend/context/ThemeContext` | `src/context/ThemeContext.tsx` | Dark/Light mode theme management |
| `@hospiico/frontend/api` | `src/api.ts` | API request utilities |
| `@hospiico/frontend/store/store` | `src/store/store.ts` | Redux store configuration |
| `@hospiico/frontend/components/HospitalCard` | `src/components/HospitalCard.tsx` | Hospital card display component |

## Required Dependencies in Main App

The main Hospiico application must provide:

### 1. Redux Store Setup
```tsx
// src/store/store.ts
import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer
  }
});

export type RootState = ReturnType<typeof store.getState>;
```

### 2. Auth Slice State
```tsx
// src/store/authSlice.ts
const initialState = {
  isAuthenticated: false,
  user: null
};

// Must have 'isAuthenticated' boolean in state
```

### 3. Theme Context
```tsx
// src/context/ThemeContext.tsx
export interface ThemeContextType {
  theme: 'light' | 'dark';
  setTheme: (theme: 'light' | 'dark') => void;
}

export const useTheme = (): ThemeContextType => { ... }
```

### 4. API Request Utility
```tsx
// src/api.ts
export const apiRequest = async <T, D>(
  url: string,
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  data?: D
): Promise<T> => {
  // Implementation
}
```

### 5. Hospital Card Component
```tsx
// src/components/HospitalCard.tsx
export interface Hospital {
  id?: string;
  clinicId?: string;
  name: string;
  latitude: number;
  longitude: number;
  address?: string;
  phone?: string;
  distance?: number;
  specializations?: string[];
}

export const SharedHospitalCard = (props: {
  hospital: Hospital;
  theme: 'light' | 'dark';
  showDistance?: boolean;
}) => { ... }
```

## Using ChatWidget in Main App

### Basic Usage
```tsx
import ChatWidget from '@hospiico/chatbot/src/components/ChatWidget';
import { Provider } from 'react-redux';
import { store } from './store/store';
import { ThemeProvider } from './context/ThemeContext';

function App() {
  return (
    <Provider store={store}>
      <ThemeProvider>
        <YourMainApp />
        <ChatWidget />
      </ThemeProvider>
    </Provider>
  );
}
```

### With Custom Props
```tsx
<ChatWidget autoOpen={false} embedMode={false} />
```

## Backend API Endpoint

The chatbot communicates with `/api/chat` endpoint:

```http
POST /api/chat
Content-Type: application/json

{
  "messages": [
    {
      "role": "user",
      "content": "I have a headache"
    }
  ],
  "language": "en",
  "latitude": 28.7041,
  "longitude": 77.1025
}
```

**Required Response Format:**
```json
{
  "reply": "Based on your symptoms...",
  "type": "specialization_match|general",
  "hospitals": [...],
  "symptom": "headache",
  "inferredIssue": "...",
  "specializations": ["Neurology", "General Practice"],
  "confidence": "high",
  "disclaimer": "Not a medical diagnosis..."
}
```

## Troubleshooting

### Issue: "Cannot find module '@hospiico/frontend/...'"
**Solution**: Ensure tsconfig.json paths are configured correctly in main app

### Issue: "useTheme is not defined"
**Solution**: Wrap your app with `<ThemeProvider>` at root level

### Issue: "Redux store not found"
**Solution**: Wrap app with `<Provider store={store}>` before using ChatWidget

### Issue: API calls failing
**Solution**: Verify `/api/chat` endpoint exists and returns proper response format

## Development Workflow

1. Make changes in chatbot submodule (`hospico-frontend-main/src/features/chatbot/`)
2. Test in main app context
3. Commit changes to chatbot repo
4. Update main repo submodule reference: `git submodule update --remote`

## File Structure
```
Main App (Hospiico)
├── src/
│   ├── context/ThemeContext.tsx
│   ├── api.ts
│   ├── store/store.ts
│   └── components/HospitalCard.tsx
├── hospico-frontend-main/
│   └── src/features/chatbot/  ← Submodule
│       ├── src/components/ChatWidget.tsx
│       └── src/types/index.ts
└── tsconfig.json (with path aliases)
```
