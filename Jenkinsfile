pipeline {
    agent {
        docker {
            image 'maven:3.8.5-openjdk-17'
        }
    }

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        SSH_CREDENTIALS = credentials('ssh-credentials')
        
        // Configuration des serveurs
        STAGING_SERVER = 'votre-staging-server-ip'
        PRODUCTION_SERVER = 'votre-production-server-ip'
    }

    stages {
        // Étape 1: Tests et Build
        stage('Tests et Build') {
            steps {
                echo '🧪 Exécution des tests et construction...'
                sh 'mvn clean package -Dmaven.test.failure.ignore=true'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    // Archive les artefacts avec un pattern plus flexible
                    archiveArtifacts artifacts: 'target/*.jar, target/*.war', allowEmptyArchive: true
                }
            }
        }

        // Étape 2: Construction Image Docker
        stage('Build Image Docker') {
            steps {
                echo '🐳 Construction de l\'image Docker...'
                script {
                    // Trouve le fichier JAR/WAR généré
                    def jarFile = findFiles(glob: 'target/*.jar')[0]?.name
                    if (!jarFile) {
                        jarFile = findFiles(glob: 'target/*.war')[0]?.name
                    }
                    
                    if (jarFile) {
                        env.APP_JAR = jarFile
                        echo "Fichier d'application trouvé: ${jarFile}"
                    } else {
                        error "Aucun fichier JAR/WAR trouvé dans target/"
                    }
                    
                    // Récupère la version du projet
                    def version = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()
                    env.APP_VERSION = version
                }
                sh '''
                    docker build -t $DOCKERHUB_CREDENTIALS_USR/spring-app:${APP_VERSION} .
                    docker tag $DOCKERHUB_CREDENTIALS_USR/spring-app:${APP_VERSION} $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                '''
            }
        }

        // Étape 3: Push vers DockerHub
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

        // Étape 4: Déploiement Staging (optionnel)
        stage('Déploiement Staging') {
            when {
                branch 'main'
            }
            steps {
                echo '🚀 Déploiement en environnement staging...'
                script {
                    try {
                        sshagent(['ssh-credentials']) {
                            sh """
                                ssh -o StrictHostKeyChecking=no -o ConnectTimeout=30 ubuntu@${env.STAGING_SERVER} "
                                    docker pull $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                                    docker stop spring-app-staging || true
                                    docker rm spring-app-staging || true
                                    docker run -d --name spring-app-staging -p 8080:8080 \
                                        $DOCKERHUB_CREDENTIALS_USR/spring-app:latest
                                "
                            """
                        }
                    } catch (Exception e) {
                        echo "⚠️ Déploiement staging échoué mais continuation: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Statut du build: ${currentBuild.currentResult}"
            echo "Version construite: ${env.APP_VERSION ?: 'N/A'}"
        }

        success {
            echo '🎉 Pipeline exécutée avec succès!'
        }

        failure {
            echo '❌ Pipeline échouée! Vérifiez les logs pour plus de détails.'
        }
    }
}