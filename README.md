# Hospico - Hospital Finder Application

A full-stack hospital booking application with React frontend and Spring Boot backend.

## Deployment to Render

This application is configured for deployment to Render using the `render.yaml` file.

### Services Included:
1. **Frontend** - React application (hospital-finder-frontend)
   https://www.hospiico.onslate.com/

2. **Backend** - Spring Boot API (hospital-finder-backend)
   https://hospiico-backend-50037434927.development.catalystappsail.in/
    
4. **Database** - PostgreSQL (hospital-db)

### Environment Variables Needed:
- `REACT_APP_API_URL` - URL of the backend service (set automatically by Render)
- Database credentials are managed by Render

### Deployment Steps:
1. Fork this repository to your GitHub account
2. Connect your GitHub repository to Render
3. Import the `render.yaml` blueprint
4. Deploy the services

### Local Development:
```bash
# Start all services with Docker Compose
docker-compose up -d

# Access the application:
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Database: postgresql://localhost:5432
```

### Tech Stack:
- **Frontend**: React, TypeScript, Tailwind CSS
- **Backend**: Spring Boot, Java
- **Database**: PostgreSQL
- **Deployment**: Docker, Render
