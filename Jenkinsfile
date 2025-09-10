pipeline {
    agent any
    
    environment {
        STAGING_SERVER_IP = '3.27.255.232'
        PRODUCTION_SERVER_IP = 'votre-ip-production'
        STAGING_SSH_CREDENTIALS = 'ec2-production-key'
        PRODUCTION_SSH_CREDENTIALS = 'ec2-production-key'
        DOCKERHUB_CREDENTIALS = 'docker-hub'
        SONAR_TOKEN = credentials('sonar-token')
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
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('SonarQube Analysis') {
            agent {
                docker {
                    image 'maven:3.8.6-openjdk-17'
                    args '-v /root/.m2:/root/.m2'
                }
            }
            steps {
                withSonarQubeEnv('sonarcloud') {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=votre-project-key -Dsonar.organization=votre-organisation'
                }
            }
        }

        stage('Package Application') {
            agent {
                docker {
                    image 'maven:3.8.6-openjdk-17'
                    args '-v /root/.m2:/root/.m2'
                }
            }
            steps {
                sh 'mvn package -DskipTests'
                script {
                    JAR_FILE = sh(script: 'find target -name "*.jar" | head -1', returnStdout: true).trim()
                    DOCKER_TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}".replace('/', '-')
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "🐳 Building Docker image..."
                    // Utiliser Docker directement depuis le host
                    sh """
                        sudo docker build -t gseha/springboot-app:${DOCKER_TAG} .
                        sudo docker tag gseha/springboot-app:${DOCKER_TAG} gseha/springboot-app:latest
                    """
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    echo "📤 Pushing Docker image to Docker Hub..."
                    withCredentials([usernamePassword(credentialsId: "${DOCKERHUB_CREDENTIALS}", usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh """
                            echo "$DOCKER_PASSWORD" | sudo docker login -u "$DOCKER_USERNAME" --password-stdin
                            sudo docker push gseha/springboot-app:latest
                            sudo docker push gseha/springboot-app:${DOCKER_TAG}
                            sudo docker logout
                        """
                    }
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                script {
                    echo "🚀 Déploiement sur Staging (${STAGING_SERVER_IP})..."
                    sshagent(credentials: ["${STAGING_SSH_CREDENTIALS}"]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                # Vérifier si Docker est installé
                                if ! command -v docker &> /dev/null; then
                                    sudo yum install docker -y
                                    sudo service docker start
                                    sudo usermod -a -G docker ec2-user
                                fi
                                
                                # Arrêter le conteneur existant
                                sudo docker stop springboot-app || true
                                sudo docker rm springboot-app || true
                                
                                # Pull de la nouvelle image
                                sudo docker pull gseha/springboot-app:${DOCKER_TAG}
                                
                                # Démarrer le nouveau conteneur
                                sudo docker run -d \
                                    --name springboot-app \
                                    -p 8080:8080 \
                                    -e SPRING_PROFILES_ACTIVE=staging \
                                    gseha/springboot-app:${DOCKER_TAG}
                            '
                        """
                    }
                }
            }
        }

        stage('Staging Smoke Test') {
            steps {
                script {
                    echo "🧪 Test de santé de l'application sur staging..."
                    retry(5) {
                        sleep 10
                        sh """
                            curl -s -f http://${STAGING_SERVER_IP}:8080/actuator/health || exit 1
                        """
                    }
                    echo "✅ Application déployée avec succès sur staging!"
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Nettoyage des ressources Docker
                sh 'sudo docker system prune -f || true'
            }
        }
        success {
            script {
                def message = "✅ SUCCÈS - Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} (${env.BRANCH_NAME})"
                message += " - Déployé sur staging: http://${STAGING_SERVER_IP}:8080"
                
                slackSend (
                    channel: "${SLACK_CHANNEL}",
                    color: 'good',
                    message: message
                )
            }
        }
        failure {
            slackSend (
                channel: "${SLACK_CHANNEL}",
                color: 'danger',
                message: "❌ ÉCHEC - Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} (${env.BRANCH_NAME})"
            )
        }
    }
}