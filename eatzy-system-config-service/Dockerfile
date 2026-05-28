FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY docker-artifacts/eatzy-system-config-service.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
