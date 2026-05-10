import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "4.0.6" apply false
    id("org.sonarqube") version "7.2.3.7755"
}

// Java 25 GA — Spring Boot 4.0.6 fully supports it.
// Lombok is NOT used in this project: JDK 25's Flow$AliveAnalyzer has a bug (JDK-8344706)
// that NPEs on Lombok-generated synthetic VarDecls. All boilerplate is written explicitly.
val springBootVersion = "4.0.6"

allprojects {
    group = "io.audita"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
}

// Only :api produces a runnable fat jar
project(":api") {
    apply(plugin = "org.springframework.boot")
    tasks.named<BootJar>("bootJar") {
        archiveFileName.set("audita-api.jar")
    }
}

// All other modules produce plain library jars
configure(subprojects.filter { it.name != "api" }) {
    apply(plugin = "org.springframework.boot")
    tasks.named<BootJar>("bootJar") { enabled = false }
    tasks.named<Jar>("jar") { enabled = true }
}
