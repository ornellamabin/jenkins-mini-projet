pipeline {
    agent any
    tools {
        maven 'M3'
        jdk 'jdk17' // ou 'jdk11' selon votre projet
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    echo '🚀 Clonage du dépôt...'
                    git branch: 'main', 
                         url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
                    // Retirez credentialsId temporairement pour tester
                }
            }
        }
        
        stage('Debug Structure') {
            steps {
                script {
                    echo '🔍 Analyse de la structure...'
                    sh '''
                        echo "Répertoire: $(pwd)"
                        echo "Fichiers:"
                        ls -la
                        echo "Recherche pom.xml:"
                        find . -name "pom.xml" 2>/dev/null || echo "Aucun pom.xml trouvé"
                    '''
                }
            }
        }
        
        stage('Build') {
            steps {
                script {
                    echo '🔨 Construction du projet...'
                    // Essayez d'abord à la racine
                    sh 'mvn clean compile || echo "Échec build racine - recherche alternative..."'
                    
                    // Si échec, cherchez le projet
                    sh '''
                        # Cherche le dossier avec pom.xml
                        PROJECT_DIR=$(find . -name "pom.xml" -printf '%h\n' | head -1)
                        if [ -n "$PROJECT_DIR" ]; then
                            echo "Projet trouvé dans: $PROJECT_DIR"
                            cd "$PROJECT_DIR"
                            mvn clean compile
                        else
                            echo "❌ Aucun projet Maven trouvé!"
                            exit 1
                        fi
                    '''
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
            echo '✅ Succès!'
        }
    }
}