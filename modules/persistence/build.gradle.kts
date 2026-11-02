plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jooq.jooq-codegen-gradle") version "3.21.9"
}

group = "dev.junyoung.vai"
version = "0.0.1-SNAPSHOT"
description = "persistence"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    jooqCodegen("org.postgresql:postgresql:42.7.14")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Run explicitly after Flyway migrations have been applied to the code generation database.
jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = providers.environmentVariable("JOOQ_JDBC_URL").orElse("jdbc:postgresql://localhost:5432/vai").get()
            user = providers.environmentVariable("JOOQ_JDBC_USER").orElse("postgres").get()
            password = providers.environmentVariable("JOOQ_JDBC_PASSWORD").orElse("postgres").get()
        }
        generator {
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"
                excludes = "flyway_schema_history|va_default"
            }
            target {
                packageName = "dev.junyoung.vai.persistence.jooq"
                directory = layout.buildDirectory.dir("generated-src/jooq/main").get().asFile.path
            }
        }
    }
}

sourceSets.main {
    java.srcDir(layout.buildDirectory.dir("generated-src/jooq/main"))
}
