// Domain module — pure business logic. No Spring, no infrastructure.
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.5"))
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:3.4.5"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("jakarta.validation:jakarta.validation-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> { useJUnitPlatform() }
