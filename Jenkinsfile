pipeline {
    agent any
    
    environment {
        STAGING_SERVER_IP = '3.27.255.232'
        STAGING_SSH_CREDENTIALS = 'ec2-production-key'
        DOCKERHUB_CREDENTIALS = 'docker-hub'
        SLACK_CHANNEL = '#jenkins-ci'
        APP_PORT = '8090'
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
                                # Arrêter le conteneur existant
                                sudo docker stop springboot-app || true
                                sudo docker rm springboot-app || true
                                
                                # Pull de la nouvelle image
                                sudo docker pull gseha/springboot-app:latest
                                
                                # Démarrer le nouveau conteneur sur le port 8090
                                sudo docker run -d \\
                                    --name springboot-app \\
                                    -p ${APP_PORT}:8080 \\
                                    -e SPRING_PROFILES_ACTIVE=staging \\
                                    gseha/springboot-app:latest
                                
                                echo "✅ Application déployée sur le port ${APP_PORT}"
                            '
                        """
                    }
                }
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    echo "🧪 Test de santé de l'application sur le port ${APP_PORT}..."
                    retry(5) {
                        sleep 10
                        sh """
                            curl -s -f http://${STAGING_SERVER_IP}:${APP_PORT}/actuator/health || exit 1
                        """
                    }
                    echo "✅ Application fonctionne correctement sur http://${STAGING_SERVER_IP}:${APP_PORT}"
                }
            }
        }
    }
    
    post {
        success {
            slackSend (
                channel: "${SLACK_CHANNEL}",
                color: 'good',
                message: "✅ SUCCÈS - Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} - Application déployée sur http://${STAGING_SERVER_IP}:${APP_PORT}"
            )
        }
        failure {
            slackSend (
                channel: "${SLACK_CHANNEL}",
                color: 'danger',
                message: "❌ ÉCHEC - Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} - Déploiement échoué"
            )
        }
    }
}