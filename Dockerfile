FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
COPY backend.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]