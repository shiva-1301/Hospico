# Use OpenJDK 21 as the base image
FROM eclipse-temurin:21-jdk-alpine

# Install Maven
RUN apk add --no-cache maven

# Set the working directory
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY backend/pom.xml .
COPY backend/.mvn .mvn
COPY backend/mvnw .
COPY backend/src src

# Make mvnw executable
RUN chmod +x mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Copy the JAR file
RUN cp target/backend-0.0.1-SNAPSHOT.jar app.jar

# Expose the default AppSail port
EXPOSE 8080

# Run the application (let application.yml and environment variables handle config)
CMD ["java", "-jar", "/app/app.jar"]