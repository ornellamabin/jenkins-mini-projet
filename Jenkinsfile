pipeline {
    agent any
    
    environment {
        STAGING_SERVER_IP = '3.27.255.232'
        // Assurez-vous que cet ID correspond exactement à Jenkins
        STAGING_SSH_CREDENTIALS = 'ec2-production-key' 
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
                    // Trouve dynamiquement le fichier JAR
                    JAR_FILE = sh(script: 'find target -name "*.jar" | head -1', returnStdout: true).trim()
                }
            }
        }

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
                        // Copie le JAR
                        sh """
                            scp -o StrictHostKeyChecking=no ${JAR_FILE} ec2-user@${STAGING_SERVER_IP}:/home/ec2-user/
                        """
                        
                        // Démarre l'application
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                # Arrêt propre
                                if pgrep -f "java.*springboot-app"; then
                                    pkill -f "java.*springboot-app"
                                    sleep 3
                                fi
                                
                                # Démarrage
                                nohup java -jar /home/ec2-user/${JAR_FILE} --server.port=8080 > app.log 2>&1 &
                                sleep 10
                                
                                # Vérification
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