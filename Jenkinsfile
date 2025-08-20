pipeline {
    agent any
    tools {
        maven 'M3'
    }
    
    environment {
        SONAR_TOKEN = credentials('sonarcloud-token')
    }
    
    stages {
        stage('Checkout et Vérification') {
            steps {
                git branch: 'main', 
                url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
                
                sh '''
                    echo "=== STRUCTURE ==="
                    pwd
                    ls -la
                    find . -name pom.xml
                '''
            }
        }
        
        stage('Compilation') {
            steps {
                sh '''
                    cd springbootapp
                    mvn clean compile
                '''
            }
        }
        
        stage('Tests Unitaires') {
            steps {
                sh '''
                    cd springbootapp
                    mvn test
                '''
            }
            post {
                always {
                    junit 'springbootapp/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Analyse SonarCloud') {
            steps {
                withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        cd springbootapp
                        mvn sonar:sonar \
                            -Dsonar.projectKey=jenkins-mini-projet \
                            -Dsonar.organization=ornellamabin \
                            -Dsonar.login=$SONAR_TOKEN \
                            -Dsonar.host.url=https://sonarcloud.io
                    '''
                }
            }
        }
        
        stage('Packaging') {
            steps {
                sh '''
                    cd springbootapp
                    mvn package -DskipTests
                '''
                archiveArtifacts 'springbootapp/target/*.jar'
            }
        }
        
        stage('Docker Info') {
            steps {
                sh '''
                    echo "=== INFORMATION DOCKER ==="
                    echo "Pour activer Docker, configurez les credentials dans Jenkins:"
                    echo "1. dockerhub-username (votre username DockerHub)"
                    echo "2. dockerhub-password (votre token DockerHub)"
                    docker --version
                '''
            }
        }
    }
    
    post {
        always {
            echo "Build ${currentBuild.currentResult}"
            cleanWs()
        }
    }
}