pipeline {
    agent {
        docker {
            image 'docker:latest'
            args '-v /var/run/docker.sock:/var/run/docker.sock --user root'
        }
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup Python Environment') {
            steps {
                echo '🐍 Setting up Python virtual environment...'
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
                echo '📦 Installing Python dependencies...'
                sh '''
                    . /opt/venv/bin/activate
                    pip install -r requirements.txt
                '''
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                sh 'docker build -t gseha/python-app:latest .'
            }
        }
        
        stage('Deploy to Staging') {
            steps {
                echo '🚀 Deploying to staging...'
                sh '''
                    docker stop python-app || true
                    docker rm python-app || true
                    docker run -d --name python-app -p 5000:5000 gseha/python-app:latest
                '''
            }
        }
    }
    
    post {
        always {
            echo "Build status: ${currentBuild.currentResult}"
        }
        success {
            echo '🎉 Build successful! Application is running on http://localhost:5000'
        }
        failure {
            echo '❌ Build failed! Check the logs for details.'
        }
    }
}