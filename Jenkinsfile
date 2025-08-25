pipeline {
    agent {
        docker {
            image 'python:3.9-slim'
            args '--user root -v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/usr/bin/docker'
        }
    }
    
    environment {
        // Optionnel : retirez si vous n'avez pas encore configuré les credentials
        // DOCKERHUB_CREDENTIALS = credentials('docker-hub-credentials')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
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
                sh 'python -c "import flask; print(\"Flask version:\", flask.__version__)"'
                script {
                    sh 'python -c "from app import app; print(\"App imported successfully\")"'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    sh 'docker build -t gseha/python-app:latest .'
                }
            }
        }
        
        // Étape optionnelle pour le moment - commentez-la
        /*
        stage('Push to Docker Hub') {
            steps {
                echo '📤 Pushing to Docker Hub...'
                script {
                    sh '''
                        docker login -u $DOCKERHUB_CREDENTIALS_USR -p $DOCKERHUB_CREDENTIALS_PSW
                        docker push gseha/python-app:latest
                    '''
                }
            }
        }
        */
        
        stage('Deploy to Staging') {
            steps {
                echo '🚀 Deploying to staging...'
                script {
                    sh '''
                        docker stop python-app || true
                        docker rm python-app || true
                        docker run -d --name python-app -p 5000:5000 gseha/python-app:latest
                    '''
                }
            }
        }
    }
    
    post {
        always {
            echo "Build status: ${currentBuild.currentResult}"
            script {
                echo '🧹 Cleaning up...'
                // Commande simplifiée pour éviter l'erreur de contexte
                sh 'docker logout || true'
            }
        }
        success {
            echo '🎉 Build successful! Application is running on http://localhost:5000'
        }
        failure {
            echo '❌ Build failed! Check the logs for details.'
        }
    }
}