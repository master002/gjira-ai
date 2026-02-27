FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Build stage happens externally: mvn -pl ms-omni-ingest package -DskipTests
COPY ms-omni-ingest/target/ms-omni-ingest-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
