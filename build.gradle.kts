plugins {
    base
    eclipse
    id("org.springframework.boot") version "4.1.1" apply false
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
