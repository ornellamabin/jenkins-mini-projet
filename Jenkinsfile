pipeline {
    agent any
    tools {
        maven 'M3'
    }
    
    environment {
        SONAR_TOKEN = credentials('sonarcloud-token')
    }
    
    stages {
        // Étape 1: Checkout et vérification
        stage('Checkout et Vérification') {
            steps {
                git branch: 'main', 
                url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
                
                // DEBUG COMPLET
                sh '''
                    echo "=== CONTENU DU WORKSPACE ==="
                    pwd
                    ls -la
                    echo "=== STRUCTURE COMPLÈTE ==="
                    find . -name "pom.xml" -type f
                    echo "=== SOUS-DOSSIERS EXISTANTS ==="
                    ls -la */
                '''
            }
        }
        
        // Étape 2: Compilation DANS springbootapp
        stage('Compilation') {
            steps {
                sh '''
                    echo "=== TENTATIVE DE COMPILATION ==="
                    if [ -d "springbootapp" ] && [ -f "springbootapp/pom.xml" ]; then
                        echo "✅ springbootapp/pom.xml trouvé!"
                        cd springbootapp
                        mvn clean compile
                    else
                        echo "❌ ERREUR: springbootapp/pom.xml introuvable!"
                        echo "Structure actuelle:"
                        ls -la
                        find . -name "*.xml"
                        exit 1
                    fi
                '''
            }
        }
        
        // Étape 3: Tests Unitaires
        stage('Tests Unitaires') {
            steps {
                sh '''
                    cd springbootapp
                    mvn test
                '''
            }
            post {
                always {
                  //  junit '**/surefire-reports/*.xml'
            }
        }
        
        // Étape 4: Analyse SonarCloud
        stage('Analyse SonarCloud') {
            steps {
                sh '''
                    cd springbootapp
                    mvn sonar:sonar -Dsonar.projectKey=springboot-app -Dsonar.organization=ornellamabin -Dsonar.login=$SONAR_TOKEN -Dspring-boot.repackage.skip=true
                '''
            }
        }
        
        // Étape 5: Packaging
        stage('Packaging') {
            steps {
                sh '''
                    cd springbootapp
                    mvn package -DskipTests
                '''
                archiveArtifacts 'springbootapp/target/*.jar'
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