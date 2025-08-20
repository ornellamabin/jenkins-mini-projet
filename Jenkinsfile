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
                    find . -name "pom.xml" -type f
                    ls -la */
                '''
            }
        }
        
        // Étape 2: Se déplacer dans le bon dossier
        stage('Changer de répertoire') {
            steps {
                dir('springbootapp') {  // ← CHANGEMENT ICI
                    sh 'pwd && ls -la'
                }
            }
        }
        
        // Étape 3: Compilation (DANS LE BON DOSSIER)
        stage('Compilation') {
            steps {
                dir('springbootapp') {  // ← CHANGEMENT ICI
                    sh 'mvn clean compile'
                }
            }
        }
        
        // Étape 4: Tests Unitaires (DANS LE BON DOSSIER)
        stage('Tests Unitaires') {
            steps {
                dir('springbootapp') {  // ← CHANGEMENT ICI
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit 'springbootapp/target/surefire-reports/*.xml'  // ← CHEMODIFIÉ
                }
            }
        }
        
        // Étape 5: Analyse SonarCloud (DANS LE BON DOSSIER)
        stage('Analyse SonarCloud') {
            steps {
                dir('springbootapp') {  // ← CHANGEMENT ICI
                    withSonarQubeEnv('sonarcloud') {
                        sh 'mvn sonar:sonar -Dsonar.projectKey=springboot-app -Dsonar.organization=ornellamabin -Dsonar.login=$SONAR_TOKEN -Dspring-boot.repackage.skip=true'
                    }
                }
            }
        }
        
        // Étape 6: Packaging (DANS LE BON DOSSIER)
        stage('Packaging') {
            steps {
                dir('springbootapp') {  // ← CHANGEMENT ICI
                    sh 'mvn package -DskipTests'
                    archiveArtifacts 'target/*.jar'
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