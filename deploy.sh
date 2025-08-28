#!/bin/bash
echo "🚀 Starting deployment process..."

# Navigate to project directory
cd /home/ec2-user/jenkins-mini-projet

# Pull latest code
echo "📦 Pulling latest code from Git..."
git pull origin main

# Build the application
echo "🔨 Building application with Maven..."
mvn clean package -DskipTests

# Stop existing application if running
echo "🛑 Stopping existing application..."
sudo fuser -k 8081/tcp 2>/dev/null || true
sleep 3

# Start new application
echo "🎯 Starting Spring Boot application..."
nohup java -jar target/springboot-app-1.0.0.jar --server.port=8081 > app.log 2>&1 &

echo "⏳ Waiting for application to start..."
sleep 10

# Health check
echo "🏥 Performing health check..."
if curl -s http://localhost:8081/actuator/health | grep -q "UP"; then
    echo "✅ Deployment successful! Application is running healthy."
    echo "🌐 Application URL: http://3.27.150.136:8081/"
    echo "🔍 API Test URL: http://3.27.150.136:8081/api/test"
else
    echo "❌ Deployment failed! Application health check failed."
    echo "📋 Check logs: tail -f app.log"
    exit 1
fi