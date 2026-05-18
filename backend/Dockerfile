# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy all pom files first for layer caching
COPY pom.xml .
COPY contract/pom.xml contract/
COPY backend/pom.xml backend/
RUN mvn dependency:go-offline -q
# Copy sources and build
COPY contract/src contract/src
COPY backend/src backend/src
RUN mvn package -DskipTests -q

# --- Stage 2: Run ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
