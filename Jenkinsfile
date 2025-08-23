pipeline {
    agent any  // Changé de 'docker' à 'any' car Docker n'est pas installé
    
    environment {
        // Configuration de base
        DOCKER_IMAGE = 'votreusername/springboot-app'
        DOCKER_TAG = "${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}"
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
                    env.BRANCH_NAME == 'main' 
                }
            }
            steps {
                script {
                    echo '🔍 Analyse statique avec SonarCloud...'
                    // SonarCloud sera ajouté plus tard
                    echo 'SonarCloud configuré mais non exécuté (besoin de credentials)'
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
                    
                    // Docker désactivé temporairement
                    echo '🐳 Docker désactivé (non installé sur Jenkins)'
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo '🧹 Nettoyage des ressources...'
                // Pas de docker logout car Docker n'est pas installé
            }
        }
        
        success {
            script {
                echo '✅ Pipeline exécutée avec succès!'
                // Slack désactivé temporairement
                echo '📧 Notification Slack désactivée (configuration manquante)'
            }
        }
        
        failure {
            script {
                echo '❌ Pipeline en échec!'
                // Slack désactivé temporairement
                echo '📧 Notification Slack désactivée (configuration manquante)'
            }
        }
    }
}