pipeline {
    agent {
        docker {
            image 'maven:3.8.5-openjdk-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock --user root'
        }
    }

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        SLACK_CHANNEL = '#ci-cd-notifications'
        SONAR_TOKEN = credentials('sonarcloud-token')
        SSH_CREDENTIALS = credentials('ssh-credentials')
        GITHUB_TOKEN = credentials('github-token')
        
        // Configuration des serveurs
        STAGING_SERVER = 'votre-staging-server-ip'
        PRODUCTION_SERVER = 'votre-production-server-ip'
    }

    stages {
        // Étape 1: Tests Automatisés
        stage('Tests Automatisés') {
            steps {
                echo '🧪 Exécution des tests unitaires...'
                sh 'mvn test -Dmaven.test.failure.ignore=true'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts 'target/*.jar'
                }
            }
        }

        // Étape 2: Analyse Qualité Code avec SonarCloud
        stage('Analyse SonarCloud') {
            steps {
                echo '🔍 Analyse de la qualité du code avec SonarCloud...'
                withSonarQubeEnv('SonarCloud') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=ornellamabin_springboot-app \
                          -Dsonar.organization=ornellamabin \
                          -Dsonar.host.url=https://sonarcloud.io \
                          -Dsonar.login=$SONAR_TOKEN \
                          -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                          -Dsonar.java.binaries=target/classes \
                          -Dsonar.sourceEncoding=UTF-8
                    '''
                }
            }
        }

        // Étape 3: Vérification Quality Gate
        stage('Quality Gate') {
            steps {
                echo '🚦 Vérification de la Quality Gate...'
                timeout(time: 20, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // Étape 4: Compilation et Packaging
        stage('Build et Packaging') {
            when {
                expression { 
                    currentBuild.result == null || currentBuild.result == 'SUCCESS' 
                }
            }
            steps {
                echo '📦 Construction de l\'application...'
                sh 'mvn clean package -DskipTests'
            }
        }

        // Étape 5: Construction Image Docker
        stage('Build Image Docker') {
            when {
                expression { 
                    currentBuild.result == null || currentBuild.result == 'SUCCESS' 
                }
            }
            steps {
                echo '🐳 Construction de l\'image Docker...'
                script {
                    def version = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()
                    env.APP_VERSION = version
                }
                sh '''
                    docker build -t $DOCKERHUB_CREDENTIALS_USR/spring-app:${APP_VERSION} .
                    docker tag $DOCKERHUB_CREDENTIALS_USR/spring-app:${APP_VERSION} $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                '''
            }
        }

        // Étape 6: Push vers DockerHub
        stage('Push DockerHub') {
            when {
                expression { 
                    currentBuild.result == null || currentBuild.result == 'SUCCESS' 
                }
            }
            steps {
                echo '📤 Envoi vers DockerHub...'
                sh '''
                    echo "$DOCKERHUB_CREDENTIALS_PSW" | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin
                    docker push $DOCKERHUB_CREDENTIALS_USR/spring-app:${APP_VERSION}
                    docker push $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                '''
            }
        }

        // Étape 7: Déploiement Staging (uniquement sur main)
        stage('Déploiement Staging') {
            when {
                branch 'main'
                expression { 
                    currentBuild.result == null || currentBuild.result == 'SUCCESS' 
                }
            }
            steps {
                echo '🚀 Déploiement en environnement staging...'
                sshagent(['ssh-credentials']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${env.STAGING_SERVER} "
                            docker pull $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                            docker stop spring-app-staging || true
                            docker rm spring-app-staging || true
                            docker run -d --name spring-app-staging -p 8080:8080 \
                                -e SPRING_PROFILES_ACTIVE=staging \
                                $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                        "
                    """
                }
            }
        }

        // Étape 8: Déploiement Production (uniquement sur main)
        stage('Déploiement Production') {
            when {
                branch 'main'
                expression { 
                    currentBuild.result == null || currentBuild.result == 'SUCCESS' 
                }
            }
            steps {
                echo '🎯 Déploiement en production...'
                sshagent(['ssh-credentials']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${env.PRODUCTION_SERVER} "
                            docker pull $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                            docker stop spring-app-production || true
                            docker rm spring-app-production || true
                            docker run -d --name spring-app-production -p 80:8080 \
                                -e SPRING_PROFILES_ACTIVE=production \
                                $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                        "
                    """
                }
            }
        }

        // Étape 9: Tests de Validation
        stage('Tests Validation') {
            when {
                branch 'main'
                expression { 
                    currentBuild.result == null || currentBuild.result == 'SUCCESS' 
                }
            }
            steps {
                echo '✅ Validation des déploiements...'
                script {
                    // Test santé staging
                    try {
                        def stagingHealth = sh(script: """
                            curl -s -o /dev/null -w '%{http_code}' http://${env.STAGING_SERVER}:8080/actuator/health || echo "503"
                        """, returnStdout: true).trim()
                        
                        if (stagingHealth == "200") {
                            echo "✅ Staging health check: OK"
                        } else {
                            error "❌ Staging health check failed: HTTP $stagingHealth"
                        }
                    } catch (Exception e) {
                        error "❌ Staging health check failed: ${e.message}"
                    }

                    // Test santé production
                    try {
                        def productionHealth = sh(script: """
                            curl -s -o /dev/null -w '%{http_code}' http://${env.PRODUCTION_SERVER}/actuator/health || echo "503"
                        """, returnStdout: true).trim()
                        
                        if (productionHealth == "200") {
                            echo "✅ Production health check: OK"
                        } else {
                            error "❌ Production health check failed: HTTP $productionHealth"
                        }
                    } catch (Exception e) {
                        error "❌ Production health check failed: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Statut du build: ${currentBuild.currentResult}"
            
            // Notification Slack
            slackSend(
                channel: env.SLACK_CHANNEL,
                color: currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger',
                message: """
                *${env.JOB_NAME}* - Build #${env.BUILD_NUMBER}
                *Branche:* ${env.GIT_BRANCH}
                *Statut:* ${currentBuild.currentResult}
                *Durée:* ${currentBuild.durationString}
                *URL:* ${env.BUILD_URL}
                """
            )
        }

        success {
            echo '🎉 Pipeline exécutée avec succès!'
            slackSend(
                channel: '#deployments',
                message: "✅ DÉPLOIEMENT RÉUSSI: ${env.JOB_NAME} v${env.APP_VERSION} déployé en production!"
            )
        }

        failure {
            echo '❌ Pipeline échouée! Vérifiez les logs pour plus de détails.'
            slackSend(
                channel: '#ci-errors',
                message: "❌ BUILD ÉCHOUÉ: ${env.JOB_NAME} nécessite une attention!\n${env.BUILD_URL}"
            )
        }

        unstable {
            echo '⚠️ Pipeline instable! Tests échoués mais déploiement continué.'
            slackSend(
                channel: '#ci-warnings',
                message: "⚠️ BUILD INSTABLE: ${env.JOB_NAME} a des échecs de tests\n${env.BUILD_URL}"
            )
        }
    }
}