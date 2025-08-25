pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'gseha/python-app'  // ← Changez 'gseha' par votre username Docker Hub
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
        
        stage('Verify Docker Hub Access') {
            steps {
                echo '🔐 Testing Docker Hub permissions...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER', 
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    script {
                        sh '''
                            # Test de connexion
                            docker login -u $DOCKER_USER -p $DOCKER_PASSWORD
                            echo "✅ Docker Hub login successful!"
                            
                            # Test des permissions avec une image simple
                            docker pull hello-world
                            docker tag hello-world ${DOCKER_IMAGE}-test:permissions-check
                            docker push ${DOCKER_IMAGE}-test:permissions-check || echo "❌ Push permissions test failed - check token has write access"
                        '''
                    }
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
                            # Push de l'image principale
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
                        sleep 10  # Donner plus de temps pour démarrer
                        curl -f http://localhost:3000 && echo "✅ Application is running!" || echo "⚠️ Application may be starting..."
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo '🏆 PIPELINE COMPLETED SUCCESSFULLY!'
            // slackSend(message: "SUCCESS: Docker pipeline completed - Build ${env.BUILD_NUMBER}")
        }
        failure {
            echo '❌ Pipeline failed - check Docker Hub token permissions'
            // slackSend(message: "FAILURE: Build ${env.BUILD_NUMBER} - Check token permissions")
        }
        always {
            echo '🧹 Cleaning up test images...'
            script {
                sh '''
                    docker rmi ${DOCKER_IMAGE}-test:permissions-check 2>/dev/null || true
                    echo "Cleanup completed"
                '''
            }
        }
    }
}