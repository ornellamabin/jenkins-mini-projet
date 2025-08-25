pipeline {
    agent {
        docker {
            image 'docker:latest'
            args '-v /var/run/docker.sock:/var/run/docker.sock --user root'
        }
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
        
        stage('Simple Test') {
            steps {
                sh '''
                    . /opt/venv/bin/activate
                    python -c "from app import app; print('✅ App imports successfully')"
                '''
            }
        }
        
        stage('Build Docker Image') {
            steps {
                sh 'docker build -t gseha/python-app:latest .'
            }
        }
        
        stage('Deploy to Staging') {
            steps {
                sh '''
                    docker stop python-app || true
                    docker rm python-app || true
                    docker run -d --name python-app -p 5001:3000 gseha/python-app:latest
                '''
            }
        }
    }
    
    post {
        always {
            echo "Build status: ${currentBuild.currentResult}"
        }
    }
}