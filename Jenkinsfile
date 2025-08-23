pipeline {
    agent any
    
    environment {
        // Configuration Docker
        DOCKER_IMAGE = 'votreusername/springboot-app'
        DOCKER_TAG = "${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}"
        
        // Configuration des serveurs
        STAGING_SERVER = 'user@staging-server.com'
        PRODUCTION_SERVER = 'user@production-server.com'
    }
    
    tools {
        maven 'M3'
        jdk 'jdk17'
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
                    env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop'
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
        
        // ÉTAPE 3: COMPILATION ET PACKAGING
        stage('Compilation et Packaging') {
            steps {
                script {
                    echo '📦 Compilation et création du JAR...'
                    sh 'mvn clean package -DskipTests'
                    echo '✅ JAR créé avec succès!'
                }
            }
        }
        
        // ÉTAPE 4: CONSTRUCTION DOCKER
        stage('Build Docker') {
            steps {
                script {
                    echo '🐳 Construction de l image Docker...'
                    sh '''
                        docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        echo "Image Docker construite: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    '''
                }
            }
        }
        
        // ÉTAPE 5: PUSH DOCKER HUB
        stage('Push Docker Hub') {
            when {
                expression { 
                    env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop'
                }
            }
            steps {
                script {
                    echo '📤 Envoi de l image sur Docker Hub...'
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh '''
                            docker login -u $DOCKER_USER -p $DOCKER_PASSWORD
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                            docker logout
                        '''
                    }
                }
            }
        }
        
        // ÉTAPE 6: DÉPLOIEMENT STAGING
        stage('Déploiement Staging') {
            when {
                expression { 
                    env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop'
                }
            }
            steps {
                script {
                    echo '🚀 Déploiement en environnement staging...'
                    sshagent(['ssh-credentials']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${STAGING_SERVER} '
                                docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}
                                docker stop springboot-app-staging || true
                                docker rm springboot-app-staging || true
                                docker run -d -p 8080:8080 --name springboot-app-staging ${DOCKER_IMAGE}:${DOCKER_TAG}
                            '
                        """
                    }
                }
            }
        }
        
        // ÉTAPE 7: DÉPLOIEMENT PRODUCTION
        stage('Déploiement Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo '🎯 Déploiement en production...'
                    sshagent(['ssh-credentials']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${PRODUCTION_SERVER} '
                                docker pull ${DOCKER_IMAGE}:latest
                                docker stop springboot-app-production || true
                                docker rm springboot-app-production || true
                                docker run -d -p 80:8080 --name springboot-app-production ${DOCKER_IMAGE}:latest
                            '
                        """
                    }
                }
            }
        }
        
        // ÉTAPE 8: TESTS DE VALIDATION
        stage('Tests Validation') {
            when {
                expression { 
                    env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'develop'
                }
            }
            steps {
                script {
                    echo '✅ Validation des déploiements...'
                    sh '''
                        # Test staging
                        curl -f http://staging-server.com:8080/actuator/health || echo "Staging non accessible"
                        
                        # Test production (seulement sur main)
                        if [ "${BRANCH_NAME}" = "main" ]; then
                            curl -f http://production-server.com:80/actuator/health || echo "Production non accessible"
                        fi
                    '''
                    echo '🎉 Déploiements validés avec succès!'
                }
            }
        }
    }
    
    post {
        always {
            echo '🧹 Nettoyage des ressources...'
            sh 'docker logout || true'
        }
        
        success {
            script {
                echo '✅ Pipeline exécutée avec succès!'
                // Notification Slack
                slackSend(
                    channel: '#ci-cd',
                    message: "🎉 Pipeline SUCCÈS - ${env.JOB_NAME} #${env.BUILD_NUMBER}\nBranch: ${env.BRANCH_NAME}\nDétails: ${env.BUILD_URL}"
                )
            }
        }
        
        failure {
            script {
                echo '❌ Pipeline en échec!'
                // Notification Slack
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