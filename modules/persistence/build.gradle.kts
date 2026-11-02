buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:12.4.0")
        classpath("org.postgresql:postgresql:42.7.14")
    }
}

plugins {
    `java-library`
    id("org.flywaydb.flyway") version "12.4.0"
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
    api(platform("org.springframework.boot:spring-boot-dependencies:4.1.2"))
    api("org.jooq:jooq")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    jooqCodegen("org.postgresql:postgresql:42.7.14")
}

val codegenUrl = providers.environmentVariable("JOOQ_JDBC_URL").orElse("jdbc:postgresql://localhost:5432/vai")
val codegenUser = providers.environmentVariable("JOOQ_JDBC_USER").orElse("postgres")
val codegenPassword = providers.environmentVariable("JOOQ_JDBC_PASSWORD").orElse("postgres")

flyway {
    url = codegenUrl.get()
    user = codegenUser.get()
    password = codegenPassword.get()
    locations = arrayOf("filesystem:${project.projectDir}/src/main/resources/db/migration")
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = codegenUrl.get()
            user = codegenUser.get()
            password = codegenPassword.get()
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

val verifyJooqGenerated = tasks.register("verifyJooqGenerated") {
    doLast {
        val generatedTables = layout.buildDirectory.file("generated-src/jooq/main/dev/junyoung/vai/persistence/jooq/Tables.java").get().asFile
        check(generatedTables.isFile) {
            "jOOQ generated sources are missing. Run :persistence:flywayMigrate and then :persistence:jooqCodegen against a development database."
        }
    }
}

tasks.named("compileJava") {
    dependsOn(verifyJooqGenerated)
}
