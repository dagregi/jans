# Multi-stage build for Jans Federation Vibe

# Stage 1: Build
FROM maven:3.9-openjdk-11-slim AS builder

WORKDIR /build

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:11-jre-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy the executable JAR from build stage
COPY --from=builder /build/target/jans-federation-vibe-1.13.0-executable.jar /app/app.jar

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV PORT=8080

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/federation/metadata || exit 1

# Start the application
CMD java $JAVA_OPTS -jar /app/app.jar
