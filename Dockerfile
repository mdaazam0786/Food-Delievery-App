# Root Dockerfile - delegates to service-specific Dockerfile
# This allows Vercel to detect and build Docker when repo is at root

ARG SERVICE_PATH=backend/auth-service
ARG SERVICE_PORT=8082

FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /build

# Install Maven and Git
RUN apk add --no-cache maven git

# Copy service pom.xml
COPY ${SERVICE_PATH}/pom.xml .

# Clone and install common-exceptions
RUN git clone --depth 1 https://github.com/mdaazam0786/Food-Delievery-App.git /tmp/foodzie-repo && \
    cd /tmp/foodzie-repo/backend/common-exceptions && \
    mvn clean install -q -DskipTests && \
    cd /build

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy service source
COPY ${SERVICE_PATH}/src ./src

# Build service
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -q -O - http://localhost:${PORT:-${SERVICE_PORT}}/actuator/health | grep -q UP || exit 1

# Expose port
EXPOSE ${SERVICE_PORT}

# Run application
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT:-${SERVICE_PORT}}"]
