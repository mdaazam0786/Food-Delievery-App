# Multi-stage build for Order Fulfillment Service
FROM maven:3.9-eclipse-temurin-21 as builder

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -q -O - http://localhost:8093/actuator/health | grep -q UP || exit 1

EXPOSE 8093
ENTRYPOINT ["java", "-jar", "app.jar"]
