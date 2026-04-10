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

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    // OpenFeign (for calling Auth/Order services)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Spring Filter (ho tro @Filter Specification tren Controller)
    implementation("com.turkraft.springfilter:jpa:3.1.7")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // DevTools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test
    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
