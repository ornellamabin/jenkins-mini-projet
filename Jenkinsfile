pipeline {
    agent any
    
    tools {
        maven 'M3'      // Doit correspondre au nom dans Jenkins
        jdk 'jdk17'     // Doit correspondre au nom dans Jenkins
    }
    
    stages {
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
        
        stage('Qualité de Code') {
            when {
                expression { 
                    env.BRANCH_NAME == 'main' 
                }
            }
            steps {
                script {
                    echo '🔍 Analyse statique avec SonarCloud...'
                    echo 'SonarCloud configuré mais non exécuté (besoin de credentials)'
                }
            }
        }
        
        stage('Compilation et Packaging') {
            steps {
                script {
                    echo '📦 Compilation et création du JAR...'
                    sh 'mvn clean package -DskipTests'
                    echo '✅ JAR créé avec succès!'
                }
            }
        }
    }
    
    post {
        always {
            echo '🧹 Nettoyage des ressources...'
        }
        
        success {
            echo '✅ Pipeline exécutée avec succès!'
        }
        
        failure {
            echo '❌ Pipeline en échec!'
        }
    }
}