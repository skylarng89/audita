# ── Build stage ─────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY gradlew ./

COPY domain/build.gradle.kts domain/
COPY domain/src domain/src
COPY application/build.gradle.kts application/
COPY application/src application/src
COPY infrastructure/build.gradle.kts infrastructure/
COPY infrastructure/src infrastructure/src
COPY api/build.gradle.kts api/
COPY api/src api/src

RUN chmod +x ./gradlew && ./gradlew :api:bootJar --no-daemon

# ── Runtime stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/*

RUN addgroup --system audita && adduser --system --ingroup audita audita
USER audita

COPY --from=builder /app/api/build/libs/audita-api.jar app.jar

EXPOSE 8080
# Virtual threads available natively in Java 25 — no flag needed
ENTRYPOINT ["java", "-jar", "app.jar"]
