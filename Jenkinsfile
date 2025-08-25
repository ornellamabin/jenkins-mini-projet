pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'gseha/springboot-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Setup') {
            steps {
                echo '🔧 Setting up environment...'
                sh '''
                    echo "Java version:"
                    java -version
                    echo "Docker version:"
                    docker --version
                    echo "Maven Wrapper:"
                    chmod +x mvnw  # Assure que le wrapper est exécutable
                    ./mvnw --version
                '''
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo '🧪 Running unit tests...'
                sh './mvnw test'
            }
        }
        
        stage('Code Quality - SonarCloud') {
            steps {
                echo '🔍 Analyzing code quality...'
                withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        ./mvnw sonar:sonar \
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
                sh './mvnw clean package'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                sh '''
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
                        echo $DOCKER_PASSWORD | docker login -u $DOCKER_USER --password-stdin
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
                script {
                    try {
                        sh 'curl -f http://staging-server:3000/health'
                        echo "✅ Staging health check passed"
                    } catch (Exception e) {
                        echo "❌ Staging health check failed: ${e}"
                    }
                    
                    if (env.BRANCH_NAME == 'main') {
                        try {
                            sh 'curl -f http://production-server/health'
                            echo "✅ Production health check passed"
                        } catch (Exception e) {
                            echo "❌ Production health check failed: ${e}"
                        }
                    }
                }
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
        
        cleanup {
            echo '🧹 Cleaning up...'
            sh 'docker logout || true'
        }
    }
}