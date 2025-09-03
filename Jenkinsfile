pipeline {
    agent any
    
    environment {
        STAGING_SERVER_IP = '3.27.255.232'
        STAGING_SSH_CREDENTIALS = 'ec2-production-key'
        DOCKERHUB_CREDENTIALS = 'docker-hub'  // ← Changé pour utiliser votre credential existant
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
        }

        stage('Package Application') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                script {
                    JAR_FILE = sh(script: 'find target -name "*.jar" | head -1', returnStdout: true).trim()
                }
            }
        }

        // ========== ÉTAPES DOCKER ==========
        stage('Build Docker Image') {
            steps {
                script {
                    echo "🐳 Building Docker image..."
                    sh 'docker build -t gseha/springboot-app:latest .'
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    echo "📤 Pushing Docker image to Docker Hub..."
                    withCredentials([usernamePassword(credentialsId: "${DOCKERHUB_CREDENTIALS}", usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh '''
                            echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                            docker push gseha/springboot-app:latest
                        '''
                    }
                }
            }
        }
        // ====================================

        stage('Install Java on Staging') {
            steps {
                script {
                    echo "📦 Installation de Java sur le serveur staging..."
                    sshagent(credentials: ["${STAGING_SSH_CREDENTIALS}"]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                sudo yum install java-17-amazon-corretto -y
                                java -version
                            '
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
                            scp -o StrictHostKeyChecking=no ${JAR_FILE} ec2-user@${STAGING_SERVER_IP}:/home/ec2-user/
                        """
                        
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                if pgrep -f "java.*springboot-app"; then
                                    pkill -f "java.*springboot-app"
                                    sleep 3
                                fi
                                
                                nohup java -jar /home/ec2-user/${JAR_FILE} --server.port=8080 > app.log 2>&1 &
                                sleep 10
                                
                                curl -f http://localhost:8080/actuator/health || exit 1
                                echo "✅ Application démarrée avec succès"
                            '
                        """
                    }
                }
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    sh """
                        echo "🧪 Test de santé de l'application..."
                        for i in {1..10}; do
                            if curl -s -f http://${STAGING_SERVER_IP}:8080/actuator/health; then
                                echo "✅ Application déployée avec succès!"
                                exit 0
                            fi
                            sleep 5
                        done
                        echo "❌ L'application ne répond pas"
                        exit 1
                    """
                }
            }
        }
    }
    
    post {
        success {
            slackSend (
                channel: '#jenkins-ci',
                color: 'good',
                message: "✅ SUCCÈS - Application déployée sur http://${STAGING_SERVER_IP}:8080"
            )
        }
        failure {
            slackSend (
                channel: '#jenkins-ci',
                color: 'danger',
                message: "❌ ÉCHEC - Déploiement échoué sur ${STAGING_SERVER_IP}"
            )
        }
    }
}