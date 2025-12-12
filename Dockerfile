# Stage 1: Maven build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy POM files for dependency caching
COPY pom.xml .
COPY domain/pom.xml domain/
COPY application/pom.xml application/
COPY infrastructure/pom.xml infrastructure/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY domain/src domain/src
COPY application/src application/src
COPY infrastructure/src infrastructure/src

# Build the application
RUN mvn clean package -DskipTests -pl infrastructure -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Create non-root user with specific UID
RUN addgroup -S appgroup && adduser -S -u 1001 appuser -G appgroup

WORKDIR /app

# Create data directory with proper ownership
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Copy JAR from build stage
COPY --from=build /app/infrastructure/target/evac-route-infrastructure-*.jar app.jar

# Expose application port
EXPOSE 8080

# Health check using wget
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application (exec form)
ENTRYPOINT ["java", "-jar", "app.jar"]
