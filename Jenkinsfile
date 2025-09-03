pipeline {
    agent any
    
    environment {
        STAGING_SERVER_IP = '3.27.150.136'
        STAGING_SSH_CREDENTIALS = 'ec2-production-key'
        JAR_FILE = sh(script: 'ls target/*.jar | head -1', returnStdout: true).trim()
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
                    JAR_FILE = sh(script: 'ls target/*.jar | head -1', returnStdout: true).trim()
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                script {
                    echo "🚀 Déploiement sur Staging (${STAGING_SERVER_IP})..."
                    sshagent(["${STAGING_SSH_CREDENTIALS}"]) {
                        // Vérification et copie
                        sh """
                            set -e
                            ls -la ${JAR_FILE} || exit 1
                            scp -o StrictHostKeyChecking=no ${JAR_FILE} ec2-user@${STAGING_SERVER_IP}:/home/ec2-user/
                        """
                        
                        // Déploiement sécurisé
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                set -e
                                # Arrêt propre sur le port 8080
                                if lsof -ti:8080; then
                                    echo "Arrêt de l'application existante..."
                                    kill $(lsof -ti:8080)
                                    sleep 3
                                fi
                                
                                # Démarrage
                                echo "Démarrage de la nouvelle version..."
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
                        set -e
                        echo "🧪 Test de santé de l'application..."
                        curl -s --retry 10 --retry-delay 5 --retry-all-errors -f http://${STAGING_SERVER_IP}:8080/actuator/health
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
        failure {
            slackSend (
                channel: '#jenkins-ci',
                color: 'danger',
                message: "❌ ÉCHEC - Déploiement échoué sur ${STAGING_SERVER_IP}"
            )
        }
    }
}