plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.eatzy"
version = "0.0.1-SNAPSHOT"
description = "eatzy-api-gateway"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-web")
    }
    // Ép dự án sử dụng WebFlux và Netty
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // Yêu cầu Gradle kéo module common vào, LOẠI BỎ Tomcat/Web/Security để Gateway chạy Netty thuần
    implementation(project(":eatzy-common")) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-web")
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-security")
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-oauth2-resource-server")
    }

    // OpenAPI / Swagger for WebFlux
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.6")
}

configurations.all {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-web")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}



tasks.withType<Test> {
    useJUnitPlatform()
}
