
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

        stage('Build and Unit Tests') {
            steps {
                sh '''
                    set -e
                    CONTAINER="maven-build-${BUILD_NUMBER}"

                    docker create \
                      --name "$CONTAINER" \
                      --workdir /workspace/pay \
                      maven:3.9.9-eclipse-temurin-17 \
                      mvn -B clean verify

                    docker cp "$WORKSPACE/." "$CONTAINER:/workspace"

                    set +e
                    docker start -a "$CONTAINER"
                    STATUS=$?
                    set -e

                    docker cp "$CONTAINER:/workspace/pay/target" \
                      "$WORKSPACE/pay/" || true

                    docker rm "$CONTAINER" || true
                    exit "$STATUS"
                '''
            }
        }

        stage('Publish Unit Test Results') {
            steps {
                junit testResults: 'pay/target/surefire-reports/*.xml',
                      allowEmptyResults: true
            }
        }

        stage('SonarQube Cloud Analysis') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'sonar-token',
                        variable: 'SONAR_TOKEN'
                    )
                ]) {
                    sh '''
                        set -e
                        CONTAINER="sonar-build-${BUILD_NUMBER}"

                        docker create \
                          --name "$CONTAINER" \
                          --workdir /workspace/pay \
                          -e SONAR_TOKEN \
                          maven:3.9.9-eclipse-temurin-17 \
                          mvn -B \
                          org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                          -Dsonar.host.url=https://sonarcloud.io \
                          -Dsonar.organization=snittoor20-oss \
                          -Dsonar.projectKey=snittoor20-oss_finsecure-pay

                        docker cp "$WORKSPACE/." "$CONTAINER:/workspace"

                        set +e
                        docker start -a "$CONTAINER"
                        STATUS=$?
                        set -e

                        docker rm "$CONTAINER" || true
                        exit "$STATUS"
                    '''
                }
            }
        }

        stage('Secret Scan - Source Code') {
    steps {
        sh '''
            mkdir -p reports

            trivy repo \
              --scanners secret \
              --severity HIGH,CRITICAL \
              --skip-dirs .git \
              --format json \
              --exit-code 0 \
              --output reports/source-secret-report.json \
              .

            trivy repo \
              --scanners secret \
              --severity HIGH,CRITICAL \
              --skip-dirs .git \
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

        stage('Push Image to Docker Hub') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sh '''
                        set +x

                        echo "$DOCKER_PASSWORD" | docker login \
                          -u "$DOCKER_USER" \
                          --password-stdin

                        docker push ${IMAGE_NAME}:${IMAGE_TAG}

                        docker logout || true
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'reports/*.json',
                             allowEmptyArchive: true,
                             fingerprint: true

            echo 'Security scan evidence archived.'
        }

        success {
            echo 'FinSecure Pay pipeline completed successfully.'
        }

        failure {
            echo 'Pipeline failed. Review Jenkins console output and reports.'
        }
    }
}
