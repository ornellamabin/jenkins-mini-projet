pipeline {
    agent any
    
    environment {
        STAGING_SERVER_IP = '3.27.255.232'
        STAGING_SSH_CREDENTIALS = 'ec2-production-key'
        DOCKERHUB_CREDENTIALS = 'docker-hub'
        SLACK_CHANNEL = '#jenkins-ci'
    }
    
    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean compile test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    DOCKER_TAG = "build-${env.BUILD_NUMBER}"
                    sh """
                        echo "🐳 Building Docker image..."
                        
                        # Déterminer la commande Docker à utiliser
                        if docker info >/dev/null 2>&1; then
                            echo "✅ Docker accessible directement"
                            DOCKER_CMD="docker"
                        else
                            echo "⚠️ Utilisation de sudo pour Docker"
                            DOCKER_CMD="sudo docker"
                            
                            # Tentative de résolution des permissions
                            sudo usermod -a -G docker \$USER 2>/dev/null || true
                            sudo chmod 666 /var/run/docker.sock 2>/dev/null || true
                        fi
                        
                        # Construction de l'image
                        \$DOCKER_CMD build -t gseha/springboot-app:${DOCKER_TAG} .
                        \$DOCKER_CMD tag gseha/springboot-app:${DOCKER_TAG} gseha/springboot-app:latest
                        
                        echo "✅ Image Docker construite avec succès"
                    """
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: "${DOCKERHUB_CREDENTIALS}", 
                        usernameVariable: 'DOCKER_USERNAME', 
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh """
                            echo "📤 Pushing to Docker Hub..."
                            
                            # Déterminer la commande Docker
                            if docker info >/dev/null 2>&1; then
                                DOCKER_CMD="docker"
                            else
                                DOCKER_CMD="sudo docker"
                            fi
                            
                            # Authentification
                            echo "\$DOCKER_PASSWORD" | \$DOCKER_CMD login -u "\$DOCKER_USERNAME" --password-stdin
                            
                            # Push des images
                            \$DOCKER_CMD push gseha/springboot-app:latest
                            \$DOCKER_CMD push gseha/springboot-app:${DOCKER_TAG}
                            
                            # Nettoyage
                            \$DOCKER_CMD logout
                            echo "✅ Images pushed successfully"
                        """
                    }
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                script {
                    sshagent(credentials: ["${STAGING_SSH_CREDENTIALS}"]) {
                        sh """
                            echo "🚀 Deploying to ${STAGING_SERVER_IP}"
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                sudo docker stop springboot-app 2>/dev/null || true
                                sudo docker rm springboot-app 2>/dev/null || true
                                sudo docker pull gseha/springboot-app:latest
                                sudo docker run -d \\
                                    --name springboot-app \\
                                    -p 80:8080 \\
                                    -e SPRING_PROFILES_ACTIVE=staging \\
                                    gseha/springboot-app:latest
                                echo "✅ Deployment completed"
                            '
                        """
                    }
                }
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    retry(5) {
                        sleep 10
                        sh """
                            curl -s -f http://${STAGING_SERVER_IP}/actuator/health || exit 1
                        """
                    }
                    echo "✅ Application is healthy"
                }
            }
        }
    }
    
    post {
        success {
            slackSend (
                channel: "${SLACK_CHANNEL}",
                color: 'good',
                message: "✅ SUCCESS - Application deployed to http://${STAGING_SERVER_IP}"
            )
        }
        failure {
            slackSend (
                channel: "${SLACK_CHANNEL}",
                color: 'danger',
                message: "❌ FAILED - Pipeline failed"
            )
        }
    }
}