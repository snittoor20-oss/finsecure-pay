
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    environment {
        IMAGE_NAME = 'dskn123/finsecure-pay'
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Secret Scan - Source Code') {
            steps {
                sh '''
                    mkdir -p reports

                    trivy fs \
                      --scanners secret \
                      --severity HIGH,CRITICAL \
                      --format json \
                      --exit-code 0 \
                      --output reports/source-secret-report.json \
                      .

                    trivy fs \
                      --scanners secret \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      .
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker build \
                      -t ${IMAGE_NAME}:${IMAGE_TAG} \
                      -f pay/Dockerfile \
                      pay
                '''
            }
        }

        stage('Vulnerability Scan - Trivy') {
            steps {
                sh '''
                    mkdir -p reports

                    trivy image \
                      --scanners vuln \
                      --severity HIGH,CRITICAL \
                      --format json \
                      --exit-code 0 \
                      --output reports/image-vulnerability-report.json \
                      ${IMAGE_NAME}:${IMAGE_TAG}

                    trivy image \
                      --scanners vuln \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      ${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'reports/*.json',
                             allowEmptyArchive: true,
                             fingerprint: true

            echo 'Trivy security scan evidence archived.'
        }

        success {
            echo 'FinSecure Pay pipeline completed successfully.'
        }

        failure {
            echo 'Pipeline failed. Review the Jenkins console output and Trivy reports.'
        }
    }
}
