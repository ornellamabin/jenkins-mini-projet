pipeline {
    agent any

    environment {
        // Définis ici des variables d'environnement si nécessaire
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk' // adapte selon ton agent Jenkins
        PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                // Récupération du code source
                git branch: "${env.BRANCH_NAME}", url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
            }
        }

        stage('Build') {
            steps {
                // Compilation du projet avec Maven
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                // Exécution des tests unitaires
                sh './mvnw test'
            }
            post {
                always {
                    // Publication des rapports de tests (optionnel)
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                // Création du jar exécutable
                sh './mvnw package'
            }
        }

        stage('Archive') {
            steps {
                // Archivage de l'artifact pour Jenkins
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo "Pipeline terminé avec succès pour la branche ${env.BRANCH_NAME}"
        }
        failure {
            echo "La pipeline a échoué pour la branche ${env.BRANCH_NAME}"
        }
    }
}
