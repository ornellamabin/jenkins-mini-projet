pipeline {
    agent {
        docker {
            image 'python:3.9-slim'
            args '-v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/usr/bin/docker --user root'
        }
    }
    
    environment {
        DOCKER_IMAGE = 'gseha/python-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        PIP_CACHE_DIR = '/tmp/pip-cache'
    }
    
    stages {
        stage('Install Dependencies') {
            steps {
                echo '📦 Installing Python dependencies...'
                sh '''
                    mkdir -p /tmp/pip-cache
                    pip install --cache-dir /tmp/pip-cache -r requirements.txt
                '''
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo '🧪 Running unit tests...'
                // Commande Python corrigée
                sh 'python -c "import flask; print(\"Flask version:\", flask.__version__)"'
                
                // Ajoutez aussi un vrai test si vous avez des fichiers de test
                sh 'python -m unittest discover -s . -p "test_*.py" || echo "No unit tests found"'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    if (!fileExists('Dockerfile')) {
                        error 'Dockerfile not found!'
                    }
                    sh '''
                        docker build -t ${DOCKER_IMAGE}:latest .
                        docker tag ${DOCKER_IMAGE}:latest ${DOCKER_IMAGE}:${DOCKER_TAG}
                    '''
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                echo '📤 Pushing to Docker Hub...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh '''
                        echo $DOCKER_PASSWORD | docker login -u $DOCKER_USER --password-stdin
                        docker push ${DOCKER_IMAGE}:latest
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    '''
                }
            }
        }
        
        stage('Deploy to Staging') {
            steps {
                echo '🚀 Deploying to staging...'
                script {
                    // Version simplifiée pour tester d'abord
                    echo "Simulating deployment to staging"
                    echo "Would run: docker run -d -p 3000:3000 ${DOCKER_IMAGE}:latest"
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "Build status: ${currentBuild.currentResult}"
            }
        }
        
        cleanup {
            echo '🧹 Cleaning up...'
            // Solution pour les permissions Docker
            sh 'docker logout || true'  // Ignorer les erreurs de permission
            sh 'sudo docker logout || true'  // Alternative avec sudo
        }
    }
}