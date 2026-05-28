FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY eatzy-common eatzy-common
COPY eatzy-auth-service eatzy-auth-service
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle,sharing=locked rm -rf /root/.gradle/wrapper/dists/gradle-* && ./gradlew :eatzy-auth-service:bootJar -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/eatzy-auth-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
