pipeline {
    agent any
    tools {
        maven 'M3'
    }
    
    environment {
        SONAR_TOKEN = credentials('sonarcloud-token')
        DOCKER_USERNAME = credentials('dockerhub-username')
        DOCKER_PASSWORD = credentials('dockerhub-password')
        DOCKER_IMAGE = 'ornellamabin/springboot-app'  // ← Votre username DockerHub
        
        // Variables pour les serveurs (à configurer dans Jenkins)
        STAGING_SERVER = credentials('staging-server')
        PRODUCTION_SERVER = credentials('production-server')
        SLACK_WEBHOOK = credentials('slack-webhook')
    }
    
    stages {
        // ÉTAPE 1: CHECKOUT
        stage('Checkout et Vérification') {
            steps {
                git branch: 'main', 
                url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
                
                sh '''
                    echo "=== STRUCTURE DU PROJET ==="
                    pwd
                    ls -la
                    find . -name pom.xml
                    echo "=== VÉRIFICATION DOCKER ==="
                    docker --version
                '''
            }
        }
        
        // ÉTAPE 2: COMPILATION
        stage('Compilation') {
            steps {
                sh '''
                    cd springbootapp
                    mvn clean compile
                '''
            }
        }
        
        // ÉTAPE 3: TESTS AUTOMATISÉS
        stage('Tests Unitaires') {
            steps {
                sh '''
                    cd springbootapp
                    mvn test
                '''
            }
            post {
                always {
                    junit 'springbootapp/target/surefire-reports/*.xml'
                    archiveArtifacts 'springbootapp/target/surefire-reports/*.xml'
                }
            }
        }
        
        // ÉTAPE 4: QUALITÉ DE CODE
        stage('Analyse SonarCloud') {
            steps {
                withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        cd springbootapp
                        mvn sonar:sonar \
                            -Dsonar.projectKey=jenkins-mini-projet \
                            -Dsonar.organization=ornellamabin \
                            -Dsonar.login=$SONAR_TOKEN \
                            -Dsonar.host.url=https://sonarcloud.io \
                            -Dsonar.sourceEncoding=UTF-8
                    '''
                }
            }
        }
        
        // ÉTAPE 5: PACKAGING
        stage('Packaging') {
            steps {
                sh '''
                    cd springbootapp
                    mvn package -DskipTests
                    echo "=== JAR GÉNÉRÉ ==="
                    ls -la target/*.jar
                '''
                archiveArtifacts 'springbootapp/target/*.jar'
            }
        }
        
        // ÉTAPE 6: BUILD DOCKER
        stage('Build et Push Docker') {
            steps {
                sh '''
                    echo "=== CONSTRUCTION DE L'IMAGE DOCKER ==="
                    cd springbootapp
                    
                    # Build de l'image Docker
                    docker build -t $DOCKER_IMAGE:$BUILD_NUMBER .
                    docker build -t $DOCKER_IMAGE:latest .
                    
                    echo "=== CONNEXION À DOCKERHUB ==="
                    echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin
                    
                    echo "=== ENVOI DE L'IMAGE ==="
                    docker push $DOCKER_IMAGE:$BUILD_NUMBER
                    docker push $DOCKER_IMAGE:latest
                    
                    echo "=== NETTOYAGE LOCAL ==="
                    docker logout
                    echo "✅ Image Docker poussée avec succès: $DOCKER_IMAGE:$BUILD_NUMBER"
                '''
            }
        }
        
        // ÉTAPE 7: DÉPLOIEMENT STAGING
        stage('Déploiement Staging') {
            steps {
                sshagent(['ssh-staging-key']) {
                    sh '''
                        echo "=== DÉPLOIEMENT STAGING ==="
                        ssh -o StrictHostKeyChecking=no $STAGING_SERVER "
                            echo '🐳 Mise à jour de l application sur le serveur staging...'
                            docker pull $DOCKER_IMAGE:latest
                            docker stop springboot-app || true
                            docker rm springboot-app || true
                            docker run -d -p 8080:8080 --name springboot-app $DOCKER_IMAGE:latest
                            echo '✅ Application déployée sur staging:8080'
                        "
                    '''
                }
            }
        }
        
        // ÉTAPE 8: TESTS STAGING
        stage('Tests Validation Staging') {
            steps {
                sh '''
                    echo "=== TESTS DE VALIDATION STAGING ==="
                    sleep 10  # Attente du démarrage
                    curl -f http://$STAGING_SERVER:8080/api/hello || exit 1
                    echo "✅ Application staging fonctionnelle"
                '''
            }
        }
        
        // ÉTAPE 9: DÉPLOIEMENT PRODUCTION (seulement sur main)
        stage('Déploiement Production') {
            when {
                branch 'main'
            }
            steps {
                sshagent(['ssh-production-key']) {
                    sh '''
                        echo "=== DÉPLOIEMENT PRODUCTION ==="
                        ssh -o StrictHostKeyChecking=no $PRODUCTION_SERVER "
                            echo '🚀 Mise en production...'
                            docker pull $DOCKER_IMAGE:latest
                            docker stop springboot-app-prod || true
                            docker rm springboot-app-prod || true
                            docker run -d -p 80:8080 --name springboot-app-prod $DOCKER_IMAGE:latest
                            echo '✅ Application en production!'
                        "
                    '''
                }
            }
        }
        
        // ÉTAPE 10: TESTS PRODUCTION
        stage('Tests Validation Production') {
            when {
                branch 'main'
            }
            steps {
                sh '''
                    echo "=== TESTS DE VALIDATION PRODUCTION ==="
                    sleep 15  # Attente du démarrage
                    curl -f http://$PRODUCTION_SERVER/api/hello || exit 1
                    echo "✅ Application production fonctionnelle"
                '''
            }
        }
    }
    
    // ÉTAPE FINALE: NOTIFICATIONS
    post {
        success {
            echo "✅ BUILD RÉUSSI - Pipeline complète exécutée avec succès"
            slackSend(
                channel: '#jenkins-notifications',
                message: "✅ SUCCÈS: Pipeline ${JOB_NAME} #${BUILD_NUMBER}\n• Branch: ${BRANCH_NAME}\n• Détails: ${BUILD_URL}\n• Application déployée sur staging et production"
            )
        }
        failure {
            echo "❌ BUILD ÉCHOUÉ - Vérifiez les logs"
            slackSend(
                channel: '#jenkins-notifications',
                message: "❌ ÉCHEC: Pipeline ${JOB_NAME} #${BUILD_NUMBER}\n• Branch: ${BRANCH_NAME}\n• Détails: ${BUILD_URL}\n• Erreur: ${currentBuild.currentResult}"
            )
        }
        always {
            echo "Build ${currentBuild.currentResult} - Nettoyage"
            cleanWs()
        }
    }
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
}