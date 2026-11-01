
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

                    CONTAINER="finsecure-maven-$BUILD_NUMBER"

                    docker rm -f "$CONTAINER" 2>/dev/null || true

                    docker create \
                      --name "$CONTAINER" \
                      --workdir /workspace/pay \
                      maven:3.9.9-eclipse-temurin-17 \
                      mvn clean verify

                    docker cp "$WORKSPACE/." "$CONTAINER:/workspace"

                    docker start -a "$CONTAINER"

                    docker cp \
                      "$CONTAINER:/workspace/pay/target" \
                      "$WORKSPACE/pay/"

                    docker rm "$CONTAINER"
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
            echo 'Pipeline failed. Check Console Output.'
        }

        always {
            echo 'Pipeline execution finished.'
        }
    }
}
