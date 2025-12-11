# =====================
# BUILD STAGE
# =====================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

# Build app using Maven
RUN mvn clean package -DskipTests

# =====================
# RUNTIME STAGE
# =====================
FROM eclipse-temurin:21-jdk-alpine

# Copy built jar from previous stage
COPY --from=build /app/target/post-service-*.jar app.jar

# Expose port (must match `server.port` in application.properties)
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
