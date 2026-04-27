// API module — REST controllers, security config, application entry point.
// The only module that produces a runnable Spring Boot fat jar.
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))

    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))

    // Spring Boot 4: spring-boot-starter-web was renamed to spring-boot-starter-webmvc
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Spring Data for Pageable/Page support in controllers (Spring Data JPA is in :infrastructure)
    implementation("org.springframework.data:spring-data-commons")

    implementation("org.springframework.boot:spring-boot-starter-security")

    // Spring Boot 4: spring-boot-starter-oauth2-client renamed to spring-boot-starter-security-oauth2-client
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Structured JSON logging — encodes Logback events as JSON with MDC support
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    // JWT — needed by JwtAuthenticationFilter (not transitively visible from :infrastructure)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.flywaydb:flyway-core")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}
