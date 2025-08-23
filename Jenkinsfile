pipeline {
    agent any
    tools {
        maven 'M3'
        jdk 'jdk17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    echo '🚀 Clonage du dépôt...'
                    git branch: 'main', 
                         url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
                }
            }
        }
        
        stage('Build') {
            steps {
                script {
                    echo '🔨 Construction du projet...'
                    sh 'mvn clean compile'
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    echo '🧪 Exécution des tests...'
                    sh 'mvn test'
                }
            }
        }
        
        stage('Package') {
            steps {
                script {
                    echo '📦 Création du package...'
                    sh 'mvn package -DskipTests'
                }
            }
        }
    }
    
    post {
        always {
            echo '🧹 Nettoyage...'
        }
        failure {
            echo '❌ Échec du pipeline!'
        }
        success {
            echo '✅ Pipeline réussi!'
        }
    }
}