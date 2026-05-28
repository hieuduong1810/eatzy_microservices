FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY docker-artifacts/eatzy-communication-service.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
