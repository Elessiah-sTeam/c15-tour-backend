# backend/Dockerfile

# On part d'une image Java 21 légère
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# On copie le JAR généré
COPY target/*.jar app.jar

# On expose le port 8080
EXPOSE 8080

# On lance l'application avec le profil "docker" pour la config BDD
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
