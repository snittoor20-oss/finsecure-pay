groovy
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    environment {
        IMAGE_NAME = 'dskn123/finsecure-pay'
        APP_DIR = 'pay'
        CONTAINER_NAME = 'finsecure-pay'
        K8S_DEPLOYMENT = 'finsecure-pay'
        K8S_NAMESPACE = 'default'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo 'Source code checked out successfully.'
            }
        }

        stage('Pre-flight Checks') {
            steps {
                sh '''
                    set -e

                    echo "Checking Docker CLI..."
                    docker --version

                    echo "Checking Docker connection..."
                    docker info

                    echo "Checking project files..."
                    test -f pay/pom.xml
                    test -f pay/Dockerfile

                    echo "Checking Trivy..."
                    trivy --version

                    echo "Pre-flight checks completed."
                '''
            }
        }

        stage('Build and Test') {
            steps {
                sh '''
                    set -e

                    docker run --rm \
                      -v "$WORKSPACE:/workspace" \
                      -w /workspace/pay \
                      maven:3.9.9-eclipse-temurin-17 \
                      mvn clean verify
                '''
            }

            post {
                always {
                    junit(
                        testResults: 'pay/target/surefire-reports/*.xml',
                        allowEmptyResults: false
                    )
                }
            }
        }

        stage('SonarCloud Analysis') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'sonar-token',
                        variable: 'SONAR_TOKEN'
                    )
                ]) {
                    sh '''
                        set -e

                        docker run --rm \
                          -v "$WORKSPACE:/workspace" \
                          -w /workspace/pay \
                          -e SONAR_TOKEN \
                          maven:3.9.9-eclipse-temurin-17 \
                          mvn sonar:sonar \
                          -Dsonar.token="$SONAR_TOKEN"
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -e

                    docker build \
                      -t "$IMAGE_NAME:$BUILD_NUMBER" \
                      -t "$IMAGE_NAME:latest" \
                      ./pay

                    echo "Docker image built:"
                    docker images "$IMAGE_NAME"
                '''
            }
        }

        stage('Trivy Security Scan') {
            steps {
                sh '''
                    set -e

                    echo "Scanning Docker image for HIGH and CRITICAL vulnerabilities..."

                    trivy image \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      --ignore-unfixed \
                      "$IMAGE_NAME:$BUILD_NUMBER"
                '''
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USER',
                        passwordVariable: 'DOCKERHUB_TOKEN'
                    )
                ]) {
                    sh '''
                        set -e

                        echo "$DOCKERHUB_TOKEN" | docker login \
                          --username "$DOCKERHUB_USER" \
                          --password-stdin

                        docker push "$IMAGE_NAME:$BUILD_NUMBER"
                        docker push "$IMAGE_NAME:latest"

                        docker logout
                    '''
                }
            }
        }

        stage('Deploy to GKE') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'gke-kubeconfig',
                        variable: 'KUBECONFIG'
                    )
                ]) {
                    sh '''
                        set -e

                        echo "Checking Kubernetes connection..."
                        kubectl cluster-info

                        echo "Updating deployment image..."
                        kubectl set image \
                          deployment/"$K8S_DEPLOYMENT" \
                          "$CONTAINER_NAME"="$IMAGE_NAME:$BUILD_NUMBER" \
                          -n "$K8S_NAMESPACE"

                        echo "Waiting for rollout..."
                        kubectl rollout status \
                          deployment/"$K8S_DEPLOYMENT" \
                          -n "$K8S_NAMESPACE" \
                          --timeout=300s

                        echo "Deployment details:"
                        kubectl get deployment "$K8S_DEPLOYMENT" \
                          -n "$K8S_NAMESPACE"

                        echo "Running pods:"
                        kubectl get pods \
                          -n "$K8S_NAMESPACE" \
                          -l app=finsecure-pay
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'FinSecure Pay CI/CD pipeline completed successfully.'
        }

        failure {
            echo 'Pipeline failed. Check the Console Output for the failed stage.'
        }

        always {
            echo 'Pipeline execution finished.'
        }
    }
}
