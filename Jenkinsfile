pipeline {
    agent {
        docker {
            image 'maven:3.9.6-eclipse-temurin-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/usr/bin/docker'
        }
    }
    
    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        SSH_CREDENTIALS = credentials('ssh-credentials')
        SONAR_TOKEN = credentials('sonarcloud-token')
        SLACK_WEBHOOK = credentials('slack-webhook')
        
        // Configuration Docker
        DOCKER_IMAGE = 'votreusername/springboot-app'
        DOCKER_TAG = "${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}"
        
        // Configuration déploiement
        STAGING_SERVER = 'user@staging-server.com'
        PRODUCTION_SERVER = 'user@production-server.com'
    }
    
    stages {
        // ÉTAPE 1: TESTS AUTOMATISÉS
        stage('Tests Automatisés') {
            steps {
                script {
                    echo '🧪 Exécution des tests unitaires et d intégration...'
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        // ÉTAPE 2: QUALITÉ DE CODE AVEC SONARCLOUD
        stage('Qualité de Code') {
            when {
                expression { 
                    env.BRANCH_NAME == 'main' || 
                    env.BRANCH_NAME == 'develop' 
                }
            }
            steps {
                script {
                    echo '🔍 Analyse statique avec SonarCloud...'
                    withSonarQubeEnv('sonarcloud') {
                        sh 'mvn sonar:sonar -Dsonar.projectKey=your-project-key -Dsonar.organization=your-org'
                    }
                }
            }
        }
        
        // ÉTAPE 3: COMPILATION ET PACKAGING DOCKER
        stage('Compilation et Packaging') {
            steps {
                script {
                    echo '📦 Compilation et création du JAR...'
                    sh 'mvn clean package -DskipTests'
                    
                    echo '🐳 Construction de l image Docker...'
                    sh """
                        docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} .
                        docker login -u ${env.DOCKERHUB_CREDENTIALS_USR} -p ${env.DOCKERHUB_CREDENTIALS_PSW}
                        docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                    """
                }
            }
        }
        
        // ÉTAPE 4: DÉPLOIEMENT STAGING (Seulement sur main)
        stage('Déploiement Staging') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo '🚀 Déploiement en environnement de staging...'
                    sshagent([env.SSH_CREDENTIALS]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${env.STAGING_SERVER} '
                                docker pull ${env.DOCKER_IMAGE}:latest
                                docker stop springboot-app-staging || true
                                docker rm springboot-app-staging || true
                                docker run -d --name springboot-app-staging -p 8080:8080 ${env.DOCKER_IMAGE}:latest
                            '
                        """
                    }
                }
            }
        }
        
        // ÉTAPE 5: DÉPLOIEMENT PRODUCTION (Seulement sur main après validation)
        stage('Déploiement Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo '🎯 Déploiement en production...'
                    sshagent([env.SSH_CREDENTIALS]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${env.PRODUCTION_SERVER} '
                                docker pull ${env.DOCKER_IMAGE}:latest
                                docker stop springboot-app-production || true
                                docker rm springboot-app-production || true
                                docker run -d --name springboot-app-production -p 80:8080 ${env.DOCKER_IMAGE}:latest
                            '
                        """
                    }
                }
            }
        }
        
        // ÉTAPE 6: TESTS DE VALIDATION
        stage('Tests de Validation') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo '✅ Validation du déploiement...'
                    sh """
                        curl -f http://${env.PRODUCTION_SERVER}:80/actuator/health || exit 1
                        curl -f http://${env.STAGING_SERVER}:8080/actuator/health || exit 1
                    """
                    echo '🎉 Déploiement validé avec succès!'
                }
            }
        }
    }
    
    post {
        always {
            echo '🧹 Nettoyage des ressources...'
            sh 'docker logout'
        }
        
        success {
            script {
                echo '✅ Pipeline exécutée avec succès!'
                slackSend(
                    channel: '#ci-cd',
                    message: "🎉 Pipeline SUCCÈS - ${env.JOB_NAME} #${env.BUILD_NUMBER}\nBranch: ${env.BRANCH_NAME}\nDétails: ${env.BUILD_URL}"
                )
            }
        }
        
        failure {
            script {
                echo '❌ Pipeline en échec!'
                slackSend(
                    channel: '#ci-cd',
                    message: "🚨 Pipeline ÉCHEC - ${env.JOB_NAME} #${env.BUILD_NUMBER}\nBranch: ${env.BRANCH_NAME}\nDétails: ${env.BUILD_URL}"
                )
            }
        }
        
        unstable {
            script {
                echo '⚠️ Pipeline instable (qualité de code)'
                slackSend(
                    channel: '#ci-cd',
                    message: "⚠️ Pipeline INSTABLE - ${env.JOB_NAME} #${env.BUILD_NUMBER}\nBranch: ${env.BRANCH_NAME}\nDétails: ${env.BUILD_URL}"
                )
            }
        }
    }
}