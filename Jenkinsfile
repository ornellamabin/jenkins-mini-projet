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
                script {
                    // Nettoyer les caractères non-UTF-8
                    sh '''
                        find . -name "*.java" -exec sed -i "s/[^[:print:]]//g" {} \\;
                    '''
                }
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean compile test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    DOCKER_TAG = "build-${env.BUILD_NUMBER}"
                    sh """
                        echo "🐳 Building Docker image with tag: ${DOCKER_TAG}"
                        docker build -t gseha/springboot-app:${DOCKER_TAG} .
                        docker tag gseha/springboot-app:${DOCKER_TAG} gseha/springboot-app:latest
                        echo "✅ Docker image built successfully"
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
                            echo "📤 Pushing to Docker Hub as user: \$DOCKER_USERNAME"
                            
                            # Authentification
                            echo "\$DOCKER_PASSWORD" | docker login -u "\$DOCKER_USERNAME" --password-stdin
                            
                            # Push des images avec retry
                            push_image() {
                                local image=\$1
                                local attempts=3
                                local count=1
                                
                                while [ \$count -le \$attempts ]; do
                                    echo "Attempt \$count: Pushing \$image"
                                    if docker push \$image; then
                                        echo "✅ Success: \$image"
                                        return 0
                                    fi
                                    echo "⚠️ Attempt \$count failed"
                                    count=\$((count + 1))
                                    sleep 5
                                done
                                echo "❌ Failed to push \$image"
                                return 1
                            }
                            
                            push_image "gseha/springboot-app:latest"
                            push_image "gseha/springboot-app:${DOCKER_TAG}"
                            
                            # Nettoyage
                            docker logout
                            echo "🎉 All images pushed successfully to Docker Hub"
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
                            echo "🚀 Deploying to staging server: ${STAGING_SERVER_IP}"
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                echo "Starting deployment process..."
                                
                                # Arrêter et nettoyer l'ancien conteneur
                                sudo docker stop springboot-app 2>/dev/null || true
                                sudo docker rm springboot-app 2>/dev/null || true
                                
                                # Pull de la nouvelle image
                                echo "Pulling latest image from Docker Hub..."
                                sudo docker pull gseha/springboot-app:latest
                                
                                # Démarrer le nouveau conteneur
                                echo "Starting new container..."
                                sudo docker run -d \\
                                    --name springboot-app \\
                                    -p 80:8080 \\
                                    -e SPRING_PROFILES_ACTIVE=staging \\
                                    --restart unless-stopped \\
                                    gseha/springboot-app:latest
                                
                                echo "✅ Deployment completed successfully"
                            '
                        """
                    }
                }
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    echo "🧪 Performing smoke test on staging server"
                    retry(5) {
                        sleep 10
                        sh """
                            echo "Testing application health..."
                            response=\$(curl -s -f http://${STAGING_SERVER_IP}/actuator/health || echo "FAILED")
                            if [ "\$response" = "FAILED" ]; then
                                echo "❌ Health check failed"
                                exit 1
                            fi
                            echo "✅ Health check response: \$response"
                        """
                    }
                    echo "🎯 Application is healthy and running at http://${STAGING_SERVER_IP}"
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    try {
                        withSonarQubeEnv('sonarcloud') {
                            sh 'mvn sonar:sonar -Dsonar.projectKey=springboot-app -Dsonar.organization=gseha'
                        }
                    } catch (Exception e) {
                        echo "⚠️ SonarQube analysis skipped or failed: ${e.getMessage()}"
                        // Continuer même si SonarQube échoue
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "🧹 Cleaning up Docker resources"
                sh 'docker system prune -f 2>/dev/null || true'
            }
        }
        success {
            script {
                def message = """✅ SUCCESS - Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}
• Application: http://${STAGING_SERVER_IP}
• Docker Image: gseha/springboot-app:${DOCKER_TAG}
• Branch: ${env.BRANCH_NAME}"""
                
                slackSend (
                    channel: "${SLACK_CHANNEL}",
                    color: 'good',
                    message: message
                )
            }
        }
        failure {
            script {
                def message = """❌ FAILED - Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}
• Branch: ${env.BRANCH_NAME}
• Build URL: ${env.BUILD_URL}"""
                
                slackSend (
                    channel: "${SLACK_CHANNEL}",
                    color: 'danger',
                    message: message
                )
            }
        }
        unstable {
            script {
                def message = """⚠️ UNSTABLE - Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}
• Branch: ${env.BRANCH_NAME}
• Build URL: ${env.BUILD_URL}"""
                
                slackSend (
                    channel: "${SLACK_CHANNEL}",
                    color: 'warning',
                    message: message
                )
            }
        }
    }
}