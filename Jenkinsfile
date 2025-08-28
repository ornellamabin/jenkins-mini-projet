stage('Build Docker Image') {
    steps {
        script {
            echo "✅ Simulation: Image Docker ${DOCKER_IMAGE} built successfully"
            // docker.build("${DOCKER_IMAGE}") - ÉTAPE DÉSACTIVÉE
        }
    }
}

stage('Push to Docker Hub') {
    steps {
        script {
            echo "✅ Simulation: Image pushed to Docker Hub"
            // dockerImage.push() - ÉTAPE DÉSACTIVÉE
        }
    }
}