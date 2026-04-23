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

    implementation("org.springframework.boot:spring-boot-starter-security")

    // Spring Boot 4: spring-boot-starter-oauth2-client renamed to spring-boot-starter-security-oauth2-client
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}
