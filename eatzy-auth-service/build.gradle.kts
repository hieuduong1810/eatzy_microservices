plugins {
    java
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(project(":eatzy-common"))

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

    // Mail
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Spring Filter (ho tro @Filter Specification tren Controller)
    implementation("com.turkraft.springfilter:jpa:3.1.7")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    // DevTools (auto-restart khi save file)
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
