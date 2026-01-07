# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

ENV PATH="/opt/java/openjdk/bin:${PATH}"

EXPOSE 9000

CMD ["java", "-jar", "/app/app.jar"]
