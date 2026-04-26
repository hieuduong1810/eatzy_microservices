FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY eatzy-common eatzy-common
COPY eatzy-system-config-service eatzy-system-config-service
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle,sharing=locked ./gradlew :eatzy-system-config-service:bootJar -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/eatzy-system-config-service/build/libs/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
