FROM maven:3.8.6-openjdk-17 AS build

WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests

FROM openjdk:17-jdk-slim

EXPOSE 8080

COPY --from=build /app/target/deploy_render-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
