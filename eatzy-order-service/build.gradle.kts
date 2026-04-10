plugins {
    java
}

dependencies {
    // Common module
    implementation(project(":eatzy-common"))

    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // JPA + Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

    // Security + JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Eureka Client
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Config Client
    implementation("org.springframework.cloud:spring-cloud-starter-config")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // OpenFeign (for calling other services)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Redis (for driver rejection tracking)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Spring Filter (ho tro @Filter Specification tren Controller)
    implementation("com.turkraft.springfilter:jpa:3.1.7")

    // DevTools (auto-restart khi save file)
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test
    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
