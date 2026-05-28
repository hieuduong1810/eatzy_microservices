FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY docker-artifacts/eatzy-restaurant-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
