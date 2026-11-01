# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

# Stage 2: Runtime stage (smaller image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S finsecure && adduser -S finsecure -G finsecure

# Copy JAR from build stage
COPY --from=builder /build/target/pay-0.0.1-SNAPSHOT.jar app.jar

# Set ownership
RUN chown finsecure:finsecure app.jar

# Switch to non-root user
USER finsecure

EXPOSE 8080

# Health check built into the image
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]