# Multi-stage build for Quarkus application with Java 25

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-25-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the application (uber-jar for easier deployment)
RUN mvn package -DskipTests -Dquarkus.package.type=uber-jar

# Stage 2: Runtime with Java 25
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy the uber jar from build stage
COPY --from=build /app/target/*-runner.jar /app/application.jar

# Create non-root user for security
RUN addgroup -g 1001 quarkus && \
    adduser -D -u 1001 -G quarkus quarkus && \
    chown -R quarkus:quarkus /app

USER quarkus

# Expose HTTP and debug ports
EXPOSE 3000 5005

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:3000/health || exit 1

# JVM Options for container
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/application.jar"]
