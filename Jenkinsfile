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
        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                             sh '''
                                 echo "DOCKERPASS"|dockerlogin-u"DOCKER_USER" --password-stdin
                                 docker tag movie-booking-backend DOCKERUSER/movie-booking-backend:vBUILD_NUMBER
                                 docker tag movie-booking-backend $DOCKER_USER/movie-booking-backend:latest
                                 docker push DOCKERUSER/movie-booking-backend:vBUILD_NUMBER
                                 docker push $DOCKER_USER/movie-booking-backend:latest
                             '''
                }
            }
        }



        stage('Deploy') {
            steps {
                sh 'DOCKER_BUILDKIT=0 docker-compose up -d --build'
            }
        }
    }
}
