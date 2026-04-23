// API module — REST controllers, DTOs, security config, application entry point.
// This module is the only one that produces a runnable Spring Boot fat jar.
dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))

    // Spring Boot Web (embedded Tomcat)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Spring Security OAuth2 Client (Google + Microsoft OIDC)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // Spring Boot Actuator (health, metrics)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Configuration metadata
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Test slices
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    // TestContainers for integration tests
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}
