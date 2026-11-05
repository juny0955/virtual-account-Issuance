plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.junyoung.vai"
version = "0.0.1-SNAPSHOT"
description = "batch"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":persistence"))
    implementation("org.springframework.boot:spring-boot-starter-batch-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-batch-jdbc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
