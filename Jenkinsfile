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
            agent {
                docker {
                    image 'maven:3.8.6-openjdk-17'
                    args '-v /root/.m2:/root/.m2'
                }
            }
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
            agent {
                docker {
                    image 'docker:20.10-dind'
                    args '--privileged --network host -v /var/run/docker.sock:/var/run/docker.sock'
                    reuseNode true
                }
            }
            steps {
                script {
                    DOCKER_TAG = "build-${env.BUILD_NUMBER}"
                    sh """
                        echo "🐳 Building Docker image using DinD..."
                        docker build -t gseha/springboot-app:${DOCKER_TAG} .
                        docker tag gseha/springboot-app:${DOCKER_TAG} gseha/springboot-app:latest
                        echo "✅ Docker image built successfully"
                    """
                }
            }
        }

        stage('Push to Docker Hub') {
            agent {
                docker {
                    image 'docker:20.10-dind'
                    args '--privileged --network host -v /var/run/docker.sock:/var/run/docker.sock'
                    reuseNode true
                }
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: "${DOCKERHUB_CREDENTIALS}", 
                        usernameVariable: 'DOCKER_USERNAME', 
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh """
                            echo "📤 Pushing to Docker Hub as user: \$DOCKER_USERNAME"
                            echo "\$DOCKER_PASSWORD" | docker login -u "\$DOCKER_USERNAME" --password-stdin
                            docker push gseha/springboot-app:latest
                            docker push gseha/springboot-app:${DOCKER_TAG}
                            docker logout
                            echo "✅ Images pushed successfully to Docker Hub"
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
                    echo "🧪 Testing application health..."
                    retry(5) {
                        sleep 10
                        sh """
                            curl -s -f http://${STAGING_SERVER_IP}/actuator/health || exit 1
                        """
                    }
                    echo "✅ Application is healthy at http://${STAGING_SERVER_IP}"
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