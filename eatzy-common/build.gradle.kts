plugins {
    id("java")
}

group = "com.eatzy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    compileOnly("org.springframework.cloud:spring-cloud-starter-openfeign")
}

tasks.bootJar {
    // Tắt tính năng đóng gói thành file thực thi (vì nó chỉ là thư viện dùng chung)
    enabled = false
}
tasks.jar {
    enabled = true
}

tasks.test {
    useJUnitPlatform()
}