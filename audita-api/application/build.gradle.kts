// Application module — use cases / application services.
// Depends on domain. Orchestrates domain objects and calls ports (interfaces).
// No Spring web or JPA — only Spring context for @Service, @Transactional.
dependencies {
    implementation(project(":domain"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")
}
