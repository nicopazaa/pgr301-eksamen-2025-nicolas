# syntax=docker/dockerfile:1

##############################
# Stage 1: Build the application
##############################
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven config and sources
COPY pom.xml .
COPY src ./src

# Build the JAR (skip tests for faster CI builds)
RUN mvn -B clean package -DskipTests

##############################
# Stage 2: Runtime image
##############################
FROM eclipse-temurin:21-jre
WORKDIR /app

# Konfigureres via miljøvariabler ved runtime (ingen secrets i bildet)
# Disse kan overstyres når containeren kjøres:
ENV SERVER_PORT=8080

# Valgfritt: default verdier som kan overstyres av docker run / kubernetes env
ENV AWS_REGION=eu-north-1
ENV S3_BUCKET_NAME=pgr301-analysis-nicopazaa

# Kopier bygget jar fra build-stage
COPY --from=build /app/target/*.jar app.jar

# Eksponer HTTP-port
EXPOSE 8080

# Støtter ekstra JAVA_OPTS (f.eks. -Xms256m -Xmx512m)
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
