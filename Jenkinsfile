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
                        docker build -t gseha/springboot-app:${DOCKER_TAG} . || sudo docker build -t gseha/springboot-app:${DOCKER_TAG} .
                        docker tag gseha/springboot-app:${DOCKER_TAG} gseha/springboot-app:latest || sudo docker tag gseha/springboot-app:${DOCKER_TAG} gseha/springboot-app:latest
                    """
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "${DOCKERHUB_CREDENTIALS}", usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh """
                            echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin || echo "$DOCKER_PASSWORD" | sudo docker login -u "$DOCKER_USERNAME" --password-stdin
                            docker push gseha/springboot-app:latest || sudo docker push gseha/springboot-app:latest
                            docker push gseha/springboot-app:${DOCKER_TAG} || sudo docker push gseha/springboot-app:${DOCKER_TAG}
                            docker logout || sudo docker logout
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
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                echo "🚀 Starting deployment..."
                                sudo docker stop springboot-app || true
                                sudo docker rm springboot-app || true
                                sudo docker pull gseha/springboot-app:latest
                                sudo docker run -d \\
                                    --name springboot-app \\
                                    -p 80:8080 \\
                                    -e SPRING_PROFILES_ACTIVE=staging \\
                                    gseha/springboot-app:latest
                                echo "✅ Application deployed successfully"
                            '
                        """
                    }
                }
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    echo "🧪 Testing application health..."
                    retry(5) {
                        sleep 10
                        sh """
                            curl -s -f http://${STAGING_SERVER_IP}/actuator/health || exit 1
                        """
                    }
                    echo "✅ Application is running successfully at http://${STAGING_SERVER_IP}"
                }
            }
        }
    }
    
    post {
        success {
            slackSend (
                channel: "${SLACK_CHANNEL}",
                color: 'good',
                message: "✅ SUCCESS - Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} - Application deployed to http://${STAGING_SERVER_IP}"
            )
        }
        failure {
            slackSend (
                channel: "${SLACK_CHANNEL}",
                color: 'danger',
                message: "❌ FAILED - Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} - Deployment failed"
            )
        }
        always {
            script {
                // Cleanup
                sh 'docker system prune -f || sudo docker system prune -f || true'
            }
        }
    }
}