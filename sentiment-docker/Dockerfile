# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven configuration
COPY pom.xml .
COPY src ./src

# Build the JAR (skip tests for faster CI)
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose backend port
EXPOSE 8080

# Run backend
ENTRYPOINT ["java", "-jar", "app.jar"]

