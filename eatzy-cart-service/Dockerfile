FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY docker-artifacts/eatzy-cart-service.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
