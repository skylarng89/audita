# ── Build stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY gradlew ./

# Copy all module sources
COPY domain/build.gradle.kts domain/
COPY domain/src domain/src
COPY application/build.gradle.kts application/
COPY application/src application/src
COPY infrastructure/build.gradle.kts infrastructure/
COPY infrastructure/src infrastructure/src
COPY api/build.gradle.kts api/
COPY api/src api/src

RUN chmod +x ./gradlew && ./gradlew :api:bootJar --no-daemon

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S audita && adduser -S audita -G audita
USER audita

COPY --from=builder /app/api/build/libs/audita-api.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseVirtualThreads", "-jar", "app.jar"]
