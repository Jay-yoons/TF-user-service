# Multi-stage build for optimized Docker image
FROM openjdk:17-jdk-slim AS builder

# Set environment variables for Gradle
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Xmx2048m"
ENV GRADLE_HOME="/opt/gradle"
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

# Set working directory
WORKDIR /app

# Install curl for health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy gradle files first (for better caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (with retry mechanism)
RUN ./gradlew dependencies --no-daemon --stacktrace || \
    (sleep 10 && ./gradlew dependencies --no-daemon --stacktrace) || \
    (sleep 30 && ./gradlew dependencies --no-daemon --stacktrace)

# Copy source code
COPY src src

# Build the application (with retry mechanism)
RUN ./gradlew build -x test --no-daemon --stacktrace || \
    (sleep 10 && ./gradlew build -x test --no-daemon --stacktrace) || \
    (sleep 30 && ./gradlew build -x test --no-daemon --stacktrace)

# Runtime stage
FROM openjdk:17-jdk-slim

# Set timezone to Asia/Seoul
ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-Duser.timezone=Asia/Seoul -Dfile.encoding=UTF-8"

# Install curl for health check and timezone data
RUN apt-get update && apt-get install -y curl tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run the application with timezone settings
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
