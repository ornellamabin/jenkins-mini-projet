pipeline {
    agent {
        docker {
            image 'maven:3.8.6-openjdk-17'
            args '-v /root/.m2:/root/.m2'
        }
    }
    
    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', 
                    url: 'https://github.com/votre-username/jenkins-mini-projet.git'
            }
        }
        
        stage('Build Application') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Run Tests') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('Package JAR') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
    }
    
    post {
        always {
            echo 'Pipeline execution completed'
        }
    }
}