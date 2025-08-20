pipeline {
    // Exécute toutes les étapes dans un conteneur Docker avec Maven
    agent {
        docker {
            image 'maven:3.8.6-openjdk-17'
            args '-v /root/.m2:/root/.m2' // Cache Maven pour accélérer les builds
        }
    }
    
    // Définition des variables d'environnement (secrets)
    environment {
        DOCKERHUB_CREDS = credentials('dockerhub-creds') // Identifiants DockerHub
        SONAR_TOKEN = credentials('sonarcloud-token')     // Token SonarCloud
        SLACK_WEBHOOK = credentials('slack-token')        // Webhook Slack
    }
    
    stages {
        // Étape 1: Récupération du code source
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/tonuser/ton-repo.git' // REMPLACE par ton URL GitHub
            }
        }
        
        // Étape 2: Compilation et tests unitaires
        stage('Build & Tests Unitaires') {
            steps {
                sh 'mvn clean compile test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml' // Archive les résultats des tests
                }
            }
        }
        
        // Étape 3: Analyse de la qualité du code avec SonarCloud
        stage('Analyse Qualité avec SonarCloud') {
            steps {
                withSonarQubeEnv('sonarcloud') { // 'sonarcloud' doit être configuré dans Jenkins
                    sh 'mvn sonar:sonar -Dsonar.projectKey=ton-project-key -Dsonar.login=$SONAR_TOKEN' // REMPLACE ton-project-key
                }
            }
        }
        
        // Étape 4: Construction de l'image Docker
        stage('Build de l\'image Docker') {
            steps {
                script {
                    // ICI - Ton nom d'utilisateur DockerHub est utilisé
                    def imageName = "gseha/my-spring-app:${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}"
                    docker.build(imageName)
                }
            }
        }
        
        // Étape 5: Envoi de l'image sur DockerHub
        stage('Push de l\'image Docker') {
            steps {
                script {
                    // ICI - Ton nom d'utilisateur DockerHub est utilisé
                    def imageName = "gseha/my-spring-app:${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}"
                    docker.withRegistry('', 'dockerhub-creds') {
                        docker.image(imageName).push()
                    }
                }
            }
        }
        
        // Étape 6: Déploiement en production (UNIQUEMENT sur la branche main)
        stage('Déploiement Production') {
            when {
                branch 'main' // Cette étape ne s'exécute que sur la branche principale
            }
            steps {
                script {
                    // Connexion SSH au serveur et exécution du script de déploiement
                    sshagent(['production-server-ssh']) {
                        sh "ssh -o StrictHostKeyChecking=no user@ton-serveur-production.com 'cd /opt/my-spring-app && ./deploy.sh'"
                    }
                }
            }
        }
        
        // Étape 7: Vérification que l'application fonctionne
        stage('Test de Validation') {
            when {
                branch 'main'
            }
            steps {
                script {
                    // Teste que l'application répond correctement
                    retry(3) { // Réessaye 3 fois si besoin
                        sleep 10 // Attend 10 secondes que l'application démarre
                        sh 'curl -f http://ton-serveur-production.com:8080/actuator/health || exit 1'
                    }
                }
            }
        }
    }
    
    // Actions finales après l'exécution de la pipeline
    post {
        always {
            // Notification Slack dans tous les cas
            slackSend(
                channel: '#ton-channel', // REMPLACE par ton canal Slack
                message: "Build ${currentBuild.result ?: 'SUCCESS'} - Job ${env.JOB_NAME} [${env.BUILD_NUMBER}] (<${env.BUILD_URL}|Open>)"
            )
        }
        failure {
            // Notification spéciale en cas d'échec
            slackSend(
                channel: '#ton-channel', // REMPLACE par ton canal Slack
                message: "ATTENTION : Échec du build ${env.JOB_NAME} [${env.BUILD_NUMBER}] ! (<${env.BUILD_URL}|Voir les logs>)",
                color: 'danger'
            )
        }
    }
}