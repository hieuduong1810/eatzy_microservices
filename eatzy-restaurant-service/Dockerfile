FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY eatzy-common eatzy-common
COPY eatzy-restaurant-service eatzy-restaurant-service
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle,sharing=locked ./gradlew :eatzy-restaurant-service:bootJar -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/eatzy-restaurant-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
