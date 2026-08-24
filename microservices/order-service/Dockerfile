# ==============================================================================
# Multi-Stage Production Dockerfile for Spring Boot Microservices
# Optimized for: Security (Non-Root User), Image Size (<180MB), Layer Caching
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Dependencies Layer Cache
# ------------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Cache Maven dependencies separately to optimize Docker layer caching
COPY pom.xml ./
COPY service-registry/pom.xml ./service-registry/
COPY api-gateway/pom.xml ./api-gateway/
COPY user-auth-service/pom.xml ./user-auth-service/
COPY order-service/pom.xml ./order-service/
COPY pickup-fleet-service/pom.xml ./pickup-fleet-service/
COPY fulfillment-service/pom.xml ./fulfillment-service/
COPY tracking-service/pom.xml ./tracking-service/
COPY notification-service/pom.xml ./notification-service/

# Download dependencies offline (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline -B

# 2. Copy source code and compile
COPY order-service/src ./order-service/src
RUN mvn clean package -pl order-service -am -DskipTests

# ------------------------------------------------------------------------------
# Stage 2: Minimal Distroless / Alpine Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

# Security: Create non-root system group and user
RUN addgroup -S appgroup -g 10001 && \
    adduser -S appuser -u 10001 -G appgroup

WORKDIR /app

# Copy the built artifact from the builder stage
COPY --from=builder --chown=appuser:appgroup /build/order-service/target/*.jar app.jar

# Run as non-privileged user
USER appuser:appgroup

# Expose microservice HTTP port
EXPOSE 8081

# Production JVM Tuning & Garbage Collection flags
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

# Healthcheck for Container Orchestrators
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
