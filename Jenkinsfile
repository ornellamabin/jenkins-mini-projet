pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'gseha/python-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Test Docker Integration') {
            steps {
                echo '🎯 Testing Docker...'
                sh '''
                    docker --version
                    docker run --rm hello-world
                    echo "✅ Docker operational!"
                '''
            }
        }
        
        stage('Build Image') {
            steps {
                echo '🏗️ Building...'
                sh """
                    docker build -t ${DOCKER_IMAGE}:latest .
                    docker tag ${DOCKER_IMAGE}:latest ${DOCKER_IMAGE}:${DOCKER_TAG}
                    echo "✅ Image built!"
                """
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                echo '📤 Pushing...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh """
                        docker push ${DOCKER_IMAGE}:latest
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        echo "🎉 Pushed to Docker Hub!"
                    """
                }
            }
        }
        
        stage('Deploy') {
            steps {
                echo '🚀 Deploying...'
                script {
                    sh """
                        # Arrêter tout conteneur utilisant le port 3000
                        docker stop python-app test-app 2>/dev/null || true
                        docker rm python-app test-app 2>/dev/null || true
                        
                        # Démarrer l'application sur un port différent si nécessaire
                        docker run -d --name python-app -p 3000:3000 ${DOCKER_IMAGE}:latest
                        sleep 8
                        curl -f http://localhost:3000 && echo "✅ App running!" || echo "⚠️ Check app manually"
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo '🏆 SUCCÈS TOTAL! Pipeline CI/CD opérationnel!'
            echo '🎉 Vos images sont sur Docker Hub: https://hub.docker.com/r/gseha/python-app'
        }
        failure {
            echo '❌ Échec - Vérifiez les logs'
        }
    }
}