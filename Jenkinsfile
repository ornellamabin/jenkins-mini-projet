stage('Deploy') {
    steps {
        echo '🚀 Deploying...'
        script {
            sh """
                # Arrêter tout conteneur utilisant le port 3000
                docker stop python-app test-app 2>/dev/null || true
                docker rm python-app test-app 2>/dev/null || true
                
                # Trouver et arrêter le conteneur utilisant le port 3000
                CONTAINER_USING_PORT=\$(docker ps --format "{{.Names}}" | xargs -I {} docker port {} | grep 3000 | cut -d: -f1 | head -1)
                if [ ! -z "\$CONTAINER_USING_PORT" ]; then
                    echo "Stopping container using port 3000: \$CONTAINER_USING_PORT"
                    docker stop \$CONTAINER_USING_PORT || true
                    docker rm \$CONTAINER_USING_PORT || true
                fi
                
                # Démarrer l'application
                docker run -d --name python-app -p 3000:3000 ${DOCKER_IMAGE}:latest
                sleep 8
                curl -f http://localhost:3000 && echo "✅ App running!" || echo "⚠️ Check app manually"
            """
        }
    }
}