# Vercel-compatible Dockerfile for Payment Service

FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /build
RUN apk add --no-cache maven git
COPY pom.xml .
RUN git clone --depth 1 https://github.com/mdaazam0786/Food-Delievery-App.git /tmp/foodzie-repo && \
    cd /tmp/foodzie-repo/backend/common-exceptions && \
    mvn clean install -q -DskipTests && \
    cd /build
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -q -O - http://localhost:${PORT:-8084}/actuator/health | grep -q UP || exit 1
EXPOSE 8084
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8084}"]
