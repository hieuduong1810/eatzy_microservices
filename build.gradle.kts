// File: eatzy-microservices/build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "3.4.3" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

// Các version dùng chung
val springCloudVersion = "2024.0.0"

// Cấu hình áp dụng cho TẤT CẢ các module con
subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    group = "com.eatzy"
    version = "0.0.1-SNAPSHOT"

    java {
        sourceCompatibility = JavaVersion.VERSION_17
    }

    repositories {
        mavenCentral()
    }

    // Import Spring Cloud BOM bằng tính năng platform() của Gradle
    dependencies {
        // Platform BOM dùng để đồng bộ version Spring Cloud cho toàn bộ module con
        implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"))

        // Thư viện cơ bản module nào cũng cần
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        
        // dotenv for environment variables
        implementation("me.paulschwarz:spring-dotenv:4.0.0")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
