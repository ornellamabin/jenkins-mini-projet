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