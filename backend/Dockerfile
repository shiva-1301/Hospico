# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk-alpine AS build

RUN apk add --no-cache maven

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# AppSail injects X_ZOHO_CATALYST_LISTEN_PORT
EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]