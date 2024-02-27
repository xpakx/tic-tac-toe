# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:21-slim-bookworm
MAINTAINER xpakx.github.io
EXPOSE 8081
COPY --from=build app/target/tictactoe-0.0.1-SNAPSHOT.jar application.jar
ENTRYPOINT ["java","-jar","/application.jar"]
