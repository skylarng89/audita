// Infrastructure module — database, email, file storage, security utilities, tenant wiring.
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.5"))
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:3.4.5"))

    implementation(project(":domain"))
    implementation(project(":application"))

    // Spring Data JPA + Hibernate
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Email + templates
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // AWS SDK v2 (S3-compatible file storage)
    implementation(platform("software.amazon.awssdk:bom:2.31.7"))
    implementation("software.amazon.awssdk:s3")

    // OWASP HTML Sanitizer (sanitise TipTap rich-text before persistence)
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Spring Security crypto (BCrypt)
    implementation("org.springframework.security:spring-security-crypto")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> { useJUnitPlatform() }
