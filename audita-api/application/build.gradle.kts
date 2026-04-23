// Application module — port interfaces and pure application logic.
// Depends only on domain. No infrastructure imports.
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.5"))
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:3.4.5"))

    implementation(project(":domain"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> { useJUnitPlatform() }
