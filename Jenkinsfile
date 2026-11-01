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

        stage('Secret Scan - Trivy') {
            steps {
                sh '''
                    set -e
                    trivy image \
                      --scanners secret \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      dskn123/finsecure-pay:$BUILD_NUMBER
                '''
            }
        }
    }

stage('Vulnerability Scan - Trivy') {
    steps {
        sh '''
            mkdir -p reports
            set +e

            trivy image \
              --scanners vuln \
              --severity HIGH,CRITICAL \
              --format json \
              --output reports/image-vulnerability-report.json \
              ${IMAGE_NAME}:${IMAGE_TAG}

            SCAN_STATUS=$?

            trivy image \
              --scanners vuln \
              --severity HIGH,CRITICAL \
              ${IMAGE_NAME}:${IMAGE_TAG}

            set -e
            exit $SCAN_STATUS
        '''
    }
}

stage('Secret Scan - Source Code') {
    steps {
        sh '''
            mkdir -p reports
            set +e

            trivy fs \
              --scanners secret \
              --severity HIGH,CRITICAL \
              --format json \
              --output reports/source-secret-report.json \
              .

            SCAN_STATUS=$?

            trivy fs \
              --scanners secret \
              --severity HIGH,CRITICAL \
              .

            set -e
            exit $SCAN_STATUS
        '''
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
post {
    always {
        archiveArtifacts artifacts: 'reports/*.json',
                         allowEmptyArchive: true,
                         fingerprint: true

        echo 'Security scan evidence archived.'
    }
}
}
