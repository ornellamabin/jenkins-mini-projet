pipeline {
    agent {
        docker {
            image 'docker:20.10.23-dind'
            args '--privileged --network host -v /root/.m2:/root/.m2'
        }
    }
    environment {
        // 🔹 Configuration Maven
        MAVEN_HOME = '/usr/share/maven'
        PATH = "${MAVEN_HOME}/bin:${PATH}"
        
        // 🔹 Variables Docker
        DOCKER_IMAGE = "gseha/springboot-app:${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
        DOCKER_REGISTRY_CREDENTIALS = 'dockerhub-credentials'

        // 🔹 Variables Déploiement (Vos IPs)
        STAGING_SERVER_IP = '3.27.150.136'
        PRODUCTION_SERVER_IP = '3.27.150.136'
        STAGING_SSH_CREDENTIALS = 'ec2-production-key'
        PRODUCTION_SSH_CREDENTIALS = 'ec2-production-key'

        SONAR_SCANNER_HOME = tool 'SonarQubeScanner'
    }
    stages {
        // Étape 0: Installation de Maven
        stage('Setup Maven') {
            steps {
                sh 'apk add --no-cache maven'
            }
        }

        // Étape 1: Récupération du code
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        // Étape 2: Compilation et Tests
        stage('Build & Test') {
            steps {
                sh 'mvn clean compile test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        // Étape 3: Analyse qualité code avec SonarQube
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar-cloud') {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=springboot-app'
                }
            }
        }

        // Étape 4: Packaging de l'application
        stage('Package Application') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            }
        }

        // Étape 5: Construction image Docker
        stage('Build Docker Image') {
            steps {
                script {
                    dockerImage = docker.build("${DOCKER_IMAGE}")
                }
            }
        }

        // Étape 6: Push vers DockerHub
        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKER_REGISTRY_CREDENTIALS}") {
                        dockerImage.push()
                    }
                }
            }
        }

        // ===== DÉPLOIEMENTS CONDITIONNELS =====
        // Étape 7a: Déploiement automatique Staging (Branche DEVELOP)
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    echo "🚀 Déploiement sur Staging (${STAGING_SERVER_IP})..."
                    sshagent(["${STAGING_SSH_CREDENTIALS}"]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${STAGING_SERVER_IP} '
                                docker pull ${DOCKER_IMAGE}
                                docker stop springboot-app-staging || true
                                docker rm springboot-app-staging || true
                                docker run -d -p 8080:8080 --name springboot-app-staging ${DOCKER_IMAGE}
                            '
                        """
                    }
                }
            }
        }

        // Étape 7b: Déploiement manuel Production (Branche MAIN)
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    input message: '✅ DÉPLOYER EN PRODUCTION ?', ok: 'Déployer'
                    echo "🚀 Déploiement sur Production (${PRODUCTION_SERVER_IP})..."

                    sshagent(["${PRODUCTION_SSH_CREDENTIALS}"]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${PRODUCTION_SERVER_IP} '
                                docker pull ${DOCKER_IMAGE}
                                docker stop springboot-app-prod || true
                                docker rm springboot-app-prod || true
                                docker run -d -p 80:8080 --name springboot-app-prod ${DOCKER_IMAGE}
                            '
                        """
                    }
                }
            }
        }

        // Étape 8: Tests de validation après déploiement
        stage('Smoke Test') {
            when {
                anyOf {
                    branch 'main';
                    branch 'develop'
                }
            }
            steps {
                script {
                    def testUrl = (env.BRANCH_NAME == 'main') ? 
                        "http://${PRODUCTION_SERVER_IP}/actuator/health" : 
                        "http://${STAGING_SERVER_IP}:8080/actuator/health"

                    sh """
                        echo "🧪 Test de santé de l'application: ${testUrl}"
                        curl -s --retry 10 --retry-delay 5 --retry-all-errors -f ${testUrl} || exit 1
                        echo "✅ Application en bonne santé!"
                    """
                }
            }
        }
    }
    post {
        always {
            echo "📦 Pipeline ${currentBuild.result ?: 'SUCCESS'} - ${env.JOB_NAME} #${env.BUILD_NUMBER} (${env.BRANCH_NAME})"
        }
        success {
            slackSend (
                channel: '#jenkins-ci',
                color: 'good',
                message: """✅ SUCCÈS - Pipeline `${env.JOB_NAME}`
                    | Branch: `${env.BRANCH_NAME}` - Build: #${env.BUILD_NUMBER}
                    | Durée: `${currentBuild.durationString.replace(' and counting', '')}`
                    | Lien: ${env.BUILD_URL}console""".stripMargin()
            )
        }
        failure {
            slackSend (
                channel: '#jenkins-ci',
                color: 'danger',
                message: """❌ ÉCHEC - Pipeline `${env.JOB_NAME}`
                    | Branch: `${env.BRANCH_NAME}` - Build: #${env.BUILD_NUMBER}
                    | Lien: ${env.BUILD_URL}console""".stripMargin()
            )
        }
    }
}