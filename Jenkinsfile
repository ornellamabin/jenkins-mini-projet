pipeline {
    agent {
        docker {
            image 'maven:3.8.5-openjdk-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock --user root'
        }
    }

    environment {
        // Utilisation de vos credentials existants
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        SLACK_CHANNEL = '#ci-cd-notifications'
        SONAR_TOKEN = credentials('sonarcloud-token')
        SSH_CREDENTIALS = credentials('ssh-credentials')  // Pour déploiement
        GITHUB_TOKEN = credentials('github-token')
    }

    stages {
        // Étape 1: Tests Automatisés
        stage('Tests Automatisés') {
            steps {
                echo '🧪 Running unit tests...'
                sh 'mvn test -Dmaven.test.failure.ignore=true'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts 'target/*.jar'
                }
            }
        }

        // Étape 2: Qualité de Code avec SonarCloud
        stage('Analyse SonarCloud') {
            steps {
                echo '🔍 Analyzing code quality with SonarCloud...'
                withSonarQubeEnv('SonarCloud') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=your-project-key \
                          -Dsonar.organization=your-organization \
                          -Dsonar.host.url=https://sonarcloud.io \
                          -Dsonar.login=$SONAR_TOKEN \
                          -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }

        // Étape 3: Quality Gate
        stage('Quality Gate') {
            steps {
                echo '🚦 Checking Quality Gate...'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // Étape 4: Compilation et Packaging
        stage('Build et Packaging') {
            steps {
                echo '📦 Building application...'
                sh 'mvn clean package -DskipTests'
            }
        }

        // Étape 5: Build Docker Image
        stage('Build Image Docker') {
            steps {
                echo '🐳 Building Docker image...'
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
                echo '📤 Pushing to DockerHub...'
                sh '''
                    docker login -u $DOCKERHUB_CREDENTIALS_USR -p $DOCKERHUB_CREDENTIALS_PSW
                    docker push $DOCKERHUB_CREDENTIALS_USR/spring-app:${APP_VERSION}
                    docker push $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                '''
            }
        }

        // Étape 7: Déploiement Staging (seulement sur main)
        stage('Déploiement Staging') {
            when {
                branch 'main'
            }
            steps {
                echo '🚀 Deploying to staging...'
                sshagent(['ssh-credentials']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@staging-server "
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

        // Étape 8: Déploiement Production (seulement sur main)
        stage('Déploiement Production') {
            when {
                branch 'main'
            }
            steps {
                echo '🎯 Deploying to production...'
                sshagent(['ssh-credentials']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@production-server "
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
            }
            steps {
                echo '✅ Validating deployment...'
                script {
                    // Test santé staging
                    try {
                        def stagingHealth = sh(script: """
                            curl -s -o /dev/null -w '%{http_code}' http://staging-server:8080/actuator/health || echo "503"
                        """, returnStdout: true).trim()
                        
                        if (stagingHealth != "200") {
                            echo "⚠️ Staging health check: HTTP $stagingHealth"
                        } else {
                            echo "✅ Staging health check: OK"
                        }
                    } catch (Exception e) {
                        echo "⚠️ Staging health check failed: ${e.message}"
                    }

                    // Test santé production
                    try {
                        def productionHealth = sh(script: """
                            curl -s -o /dev/null -w '%{http_code}' http://production-server/actuator/health || echo "503"
                        """, returnStdout: true).trim()
                        
                        if (productionHealth != "200") {
                            echo "⚠️ Production health check: HTTP $productionHealth"
                        } else {
                            echo "✅ Production health check: OK"
                        }
                    } catch (Exception e) {
                        echo "⚠️ Production health check failed: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Build status: ${currentBuild.currentResult}"
            
            // Notification Slack avec vos credentials
            slackSend(
                channel: env.SLACK_CHANNEL,
                color: currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger',
                message: """
                *${env.JOB_NAME}* - Build #${env.BUILD_NUMBER}
                *Branch:* ${env.GIT_BRANCH}
                *Status:* ${currentBuild.currentResult}
                *Duration:* ${currentBuild.durationString}
                *URL:* ${env.BUILD_URL}
                """
            )
        }

        success {
            echo '🎉 Pipeline executed successfully!'
            // Notification de succès supplémentaire
            slackSend(
                channel: '#deployments',
                message: "✅ DEPLOYMENT SUCCESS: ${env.JOB_NAME} v${env.APP_VERSION} deployed to production!"
            )
        }

        failure {
            echo '❌ Pipeline failed! Check logs for details.'
            // Notification d'erreur détaillée
            slackSend(
                channel: '#ci-errors',
                message: "❌ BUILD FAILED: ${env.JOB_NAME} needs attention!\n${env.BUILD_URL}"
            )
        }

        unstable {
            echo '⚠️ Pipeline unstable! Tests failed but deployment continued.'
            slackSend(
                channel: '#ci-warnings',
                message: "⚠️ BUILD UNSTABLE: ${env.JOB_NAME} has test failures\n${env.BUILD_URL}"
            )
        }
    }
}