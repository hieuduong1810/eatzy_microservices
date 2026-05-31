FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY docker-artifacts/eatzy-payment-service.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
