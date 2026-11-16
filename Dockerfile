FROM eclipse-temurin:27-jre-alpine
VOLUME /tmp
COPY backend.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
