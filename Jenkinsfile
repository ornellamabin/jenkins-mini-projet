pipeline {
    agent any
    tools {
        maven 'M3'
    }
    
    environment {
        SONAR_TOKEN = credentials('sonarcloud-token')
        DOCKER_USERNAME = credentials('dockerhub-username')
        DOCKER_PASSWORD = credentials('dockerhub-password')
        DOCKER_IMAGE = 'ornellamabin/springboot-app'
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
        
        stage('Build et Push Docker') {
            steps {
                sh '''
                    echo "=== CONSTRUCTION DE L'IMAGE DOCKER ==="
                    cd springbootapp
                    
                    # Build de l'image Docker
                    docker build -t $DOCKER_IMAGE:$BUILD_NUMBER .
                    docker build -t $DOCKER_IMAGE:latest .
                    
                    echo "=== CONNEXION À DOCKERHUB ==="
                    echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin
                    
                    echo "=== ENVOI DE L'IMAGE ==="
                    docker push $DOCKER_IMAGE:$BUILD_NUMBER
                    docker push $DOCKER_IMAGE:latest
                    
                    echo "=== NETTOYAGE LOCAL ==="
                    docker logout
                    echo "✅ Image Docker poussée avec succès: $DOCKER_IMAGE:$BUILD_NUMBER"
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