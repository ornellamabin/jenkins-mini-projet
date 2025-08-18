pipeline {
    agent {
        docker {
            image 'maven:3.9.2-eclipse-temurin-17' // Maven + Java 17
            args '-v $HOME/.m2:/root/.m2' // Pour cacher les dépendances Maven entre builds
        }
    }

    environment {
        PROJECT_DIR = '/app'
    }

    stages {
        stage('Checkout') {
            steps {
                // Récupération du code source
                checkout scm
            }
        }

        stage('Build') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Test') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo "Pipeline terminée avec succès pour la branche ${env.BRANCH_NAME}"
        }
        failure {
            echo "La pipeline a échoué pour la branche ${env.BRANCH_NAME}"
        }
    }
}
