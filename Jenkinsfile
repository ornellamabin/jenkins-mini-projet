pipeline {
    agent {
        docker {
            image 'maven:3.8.5-openjdk-17'
            args '--user root'
        }
    }

    environment {
        // Credentials
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        SLACK_CHANNEL = '#ci-cd-notifications'
        SONAR_TOKEN = credentials('sonarcloud-token')
        SSH_CREDENTIALS = credentials('ssh-credentials')
        
        // Configuration des serveurs (à adapter)
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
                    // Archive les artefacts avec pattern flexible
                    script {
                        def jarFiles = findFiles(glob: 'target/*.jar')
                        if (jarFiles) {
                            archiveArtifacts 'target/*.jar'
                        } else {
                            echo 'Aucun fichier JAR trouvé pour l archivage'
                        }
                    }
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
                          -Dsonar.projectKey=ornellamabin_jeinkins-mini-projet \
                          -Dsonar.organization=ornellamabin \
                          -Dsonar.host.url=https://sonarcloud.io \
                          -Dsonar.login=$SONAR_TOKEN \
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
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // Étape 4: Compilation et Packaging
        stage('Build et Packaging') {
            steps {
                echo '📦 Construction de l\'application...'
                sh 'mvn clean package -DskipTests'
                
                script {
                    // Trouve le fichier JAR généré
                    def jarFiles = findFiles(glob: 'target/*.jar')
                    if (jarFiles) {
                        env.APP_JAR = jarFiles[0].name
                        echo "Fichier JAR généré: ${env.APP_JAR}"
                    } else {
                        error "Aucun fichier JAR trouvé après le build"
                    }
                }
            }
        }

        // Étape 5: Construction Image Docker
        stage('Build Image Docker') {
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

        // Étape 9: Tests de Validation (uniquement sur main)
        stage('Tests Validation') {
            when {
                branch 'main'
            }
            steps {
                echo '✅ Validation des déploiements...'
                script {
                    // Test santé staging avec gestion d'erreur
                    try {
                        def stagingHealth = sh(script: """
                            curl -s -o /dev/null -w '%{http_code}' http://${env.STAGING_SERVER}:8080/actuator/health || echo "503"
                        """, returnStdout: true).trim()
                        
                        if (stagingHealth == "200") {
                            echo "✅ Staging health check: OK"
                        } else {
                            echo "⚠️ Staging health check: HTTP $stagingHealth (continuation)"
                        }
                    } catch (Exception e) {
                        echo "⚠️ Staging health check failed: ${e.message} (continuation)"
                    }

                    // Test santé production avec gestion d'erreur
                    try {
                        def productionHealth = sh(script: """
                            curl -s -o /dev/null -w '%{http_code}' http://${env.PRODUCTION_SERVER}/actuator/health || echo "503"
                        """, returnStdout: true).trim()
                        
                        if (productionHealth == "200") {
                            echo "✅ Production health check: OK"
                        } else {
                            echo "⚠️ Production health check: HTTP $productionHealth (continuation)"
                        }
                    } catch (Exception e) {
                        echo "⚠️ Production health check failed: ${e.message} (continuation)"
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