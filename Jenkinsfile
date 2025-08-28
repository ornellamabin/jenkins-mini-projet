pipeline {
    agent any // Utilise le nœud Jenkins directement
    
    environment {
        STAGING_SERVER_IP = '3.27.150.136'
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
            }
        }

        // ÉTAPE SIMPLIFIÉE : Déploiement direct du JAR
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    echo "🚀 Déploiement sur Staging (${STAGING_SERVER_IP})..."
                    sshagent(["${STAGING_SSH_CREDENTIALS}"]) {
                        // Copie le JAR sur le serveur
                        sh """
                            scp -o StrictHostKeyChecking=no target/springboot-app-1.0.0.jar ec2-user@${STAGING_SERVER_IP}:/home/ec2-user/
                        """
                        
                        // Démarre l'application Java directement
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                # Arrête l'ancienne instance
                                pkill -f "java -jar springboot-app" || true
                                sleep 2
                                
                                # Démarre la nouvelle version
                                nohup java -jar /home/ec2-user/springboot-app-1.0.0.jar --server.port=8080 > app.log 2>&1 &
                                sleep 5
                                
                                # Vérifie que l'application démarre
                                curl -f http://localhost:8080/actuator/health || exit 1
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
                        echo "🧪 Test de santé de l'application: http://${STAGING_SERVER_IP}:8080/actuator/health"
                        curl -s --retry 10 --retry-delay 5 --retry-all-errors -f http://${STAGING_SERVER_IP}:8080/actuator/health || exit 1
                        echo "✅ Application déployée avec succès!"
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
    }
}