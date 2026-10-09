plugins {
    java
    `maven-publish`
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
}

group = "com.comply"
version = "1.0.0-SNAPSHOT"
description = "Comply Financial Compliance Intelligent engine"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

extra["jjwtVersion"] = "0.13.0"
extra["zxingVersion"] = "3.5.4"
extra["poiVersion"] = "5.5.1"
extra["opencsvVersion"] = "5.12.0"
extra["tomcatVersion"] = "11.0.25"
extra["springdocVersion"] = "3.1.1"

// Keep both Jackson families and Apache Commons Lang aligned with security fixes.
extra["jackson-2-bom.version"] = "2.21.7"
extra["jackson-bom.version"] = "3.1.7"
extra["commons-lang3.version"] = "3.20.0"

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.tomcat.embed") {
            useVersion(extra["tomcatVersion"] as String)
        }
    }
}

dependencies {
    constraints {
        implementation("org.apache.commons:commons-lang3:${property("commons-lang3.version")}")
        implementation("tools.jackson.core:jackson-core:${property("jackson-bom.version")}")
        implementation("tools.jackson.core:jackson-databind:${property("jackson-bom.version")}")
    }

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-jackson2")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:${property("springdocVersion")}")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.jsonwebtoken:jjwt-api:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${property("jjwtVersion")}")
    implementation("com.google.zxing:core:${property("zxingVersion")}")
    implementation("com.google.zxing:javase:${property("zxingVersion")}")
    implementation("commons-codec:commons-codec")
    implementation("com.opencsv:opencsv:${property("opencsvVersion")}")
    implementation("org.apache.poi:poi:${property("poiVersion")}")
    implementation("org.apache.poi:poi-ooxml:${property("poiVersion")}")

    testImplementation("org.springframework.boot:spring-boot-starter-test-classic")
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    doFirst {
        val mockitoAgent = classpath.files.firstOrNull { it.name.startsWith("mockito-core-") }
        if (mockitoAgent != null) {
            jvmArgs("-javaagent:${mockitoAgent.absolutePath}")
        }
    }
}

tasks.bootJar {
    archiveBaseName = "comply-api"
}

publishing {
    publications {
        create<MavenPublication>("api") {
            from(components["java"])
            groupId = "com.comply"
            artifactId = "comply-api"
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "EstandarMustaq/comply"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.24.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
