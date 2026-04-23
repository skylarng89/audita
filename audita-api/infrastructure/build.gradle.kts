// Infrastructure module — database, email, file storage, scheduling, SSE.
// Implements the ports defined in the application module.
dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))

    // Spring Data JPA + Hibernate
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // PostgreSQL driver
    runtimeOnly("org.postgresql:postgresql")

    // Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Spring Mail
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Thymeleaf (email templates)
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // AWS SDK v2 (S3-compatible file storage)
    implementation(platform("software.amazon.awssdk:bom:2.31.7"))
    implementation("software.amazon.awssdk:s3")

    // OWASP HTML Sanitizer (rich text input from TipTap)
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")

    // Spring Security (for password encoding, etc.)
    implementation("org.springframework.security:spring-security-crypto")
}
