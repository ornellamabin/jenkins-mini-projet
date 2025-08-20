pipeline {
    agent {
        docker {
            image 'maven:3.9.9-eclipse-temurin-17'
            args '-v $HOME/.m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock'
        }
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
                    sh 'mvn sonar:sonar -Dsonar.projectKey=ton-projet-key -Dsonar.organization=ton-organisation'
                }
            }
        }
        
        // Étape 3: Compilation et Packaging Docker
        stage('Build et Packaging Docker') {
            steps {
                script {
                    // Build de l'application
                    sh 'mvn clean package -DskipTests'
                    
                    // Build de l'image Docker
                    docker.build("votre-dockerhub-username/springboot-app:${env.BRANCH_NAME}-${env.BUILD_NUMBER}")
                }
            }
        }
        
        // Étape 4: Push vers DockerHub
        stage('Push vers DockerHub') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'dockerhub-credentials') {
                        docker.image("votre-dockerhub-username/springboot-app:${env.BRANCH_NAME}-${env.BUILD_NUMBER}").push()
                    }
                }
            }
        }
        
        // Étape 5: Déploiement Staging (uniquement sur main)
        stage('Déploiement Staging') {
            when {
                branch 'main'
            }
            steps {
                script {
                    def remote = [:]
                    remote.name = 'staging-server'
                    remote.host = 'votre-server-staging.com'
                    remote.user = 'deploy-user'
                    remote.identityFile = '/path/to/ssh/key'
                    
                    sshCommand remote: remote, command: """
                        docker pull votre-dockerhub-username/springboot-app:main-${env.BUILD_NUMBER}
                        docker stop springboot-app-staging || true
                        docker rm springboot-app-staging || true
                        docker run -d -p 8080:8080 --name springboot-app-staging \
                            votre-dockerhub-username/springboot-app:main-${env.BUILD_NUMBER}
                    """
                }
            }
        }
        
        // Étape 6: Tests Validation Staging
        stage('Tests Validation Staging') {
            when {
                branch 'main'
            }
            steps {
                script {
                    // Attendre que l'application soit ready
                    sleep time: 30, unit: 'SECONDS'
                    
                    // Tests de santé
                    sh '''
                        curl -f http://votre-server-staging.com:8080/api/v1/health || exit 1
                        curl -f http://votre-server-staging.com:8080/api/v1/hello || exit 1
                    '''
                }
            }
        }
        
        // Étape 7: Déploiement Production (manuel pour approbation)
        stage('Approbation Production') {
            when {
                branch 'main'
            }
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    input message: 'Déployer en production?', ok: 'Déployer'
                }
            }
        }
        
        stage('Déploiement Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    def remote = [:]
                    remote.name = 'production-server'
                    remote.host = 'votre-server-production.com'
                    remote.user = 'deploy-user'
                    remote.identityFile = '/path/to/ssh/key'
                    
                    sshCommand remote: remote, command: """
                        docker pull votre-dockerhub-username/springboot-app:main-${env.BUILD_NUMBER}
                        docker stop springboot-app-production || true
                        docker rm springboot-app-production || true
                        docker run -d -p 8080:8080 --name springboot-app-production \
                            -e SPRING_PROFILES_ACTIVE=production \
                            votre-dockerhub-username/springboot-app:main-${env.BUILD_NUMBER}
                    """
                }
            }
        }
        
        // Étape 8: Tests Validation Production
        stage('Tests Validation Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    sleep time: 30, unit: 'SECONDS'
                    
                    sh '''
                        curl -f https://votre-app-production.com/api/v1/health || exit 1
                        curl -f https://votre-app-production.com/api/v1/hello || exit 1
                    '''
                }
            }
        }
    }
    
    // Notifications Slack
    post {
        success {
            slackSend channel: '#dev-notifications',
                     message: "✅ Pipeline SUCCÈS - ${env.JOB_NAME} #${env.BUILD_NUMBER} (${currentBuild.currentResult})",
                     color: 'good'
        }
        failure {
            slackSend channel: '#dev-notifications',
                     message: "❌ Pipeline ÉCHEC - ${env.JOB_NAME} #${env.BUILD_NUMBER} (${currentBuild.currentResult})",
                     color: 'danger'
        }
        unstable {
            slackSend channel: '#dev-notifications',
                     message: "⚠️ Pipeline INSTABLE - ${env.JOB_NAME} #${env.BUILD_NUMBER} (${currentBuild.currentResult})",
                     color: 'warning'
        }
    }
}