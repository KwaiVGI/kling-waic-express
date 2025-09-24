FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8538

CMD ["java", "-jar", "app.jar"]
