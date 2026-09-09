pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'DOCKER_BUILDKIT=0 docker build -t movie-booking-backend .'
            }
        }

        stage('Deploy') {
            steps {
                sh 'DOCKER_BUILDKIT=0 docker compose up -d --build'
            }
        }
    }
}
