FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/CodeVeda.jar app.jar
EXPOSE 1234
ENTRYPOINT ["java", "-jar", "app.jar"]