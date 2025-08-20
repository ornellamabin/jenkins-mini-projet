pipeline {
    agent any
    tools {
        maven 'M3'
    }
    
    environment {
        SONAR_TOKEN = credentials('sonarcloud-token')
    }
    
    stages {
        // Étape 1: Checkout du code
        stage('Checkout Code') {
            steps {
                git branch: 'main', 
                url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
                
                // DEBUG: Vérifier la structure
                sh '''
                    echo "=== STRUCTURE DES FICHIERS ==="
                    pwd
                    ls -la
                    echo "=== SOUS-DOSSIERS ==="
                    ls -la */
                    echo "=== RECHERCHE POM.XML ==="
                    find . -name "pom.xml" -type f
                '''
            }
        }
        
        // Étape 2: Compilation (DANS LE BON DOSSIER)
        stage('Compilation') {
            steps {
                dir('springbootapp') {  // ← ESSENTIEL: changer de répertoire
                    sh 'mvn clean compile'
                }
            }
        }
        
        // Étape 3: Tests Unitaires (DANS LE BON DOSSIER)
        stage('Tests Unitaires') {
            steps {
                dir('springbootapp') {  // ← ESSENTIEL: changer de répertoire
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit 'springbootapp/target/surefire-reports/*.xml'
                }
            }
        }
        
        // Étape 4: Analyse SonarCloud (DANS LE BON DOSSIER)
        stage('Analyse SonarCloud') {
            steps {
                dir('springbootapp') {  // ← ESSENTIEL: changer de répertoire
                    withSonarQubeEnv('sonarcloud') {
                        sh 'mvn sonar:sonar -Dsonar.projectKey=springboot-app -Dsonar.organization=ornellamabin -Dsonar.login=$SONAR_TOKEN -Dspring-boot.repackage.skip=true'
                    }
                }
            }
        }
        
        // Étape 5: Packaging (DANS LE BON DOSSIER)
        stage('Packaging') {
            steps {
                dir('springbootapp') {  // ← ESSENTIEL: changer de répertoire
                    sh 'mvn package -DskipTests'
                    archiveArtifacts 'springbootapp/target/*.jar'
                }
            }
        }
    }
    
    post {
        always {
            echo "Build ${currentBuild.currentResult} - Voir les détails: ${env.BUILD_URL}"
            cleanWs()
        }
    }
}