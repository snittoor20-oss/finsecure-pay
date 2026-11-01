
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

            echo "Creating Maven container..."

            docker create \
              --name "$CONTAINER" \
              --workdir /workspace/pay \
              maven:3.9.9-eclipse-temurin-17 \
              mvn clean verify

            echo "Copying Jenkins workspace into Maven container..."

            docker cp "$WORKSPACE/." "$CONTAINER:/workspace"

            echo "Running Maven build and tests..."

            docker start -a "$CONTAINER"

            echo "Copying Maven build results back..."

            mkdir -p "$WORKSPACE/pay"

            docker cp \
              "$CONTAINER:/workspace/pay/target" \
              "$WORKSPACE/pay/"

            docker rm "$CONTAINER"

            echo "Maven build and tests completed."
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
