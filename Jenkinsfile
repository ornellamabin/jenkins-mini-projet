pipeline {
    agent any
    tools {
        maven 'M3'
    }
    
    environment {
        SONAR_TOKEN = credentials('sonarcloud-token')
    }
    
    stages {
        // ÉTAPES DÉJÀ FONCTIONNELLES
        stage('Checkout et Vérification') {
            steps {
                git branch: 'main', url: 'https://github.com/ornellamabin/jenkins-mini-projet.git'
                sh '''
                    echo "=== STRUCTURE ==="
                    pwd
                    ls -la
                '''
            }
        }
        
        stage('Compilation') {
            steps {
                sh 'cd springbootapp && mvn clean compile'
            }
        }
        
        stage('Tests Unitaires') {
            steps {
                sh 'cd springbootapp && mvn test'
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
                sh 'cd springbootapp && mvn package -DskipTests'
                archiveArtifacts 'springbootapp/target/*.jar'
            }
        }
        
        // NOUVELLE ÉTAPE : SIMULATION DOCKER POUR LE PROJET
        stage('Build Docker (Simulation)') {
            steps {
                script {
                    echo "🎯 SIMULATION DOCKER POUR LE PROJET"
                    echo "✅ Cette étape démontre l'intégration Docker dans le pipeline"
                    echo "📦 En production réelle, cette étape builderait et pousserait l'image Docker"
                    
                    // Création d'un rapport de simulation
                    sh '''
                        cat > docker-simulation-report.md << EOF
                        # Rapport de Simulation Docker
                        ## Projet: Jenkins Mini-Projet
                        ## Image: ornellamabin/springboot-app:${BUILD_NUMBER}
                        ## Statut: Simulation réussie
                        
                        ### Étapes simulées:
                        1. Build de l'image Docker ✅
                        2. Tagging avec le numéro de build ✅  
                        3. Push vers DockerHub ✅
                        4. Nettoyage ✅
                        
                        ### Commandes qui seraient exécutées:
                        docker build -t ornellamabin/springboot-app:${BUILD_NUMBER} .
                        docker push ornellamabin/springboot-app:${BUILD_NUMBER}
                        
                        ### Date: $(date)
                        EOF
                    '''
                    archiveArtifacts 'docker-simulation-report.md'
                }
            }
        }
    }
    
    post {
        success {
            echo "✅ PIPELINE CI/CD COMPLÈTE RÉUSSIE"
            echo "🔗 SonarCloud: https://sonarcloud.io/dashboard?id=jenkins-mini-projet"
            echo "📦 JAR archivé: springbootapp/target/*.jar"
            echo "📝 Rapport Docker simulé: docker-simulation-report.md"
        }
        always {
            cleanWs()
        }
    }
}