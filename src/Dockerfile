# Étape 1 : Utiliser une image de base avec JDK
FROM openjdk:17-jdk-alpine

# Étape 2 : Définir un "utilisateur" non-root pour plus de sécurité
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Étape 3 : Définir le point de montage pour les volumes (si besoin)
VOLUME /tmp

# Étape 4 : Copier le fichier JAR construit par Maven dans l'image
# (Le "*.jar" est important car le nom exact dépend de votre pom.xml)
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Étape 5 : Commande pour lancer l'application quand le conteneur démarre
ENTRYPOINT ["java","-jar","/app.jar"]