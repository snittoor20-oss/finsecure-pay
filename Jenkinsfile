pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo 'FinSecure Pay source code checked out.'
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
                        allowEmptyResults: true
                    )
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -e

                    docker build \
                      -t dskn123/finsecure-pay:$BUILD_NUMBER \
                      ./pay
                '''
            }
        }

        stage('Container Image Scan - Trivy') {
            steps {
                sh '''
                    set -e

                    trivy image \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      dskn123/finsecure-pay:$BUILD_NUMBER
                '''
            }
        }
    }

    post {
        success {
            echo 'FinSecure Pay pipeline completed successfully.'
        }

        failure {
            echo 'Pipeline failed. Check the failed stage in Console Output.'
        }

        always {
            echo 'Pipeline execution finished.'
        }
    }
}