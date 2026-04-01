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

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    // Spring Filter
    implementation("com.turkraft.springfilter:jpa:3.1.7")

    // DevTools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
