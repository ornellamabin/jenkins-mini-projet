pipeline {
    agent any
    tools {
        maven 'M3'  // Assurez-vous que Maven est configuré avec ce nom dans Jenkins
    }
    environment {
        SONAR_TOKEN = credentials('sonar-cloud-token')
    }
    stages {
        stage('Clone Git') {
            steps {
                git branch: 'main',
                url: 'https://github.com/ornellamabin/jenkins-mini-projet.git',
                credentialsId: 'github-credentials',
                depth: 1  // Shallow clone pour plus de rapidité
            }
        }
        
        stage('Compilation') {
            steps {
                sh '''
                    cd springbootapp
                    mvn clean compile -q
                '''
            }
        }
        
        stage('Tests Unitaires') {
            steps {
                sh '''
                    cd springbootapp
                    mvn test -q
                '''
            }
            post {
                always {
                    junit 'springbootapp/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Analyse SonarCloud') {
            when {
                expression { 
                    // Activez ceci après avoir poussé le pom.xml corrigé
                    return false 
                }
            }
            steps {
                sh '''
                    cd springbootapp
                    mvn sonar:sonar \
                      -Dsonar.projectKey=springboot-app \
                      -Dsonar.organization=ornellamabin \
                      -Dsonar.login=$SONAR_TOKEN \
                      -Dsonar.host.url=https://sonarcloud.io \
                      -Dsonar.java.binaries=target/classes
                '''
            }
        }
    }
    
    post {
        success {
            echo '🎉 Pipeline exécuté avec succès!'
            slackSend channel: '#jenkins',
                      message: "SUCCESS: Job ${env.JOB_NAME} - Build ${env.BUILD_NUMBER}"
        }
        failure {
            echo '❌ Pipeline a échoué!'
            slackSend channel: '#jenkins', 
                      message: "FAILED: Job ${env.JOB_NAME} - Build ${env.BUILD_NUMBER}"
        }
    }
}