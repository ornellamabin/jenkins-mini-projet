pipeline {
    agent any // Utilise n'importe quel agent Jenkins disponible
    
    environment {
        DOCKERHUB_CREDS = credentials('dockerhub-creds')
        SONAR_TOKEN = credentials('sonarcloud-token')
        SLACK_WEBHOOK = credentials('slack-token')
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
            }
        }
        
        stage('Setup Docker') {
            steps {
                script {
                    // Vérifie que Docker est disponible
                    sh 'docker --version'
                }
            }
        }
        
        stage('Build & Tests Unitaires') {
            steps {
                sh 'mvn clean compile test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Analyse Qualité avec SonarCloud') {
            steps {
                withSonarQubeEnv('sonarcloud') {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=ton-project-key -Dsonar.login=$SONAR_TOKEN'
                }
            }
        }
        
        // stage('Build de l\'image Docker') {
        //     steps {
        //         script {
        //             def imageName = "gseha/my-spring-app:${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}"
        //             docker.build(imageName)
        //         }
        //     }
        // }
        
        // stage('Push de l\'image Docker') {
        //     steps {
        //         script {
        //             def imageName = "gseha/my-spring-app:${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}"
        //             docker.withRegistry('', 'dockerhub-creds') {
        //                 docker.image(imageName).push()
        //             }
        //         }
        //     }
        // }
        
        // stage('Déploiement Production') {
        //     when {
        //         branch 'main'
        //     }
        //     steps {
        //         script {
        //             sshagent(['production-server-ssh']) {
        //                 sh "ssh -o StrictHostKeyChecking=no user@ton-serveur-production.com 'cd /opt/my-spring-app && ./deploy.sh'"
        //             }
        //         }
        //     }
        // }
        
        // stage('Test de Validation') {
        //     when {
        //         branch 'main'
        //     }
        //     steps {
        //         script {
        //             retry(3) {
        //                 sleep 10
        //                 sh 'curl -f http://ton-serveur-production.com:8080/actuator/health || exit 1'
        //             }
        //         }
        //     }
        // }
    }
    
    post {
        always {
            slackSend(
                channel: '#ton-channel',
                message: "Build ${currentBuild.result ?: 'SUCCESS'} - Job ${env.JOB_NAME} [${env.BUILD_NUMBER}] (<${env.BUILD_URL}|Open>)"
            )
        }
        failure {
            slackSend(
                channel: '#ton-channel',
                message: "ATTENTION : Échec du build ${env.JOB_NAME} [${env.BUILD_NUMBER}]! (<${env.BUILD_URL}|Voir les logs>)",
                color: 'danger'
            )
        }
    }
}