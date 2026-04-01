plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.eatzy"
version = "0.0.1-SNAPSHOT"
description = "eatzy-discovery-server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
