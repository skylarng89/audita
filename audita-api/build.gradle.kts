import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "3.4.5" apply false
}

val springBootVersion = "3.4.5"

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

// All other modules produce plain jars — bootJar disabled
configure(subprojects.filter { it.name != "api" }) {
    apply(plugin = "org.springframework.boot")
    tasks.named<BootJar>("bootJar") { enabled = false }
    tasks.named<Jar>("jar") { enabled = true }
}
