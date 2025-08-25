pipeline {
    agent {
        docker {
            image 'maven:3.8-openjdk-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }
    
    environment {
        DOCKER_IMAGE = 'gseha/springboot-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Unit Tests') {
            steps {
                echo '🧪 Running unit tests...'
                sh 'mvn test'
            }
        }
        
        stage('Code Quality - SonarCloud') {
            steps {
                echo '🔍 Analyzing code quality...'
                withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=ornellamabin-springboot-app \
                          -Dsonar.organization=ornellamabin \
                          -Dsonar.host.url=https://sonarcloud.io \
                          -Dsonar.login=$SONAR_TOKEN
                    '''
                }
            }
        }
        
        stage('Build and Package') {
            steps {
                echo '🏗️ Building application...'
                sh '''
                    mvn clean package
                    docker build -t ${DOCKER_IMAGE}:latest .
                    docker tag ${DOCKER_IMAGE}:latest ${DOCKER_IMAGE}:${DOCKER_TAG}
                '''
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                echo '📤 Pushing to Docker Hub...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh '''
                        docker login -u $DOCKER_USER -p $DOCKER_PASSWORD
                        docker push ${DOCKER_IMAGE}:latest
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    '''
                }
            }
        }
        
        stage('Deploy to Staging') {
            steps {
                echo '🚀 Deploying to staging...'
                sshagent(['ssh-staging-credentials']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no user@staging-server "
                            docker pull ${DOCKER_IMAGE}:latest
                            docker stop staging-app || true
                            docker rm staging-app || true
                            docker run -d --name staging-app -p 3000:8080 ${DOCKER_IMAGE}:latest
                        "
                    """
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                echo '🎯 Deploying to production...'
                sshagent(['ssh-production-credentials']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no user@production-server "
                            docker pull ${DOCKER_IMAGE}:latest
                            docker stop production-app || true
                            docker rm production-app || true
                            docker run -d --name production-app -p 80:8080 ${DOCKER_IMAGE}:latest
                        "
                    """
                }
            }
        }
        
        stage('Validation Tests') {
            steps {
                echo '✅ Running validation tests...'
                sh '''
                    # Tests de validation après déploiement
                    curl -f http://staging-server:3000/health || echo "Staging health check failed"
                    if [ "${env.BRANCH_NAME}" = "main" ]; then
                        curl -f http://production-server/health || echo "Production health check failed"
                    fi
                '''
            }
        }
    }
    
    post {
        always {
            slackSend(
                channel: '#jenkins-notifications',
                message: "Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.currentResult}",
                color: currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger'
            )
        }
    }
}