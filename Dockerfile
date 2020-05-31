FROM openjdk:11-jre-slim
COPY target/server.jar /app/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "server.jar"]
