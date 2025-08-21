pipeline {
    agent any
    tools {
        // Remplacez par cette ligne temporairement
        maven 'M3' 
    }
    stages {
        stage('Build') {
            steps {
                // Ou spécifiez le chemin directement
                sh 'mvn clean compile' 
            }
        }
    }
}