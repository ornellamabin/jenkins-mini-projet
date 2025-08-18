pipeline {
    agent {
        docker {
            image 'maven:3.9.2-eclipse-temurin-17'
            args '-v /var/jenkins_home/.m2:/root/.m2'
        }
    }

    environment {
        PROJECT_NAME = 'jenkins-mini-projet'
    }

    stages {
        stage('Checkout') {
            steps {
                echo '🔄 Clonage du dépôt Git'
                git branch: 'main', url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
            }
        }

        stage('Build') {
            steps {
                echo '⚙️ Compilation et tests Maven'
                sh 'mvn clean install'
            }
        }

        stage('Run Application') {
            steps {
                echo '🚀 Lancement du projet Spring Boot'
                sh 'mvn spring-boot:run &'
            }
        }

        stage('Dockerize') {
            steps {
                echo '🐳 Construction de l’image Docker de l’application'
                sh """
                   docker build -t ${PROJECT_NAME}:latest .
                """
            }
        }

        stage('Cleanup') {
            steps {
                echo '🧹 Nettoyage du workspace'
                sh 'mvn clean'
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline terminé avec succès'
        }
        failure {
            echo '❌ La pipeline a échoué'
        }
        always {
            echo '🔚 Pipeline terminée'
        }
    }
}
