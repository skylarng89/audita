# syntax=docker/dockerfile:1.7

# ── Build stage ─────────────────────────────────────────────────────────────────
FROM dhi.io/eclipse-temurin:25-jdk-debian13-dev AS builder
WORKDIR /app

ENV GRADLE_OPTS="-Dorg.gradle.wrapper.networkTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000 -Dorg.gradle.internal.http.socketTimeout=60000"

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY gradlew ./

COPY domain/build.gradle.kts domain/
COPY application/build.gradle.kts application/
COPY infrastructure/build.gradle.kts infrastructure/
COPY api/build.gradle.kts api/

RUN chmod +x ./gradlew

# Prime Gradle dependency cache using only build scripts.
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    sh -c 'for i in 1 2 3; do ./gradlew :api:dependencies --no-daemon && exit 0; sleep 5; done; exit 1'

COPY domain/src domain/src
COPY application/src application/src
COPY infrastructure/src infrastructure/src
COPY api/src api/src

RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    sh -c 'for i in 1 2 3; do ./gradlew :api:bootJar --no-daemon && exit 0; sleep 5; done; exit 1'

# Pre-create uploads and logs directories for runtime image without shell utilities.
RUN mkdir -p /tmp/uploads /tmp/logs

# ── Runtime stage ────────────────────────────────────────────────────────────────
FROM dhi.io/eclipse-temurin:25
WORKDIR /app

COPY --from=builder --chown=65532:65532 /tmp/uploads /data/uploads
COPY --from=builder --chown=65532:65532 /tmp/logs /data/logs
COPY --from=builder --chown=65532:65532 /app/api/build/libs/audita-api.jar app.jar

USER 65532:65532

EXPOSE 8080
# Virtual threads available natively in Java 25 — no flag needed
ENTRYPOINT ["java", "-jar", "app.jar"]
