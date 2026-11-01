FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre

ENV SPRING_PROFILES_ACTIVE=prod

WORKDIR /app
COPY --from=build /app/target/marketplace-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
