FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY docker-artifacts/eatzy-config-server.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
