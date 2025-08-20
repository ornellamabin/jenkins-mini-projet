pipeline {
    agent any
    
    tools {
        maven 'M3'  // Utilise Maven installé dans Jenkins (va dans "Gestion Jenkins" -> "Outils globaux" pour configurer Maven)
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
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Analyse Qualité avec SonarCloud') {
            steps {
                withSonarQubeEnv('sonarcloud') {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=ton-project-key -Dsonar.login=$SONAR_TOKEN'
                }
            }
        }
    }
}