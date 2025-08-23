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
                    sh 'mvn clean compile'  // À LA RACINE
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    echo '🧪 Exécution des tests...'
                    sh 'mvn test'  // À LA RACINE
                }
            }
        }
        
        stage('Package') {
            steps {
                script {
                    echo '📦 Création du JAR...'
                    sh 'mvn package -DskipTests'  // À LA RACINE
                }
            }
        }
    }
}