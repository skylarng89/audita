# ── Build stage ─────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25.0.3_9-jdk-noble AS builder
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
FROM eclipse-temurin:25.0.3_9-jre-noble
WORKDIR /app

RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/*

RUN addgroup --system audita && adduser --system --ingroup audita audita

# Pre-create the uploads directory with correct ownership so the Docker volume
# inherits audita:audita ownership on first initialisation.
RUN mkdir -p /data/uploads && chown -R audita:audita /data/uploads

USER audita

COPY --from=builder --chown=audita:audita /app/api/build/libs/audita-api.jar app.jar

EXPOSE 8080
# Virtual threads available natively in Java 25 — no flag needed
ENTRYPOINT ["java", "-jar", "app.jar"]
