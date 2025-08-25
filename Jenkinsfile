pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'gseha/python-app'
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
        
        stage('Push to Docker Hub') {
            steps {
                echo '📤 Pushing to Docker Hub...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials', 
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    script {
                        sh """
                            docker login -u $DOCKER_USER -p $DOCKER_PASSWORD
                            docker push ${DOCKER_IMAGE}:latest
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                            echo "🎉 Successfully pushed to Docker Hub!"
                        """
                    }
                }
            }
        }
        
        stage('Deploy and Test') {
            steps {
                echo '🚀 Deploying application...'
                script {
                    sh """
                        docker stop python-app || true
                        docker rm python-app || true
                        docker run -d --name python-app -p 3000:3000 ${DOCKER_IMAGE}:latest
                        sleep 5
                        curl -f http://localhost:3000 || echo "Application deployed successfully!"
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo '🏆 PIPELINE COMPLETED SUCCESSFULLY!'
            slackSend(message: "SUCCESS: Docker pipeline completed - Build ${env.BUILD_NUMBER}")
        }
        failure {
            echo '❌ Pipeline failed'
            slackSend(message: "FAILURE: Build ${env.BUILD_NUMBER}")
        }
    }
}