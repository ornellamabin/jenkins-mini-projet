pipeline {
    agent {
        docker {
            image 'docker:latest'
            args '-v /var/run/docker.sock:/var/run/docker.sock --user root'
        }
    }
    
    environment {
        SLACK_CHANNEL = '#ci-cd-notifications'
        DOCKER_REGISTRY = 'your-docker-registry'
    }
    
    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        
        stage('Setup Environment') {
            steps {
                sh '''
                    apk add --no-cache python3 py3-pip
                    python3 -m venv /opt/venv
                    . /opt/venv/bin/activate
                    pip install --upgrade pip
                '''
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh '''
                    . /opt/venv/bin/activate
                    pip install -r requirements.txt
                '''
            }
        }
        
        stage('Unit Tests & Coverage') {
            steps {
                sh '''
                    . /opt/venv/bin/activate
                    pip install pytest pytest-cov
                    python -m pytest tests/ -v --cov=app --cov-report=xml:coverage.xml
                '''
            }
            post { always { junit 'test-reports/*.xml' } }
        }
        
        stage('SonarQube Analysis') {
            environment { SONAR_SCANNER_HOME = tool 'SonarScanner' }
            steps {
                withSonarQubeEnv('SonarQube-Server') {
                    sh '''
                        . /opt/venv/bin/activate
                        $SONAR_SCANNER_HOME/bin/sonar-scanner \
                          -Dsonar.projectKey=python-flask-app \
                          -Dsonar.sources=. \
                          -Dsonar.python.coverage.reportPaths=coverage.xml
                    '''
                }
            }
        }
        
        stage('Quality Gate') {
            steps { waitForQualityGate abortPipeline: true }
        }
        
        stage('Build Docker Image') {
            steps {
                sh 'docker build -t ${DOCKER_REGISTRY}/python-app:${env.BUILD_NUMBER} .'
                sh 'docker tag ${DOCKER_REGISTRY}/python-app:${env.BUILD_NUMBER} ${DOCKER_REGISTRY}/python-app:latest'
            }
        }
        
        stage('Deploy to Staging') {
            steps {
                sh '''
                    docker stop python-app-staging || true
                    docker rm python-app-staging || true
                    docker run -d --name python-app-staging -p 5001:5000 ${DOCKER_REGISTRY}/python-app:latest
                '''
            }
        }
        
        stage('Deploy to Production') {
            when { branch 'main' }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh '''
                            docker login -u $DOCKER_USER -p $DOCKER_PASS
                            docker push ${DOCKER_REGISTRY}/python-app:${env.BUILD_NUMBER}
                            docker push ${DOCKER_REGISTRY}/python-app:latest
                        '''
                    }
                    
                    sshagent(['production-server-key']) {
                        sh """
                            ssh deploy@production-server "
                                docker pull ${DOCKER_REGISTRY}/python-app:latest
                                docker stop python-app || true
                                docker rm python-app || true
                                docker run -d --name python-app -p 80:5000 ${DOCKER_REGISTRY}/python-app:latest
                            "
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            // Notifications Slack + Email
            slackSend(
                channel: env.SLACK_CHANNEL,
                message: "${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.currentResult}"
            )
            
            emailext(
                subject: "Build ${currentBuild.currentResult}: ${env.JOB_NAME}",
                body: "Details: ${env.BUILD_URL}",
                to: 'dev-team@company.com'
            )
        }
    }
}