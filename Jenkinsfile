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
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker tag movie-booking-backend $DOCKER_USER/movie-booking-backend:v$BUILD_NUMBER
                        docker tag movie-booking-backend $DOCKER_USER/movie-booking-backend:latest
                        docker push $DOCKER_USER/movie-booking-backend:v$BUILD_NUMBER
                        docker push $DOCKER_USER/movie-booking-backend:latest
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                        kubectl apply -f k8s/service.yaml
                        sed "s|:latest|:v$BUILD_NUMBER|" k8s/deployment.yaml | kubectl apply -f -
                        kubectl rollout status deployment/movie-booking-backend --timeout=180s
                    '''
                }
            }
        }
    }
}
