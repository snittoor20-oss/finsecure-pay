
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    triggers {
        pollSCM('H/2 * * * *')
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
                      --mount type=volume,source=finsecure-maven-cache,target=/root/.m2 \
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
                          --mount type=volume,source=finsecure-maven-cache,target=/root/.m2 \
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
                    set -e
                    mkdir -p reports

                    trivy fs \
                      --scanners secret \
                      --severity HIGH,CRITICAL \
                      --format json \
                      --exit-code 0 \
                      --output reports/source-secret-report.json \
                      pay/src

                    trivy fs \
                      --scanners secret \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      pay/src
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -e

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
                    set -e
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
                        set -e
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

        stage('Deploy to GKE') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'gcp-service-account',
                        variable: 'GCP_KEY'
                    )
                ]) {
                    sh '''
                        set -eu
                        set +x

                        export CLOUDSDK_CONFIG="$(mktemp -d)"
                        export KUBECONFIG="$CLOUDSDK_CONFIG/kubeconfig"

                        trap 'rm -rf "$CLOUDSDK_CONFIG"' EXIT

                        gcloud auth activate-service-account \
                          --key-file="$GCP_KEY" \
                          --quiet

                        gcloud container clusters get-credentials \
                          finsecure-cluster \
                          --zone asia-southeast1-b \
                          --project finsecure-pay

                        echo "Deploying ${IMAGE_NAME}:${IMAGE_TAG}"

                        kubectl set image \
                          deployment/finsecure-pay \
                          finsecure-pay=${IMAGE_NAME}:${IMAGE_TAG} \
                          --namespace default

                        kubectl rollout status \
                          deployment/finsecure-pay \
                          --namespace default \
                          --timeout=300s

                        kubectl get deployment finsecure-pay \
                          --namespace default
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
