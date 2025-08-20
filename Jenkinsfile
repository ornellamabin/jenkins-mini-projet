pipeline {
    agent any
    
    tools {
        maven 'M3'
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
            }
        }
        
        stage('Build & Tests Unitaires') {
            steps {
                sh 'mvn clean compile test'
            }
            post {
                success {
                    echo "Pas de tests à archiver"
                }
            }
        }
        
        stage('Analyse Qualité avec SonarCloud') {
            steps {
                withSonarQubeEnv('sonarcloud') {
                    sh """
                mvn clean verify sonar:sonar \
                -Dsonar.projectKey=ton-projet-key \
                -Dsonar.organization=ton-organisation \
                -Dsonar.host.url=https://sonarcloud.io \
                -Dsonar.login=${SONAR_AUTH_TOKEN}
            """
                }
            }
        }
    }
}