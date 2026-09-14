FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --no-transfer-progress dependency:go-offline

COPY src src
RUN ./mvnw --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S agendapro && adduser -S agendapro -G agendapro

WORKDIR /app
COPY --from=build --chown=agendapro:agendapro /workspace/target/*.jar app.jar

USER agendapro
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
