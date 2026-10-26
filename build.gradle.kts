buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        constraints {
            classpath("org.apache.commons:commons-lang3:3.21.0")
            classpath("tools.jackson.core:jackson-core:3.2.4")
            classpath("tools.jackson.core:jackson-databind:3.2.4")
        }
    }
}

plugins {
    base
    eclipse
    id("org.springframework.boot") version "4.1.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

eclipse {
    project {
        name = "complyz"
        comment = "Complyz project managed by Gradle."
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}
