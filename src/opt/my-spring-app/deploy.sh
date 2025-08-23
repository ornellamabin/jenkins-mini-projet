#!/bin/bash
cd /opt/my-spring-app
# Pull la dernière image de DockerHub
docker-compose pull
# Recrée et relance le conteneur
docker-compose up -d --force-recreate
# Nettoyer les anciennes images pour économiser de l'espace
docker image prune -f
echo "Déploiement terminé sur le serveur de production."