FROM openjdk:8-jdk-alpine
COPY target/server.jar /app/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "server.jar"]
