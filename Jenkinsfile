pipeline {
    agent {
        docker {
            image 'python:3.9-slim'
            args '-v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/usr/bin/docker'
        }
    }
    
    environment {
        DOCKER_IMAGE = 'gseha/python-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Install Dependencies') {
            steps {
                echo '📦 Installing Python dependencies...'
                sh 'pip install -r requirements.txt'
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo '🧪 Running unit tests...'
                // Ajoutez vos commandes de test Python ici
                sh 'python -m pytest tests/ || true' // Remplacez par votre framework de test
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                sh '''
                    docker build -t ${DOCKER_IMAGE}:latest .
                    docker tag ${DOCKER_IMAGE}:latest ${DOCKER_IMAGE}:${DOCKER_TAG}
                '''
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
                sshagent(['ssh-staging-credentials']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no user@staging-server "
                            docker pull ${DOCKER_IMAGE}:latest
                            docker stop staging-app || true
                            docker rm staging-app || true
                            docker run -d --name staging-app -p 3000:3000 ${DOCKER_IMAGE}:latest
                        "
                    """
                }
            }
        }
        
        stage('Validation Tests') {
            steps {
                echo '✅ Running validation tests...'
                script {
                    try {
                        sh 'curl -f http://staging-server:3000/health'
                        echo "✅ Staging health check passed"
                    } catch (Exception e) {
                        echo "❌ Staging health check failed: ${e}"
                    }
                }
            }
        }
    }
    
    post {
        always {
            slackSend(
                channel: '#jenkins-notifications',
                message: "Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.currentResult}",
                color: currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger'
            )
        }
        
        cleanup {
            echo '🧹 Cleaning up...'
            sh 'docker logout'
        }
    }
}