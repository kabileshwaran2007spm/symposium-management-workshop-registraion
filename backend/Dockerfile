FROM maven:3.9-eclipse-temurin-25-alpine AS build

WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
RUN addgroup -S campusconnect && adduser -S -G campusconnect campusconnect
COPY --from=build --chown=campusconnect:campusconnect /app/target/campusconnect-*.jar app.jar
USER campusconnect

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
