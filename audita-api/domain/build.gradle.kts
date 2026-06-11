// Domain module — pure business logic. No Spring, no infrastructure, no Lombok.
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))

    implementation("jakarta.validation:jakarta.validation-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> { useJUnitPlatform() }
