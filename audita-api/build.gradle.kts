import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "3.4.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

// Java 25 is cutting-edge; we target 21 (LTS) with virtual threads available.
// Switch toolchain to 25 once a GA JDK 25 is available and Spring Boot supports it.
val javaVersion = JavaVersion.VERSION_21

allprojects {
    group = "io.audita"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5")
        }
    }

    dependencies {
        // Lombok — reduce boilerplate
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        // Testing
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("-XX:+EnableDynamicAgentLoading")
    }
}

// Only the :api module produces a runnable fat jar
project(":api") {
    apply(plugin = "org.springframework.boot")

    tasks.named<BootJar>("bootJar") {
        archiveFileName.set("audita-api.jar")
    }
}

// All other modules produce plain jars — disable bootJar on them
configure(subprojects.filter { it.name != "api" }) {
    apply(plugin = "org.springframework.boot")
    tasks.named<BootJar>("bootJar") { enabled = false }
    tasks.named<Jar>("jar") { enabled = true }
}
