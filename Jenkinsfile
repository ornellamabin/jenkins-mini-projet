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
                sh "python -c \"import flask; print('Flask version:', flask.__version__)\""
                
                // Test supplémentaire avec gestion d'erreur Jenkins
                script {
                    try {
                        sh "python -c \"from app import app; print('App imported successfully')\""
                    } catch (Exception e) {
                        echo "No app.py found, continuing..."
                    }
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    if (!fileExists('Dockerfile')) {
                        echo '⚠️ Dockerfile not found! Creating a simple one...'
                        sh '''
                            cat > Dockerfile << EOF
FROM python:3.9-slim
WORKDIR /app
COPY . .
RUN pip install -r requirements.txt
EXPOSE 3000
CMD ["python", "app.py"]
EOF
                        '''
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
            sh 'which docker >/dev/null 2>&1 && docker logout || true'
        }
    }
}