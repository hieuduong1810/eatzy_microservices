plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":eatzy-common"))

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Eureka Client
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Config Client
    implementation("org.springframework.cloud:spring-cloud-starter-config")

    // Database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // OpenFeign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Spring Filter
    implementation("com.turkraft.springfilter:jpa:3.1.7")
}
