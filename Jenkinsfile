pipeline {
    agent any  // ← CHANGEMENT ICI: Remplace 'docker' par 'any'
    
    tools {
        maven 'M3'  // ← Assure-toi que Maven est configuré dans Jenkins
    }
    
    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        SONAR_TOKEN = credentials('sonarcloud-token')
        SLACK_WEBHOOK = credentials('slack-webhook-url')
        SSH_CREDENTIALS = credentials('ssh-deploy-credentials')
    }
    
    stages {
        // Étape 1: Tests Automatisés
        stage('Tests Automatisés') {
            steps {
                sh 'mvn clean test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        // Étape 2: Analyse Qualité Code avec SonarCloud
        stage('Analyse Qualité Code') {
            when {
                branch 'main'
            }
            steps {
                withSonarQubeEnv('sonarcloud') {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=ton-projet-key -Dsonar.organization=ton-organisation -Dsonar.login=$SONAR_TOKEN'
                }
            }
        }
        
        // Étape 3: Compilation et Packaging (SANS DOCKER)
        stage('Build Application') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        // ÉTAPES SUPPRIMÉES : Docker build/push et déploiement
        // (À réactiver plus tard quand Docker fonctionnera)
        
        // Étape 4: Tests Validation Locaux
        stage('Tests Validation Locaux') {
            steps {
                script {
                    // Tests de santé locaux
                    sh '''
                        # Démarrer l'application localement pour tests
                        java -jar target/*.jar &
                        APP_PID=$!
                        
                        # Attendre le démarrage
                        sleep 10
                        
                        # Tests HTTP
                        curl -f http://localhost:8080/actuator/health || exit 1
                        curl -f http://localhost:8080/api/v1/hello || exit 1
                        
                        # Arrêter l'application
                        kill $APP_PID
                    '''
                }
            }
        }
    }
    
    // Notifications (commentées temporairement)
    post {
        success {
            echo "✅ Pipeline SUCCÈS - ${env.JOB_NAME} #${env.BUILD_NUMBER}"
            // slackSend channel: '#dev-notifications', message: "✅ Pipeline SUCCÈS", color: 'good'
        }
        failure {
            echo "❌ Pipeline ÉCHEC - ${env.JOB_NAME} #${env.BUILD_NUMBER}"
            // slackSend channel: '#dev-notifications', message: "❌ Pipeline ÉCHEC", color: 'danger'
        }
    }
}