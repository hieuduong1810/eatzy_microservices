FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY eatzy-common eatzy-common
COPY eatzy-discovery-server eatzy-discovery-server
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle,sharing=locked ./gradlew :eatzy-discovery-server:bootJar -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/eatzy-discovery-server/build/libs/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
