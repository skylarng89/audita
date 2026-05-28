plugins {
    `java-library`
}

// Infrastructure module — database, email, file storage, security utilities, tenant wiring.
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))

    // Exposed because :api compiles against infrastructure types annotated with jakarta.persistence.*
    api("jakarta.persistence:jakarta.persistence-api")

    implementation(project(":domain"))
    implementation(project(":application"))

    // Spring Data JPA + Hibernate (auto-configures Hibernate 7)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql:42.7.11")

    // Flyway — Spring Boot 4 requires the starter (not bare flyway-core)
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    // PostgreSQL-specific Flyway dialect (not included transitively by the starter)
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // Email + Thymeleaf templates
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // AWS SDK v2 (S3-compatible file storage)
    implementation(platform("software.amazon.awssdk:bom:2.44.14"))
    implementation("software.amazon.awssdk:s3")

    // OWASP HTML Sanitizer — sanitise TipTap rich-text before persistence
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20260313.1")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // Spring Web — RestClient (used by SsoService for OAuth2 token exchange)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Security crypto (BCrypt password hashing)
    implementation("org.springframework.security:spring-security-crypto")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> { useJUnitPlatform() }
