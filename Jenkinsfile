pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'gseha/python-app'  // ← Changez 'gseha' par votre username
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Test Docker Integration') {
            steps {
                echo '🎯 Testing Docker integration...'
                script {
                    sh '''
                        docker --version
                        docker info
                        docker run --rm hello-world
                        echo "✅ Docker is fully operational!"
                    '''
                }
            }
        }
        
        stage('Build Application Image') {
            steps {
                echo '🏗️ Building Python application image...'
                script {
                    sh """
                        docker build -t ${DOCKER_IMAGE}:latest .
                        docker tag ${DOCKER_IMAGE}:latest ${DOCKER_IMAGE}:${DOCKER_TAG}
                        echo "✅ Image built and tagged!"
                    """
                }
            }
        }
        
        stage('Check Docker Hub Token Permissions') {
            steps {
                echo '🔍 Checking token permissions...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    script {
                        sh '''
                            # Test de connexion de base
                            docker login -u $DOCKER_USER -p $DOCKER_PASSWORD
                            echo "✅ Login successful!"
                            
                            # Test spécifique des permissions
                            echo "Testing token permissions..."
                            
                            # Essayer de créer un repository de test
                            curl -s -u "$DOCKER_USER:$DOCKER_PASSWORD" \
                            -X POST \
                            -H "Content-Type: application/json" \
                            -d '{"namespace":"'$DOCKER_USER'", "name":"test-permissions", "is_private":true}' \
                            "https://hub.docker.com/v2/repositories/" \
                            || echo "❌ Token missing write permissions - create new token with Read & Write access"
                        '''
                    }
                }
            }
        }
    }
    
    post {
        failure {
            echo '❌ ÉCHEC : Token Docker Hub sans permissions écriture'
            echo '📋 SOLUTION :'
            echo '1. Allez sur https://hub.docker.com/settings/security'
            echo '2. Créez un nouveau token avec permissions "Read, Write, Delete"'
            echo '3. Mettez à jour les credentials dans Jenkins'
            echo '4. Relancez le pipeline'
        }
        always {
            echo '🧹 Cleaning up...'
        }
    }
}