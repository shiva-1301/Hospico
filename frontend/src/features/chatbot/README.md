# Hospiico Chatbot - HealthMate AI Assistant

A React-based AI chatbot component for healthcare symptom checking and hospital recommendations.

## Features

- **AI Symptom Checking**: Analyze patient symptoms and suggest medical specializations
- **Hospital Recommendations**: Find nearby hospitals based on symptoms and user location
- **Multi-language Support**: Supports 13+ Indian languages
- **Voice Input/Output**: Speech recognition and text-to-speech capabilities
- **Geolocation Support**: Find hospitals near user location
- **Dark Mode**: Themed UI with light/dark mode support
- **Medical Disclaimers**: Safety disclaimers for all recommendations
- **Embed Mode**: Can be embedded as iframe in third-party websites
- **Secure Access Control**: Submodule with granular team-based permissions

## Important: Peer Dependencies

This package requires the following **peer dependencies** to be installed in the main application:

```json
{
  "react": "^18.2.0",
  "react-dom": "^18.2.0",
  "react-redux": "^8.1.0",
  "react-router-dom": "^6.20.0"
}
```

These are NOT bundled with the chatbot package - they must exist in your main app.

## Installation

### As a Submodule (Recommended for main Hospiico app)

```bash
cd your-main-app
git submodule add https://github.com/CRVPT-10/Hospiico-Chatbot.git hospico-frontend-main/src/features/chatbot
git submodule update --init --recursive
```

Update main app's `tsconfig.json`:
```json
{
  "compilerOptions": {
    "paths": {
      "@hospiico/frontend/*": ["src/*"],
      "@hospiico/chatbot/*": ["hospico-frontend-main/src/features/chatbot/*"]
    }
  }
}
```

### Standalone Installation

```bash
git clone https://github.com/CRVPT-10/Hospiico-Chatbot.git
cd Hospiico-Chatbot
npm install
```

## Quick Start

### Import in Your App

```tsx
import ChatWidget from '@hospiico/chatbot/src/components/ChatWidget';

export default function App() {
  return (
    <>
      <YourApp />
      <ChatWidget />
    </>
  );
}
```

### Required Setup in Main App

See [INTEGRATION.md](./INTEGRATION.md) for detailed setup instructions including:
- Redux store configuration
- Theme context setup
- API utilities
- Path alias configuration

## Props

```tsx
interface ChatWidgetProps {
  autoOpen?: boolean;    // Auto-open chatbot on page load
  embedMode?: boolean;   // Full-screen embed mode
}

<ChatWidget autoOpen={false} embedMode={false} />
```

## API Endpoint

The chatbot communicates with your backend at `/api/chat`:

**Request:**
```json
{
  "messages": [{"role": "user", "content": "I have chest pain"}],
  "language": "en",
  "latitude": 28.7041,
  "longitude": 77.1025
}
```

**Response:**
```json
{
  "reply": "Based on your symptoms...",
  "type": "specialization_match",
  "hospitals": [
    {
      "id": "h1",
      "name": "Apollo Hospital",
      "latitude": 28.7041,
      "longitude": 77.1025,
      "distance": 2.5,
      "specializations": ["Cardiology", "Emergency"]
    }
  ],
  "symptom": "chest pain",
  "inferredIssue": "Cardiac concerns",
  "specializations": ["Cardiology", "Emergency Medicine"],
  "confidence": "high",
  "disclaimer": "Not a medical diagnosis. Consult a doctor."
}
```

## Development

### Setup
```bash
npm install
npm run dev
```

### Build
```bash
npm run build
```

### Type Checking
```bash
npm run type-check
```

### Linting
```bash
npm run lint
```

## Project Structure

```
src/
├── components/
│   └── ChatWidget.tsx       # Main chatbot component (385 lines)
├── types/
│   └── index.ts            # TypeScript interfaces
└── ...
```

## Security & Access Control

### Granular Permissions

This repository uses GitHub Teams for role-based access:

**Chatbot Contributors Team**
- Members: View & Edit access to **chatbot repo only**
- Cannot access main Hospiico repository
- Can make PRs and push to chatbot branches
- Use case: Team members working exclusively on chatbot features

**Full Developers Team**
- Members: You + trusted co-developers
- Admin access to **all repositories**
- Can manage releases, access control, and critical systems
- Use case: Project maintainers with full codebase access

### How to Configure

1. Go to GitHub Organization Settings → Teams
2. Create "Chatbot Contributors" team
3. Create "Full Developers" team
4. Add members to appropriate teams
5. Go to Chatbot Repo → Settings → Collaborators
6. Add teams with appropriate permissions:
   - Chatbot Contributors: `Write` access
   - Full Developers: `Admin` access
7. Main Repo access remains restricted to Full Developers only

## File Structure

```
Hospiico-Chatbot/
├── src/
│   ├── components/ChatWidget.tsx
│   └── types/index.ts
├── .github/workflows/npm-publish.yml
├── INTEGRATION.md
├── README.md
├── package.json
├── tsconfig.json
└── .gitignore
```

## Deployment

### As NPM Package

Tag releases in git:
```bash
git tag v1.0.1
git push origin v1.0.1
```

GitHub Actions will automatically publish to NPM (requires `NPM_TOKEN` secret).

### As Submodule

Simply push to main branch:
```bash
git push origin main
```

Then update main repo:
```bash
cd main-repo
git submodule update --remote
git commit -am "Update chatbot submodule"
git push
```

## Medical Disclaimers

⚠️ **Important**:
- All responses include medical disclaimers
- Not a replacement for professional medical advice
- Symptom matching is based on AI analysis only
- Always encourage users to consult licensed healthcare providers

## License

MIT

## Support & Issues

**Main Repository**: [Hospiico](https://github.com/CRVPT-10/Hospiico)

**Chatbot Repository**: [Hospiico-Chatbot](https://github.com/CRVPT-10/Hospiico-Chatbot)

**Issues**: Create an issue in the appropriate repository based on the component affected

## Team

**Full Access (Main + Chatbot)**:
- Primary Lead: [Your Name]
- Co-Developer: [Friend Name]

**Chatbot-Only Contributors**:
- List managed via GitHub Teams (see Security section above)

## Changelog

### v1.0.0
- Initial release
- ChatWidget component with full features
- Multi-language support
- Voice input/output
- Hospital recommendations
- Medical disclaimers

