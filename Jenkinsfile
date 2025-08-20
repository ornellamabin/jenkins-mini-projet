pipeline {
    agent any
    tools {
        maven 'M3'
    }
    
    environment {
        SONAR_TOKEN = credentials('sonarcloud-token')
    }
    
    stages {
        // Étape 1: Checkout du code avec vérification
        stage('Checkout Code') {
            steps {
                git branch: 'main', 
                url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
                
                // Debug: vérifier les fichiers
                sh 'ls -la'
                sh 'find . -name "pom.xml"'
            }
        }
        
        // Étape 2: Compilation
        stage('Compilation') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        // Étape 3: Tests Unitaires
        stage('Tests Unitaires') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        // Étape 4: Analyse SonarCloud
        stage('Analyse SonarCloud') {
            steps {
                withSonarQubeEnv('sonarcloud') {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=springboot-app -Dsonar.organization=ornellamabin -Dsonar.login=$SONAR_TOKEN -Dspring-boot.repackage.skip=true'
                }
            }
        }
        
        // Étape 5: Packaging
        stage('Packaging') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts 'target/*.jar'
            }
        }
    }
    
    post {
        always {
            echo "Build ${currentBuild.currentResult} - Voir les détails: ${env.BUILD_URL}"
            cleanWs()
        }
        success {
            echo "🎉 Pipeline réussi! Application compilée et testée."
        }
        failure {
            echo "❌ Pipeline échoué. Vérifiez les logs pour plus de détails."
        }
    }
}