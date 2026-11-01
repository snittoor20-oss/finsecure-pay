pipeline {
    agent any

    environment {
        DOCKER_IMAGE     = "dskn123/finsecure-pay"
        DOCKER_TAG       = "1.${BUILD_NUMBER}"
        DOCKER_LATEST    = "${DOCKER_IMAGE}:latest"
        DOCKER_VERSIONED = "${DOCKER_IMAGE}:${DOCKER_TAG}"
        SONAR_PROJECT    = "finsecure-pay"
        NOTIFICATION_EMAIL = "snittoor@gmail.com"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {

        // ─────────────────────────────────────────
        stage('Checkout') {
        // ─────────────────────────────────────────
            steps {
                echo "Checking out source code..."
                checkout scm
                echo "Branch: ${env.GIT_BRANCH}"
                echo "Commit: ${env.GIT_COMMIT}"
            }
        }

        // ─────────────────────────────────────────
        stage('Build') {
        // ─────────────────────────────────────────
            steps {
                echo "Compiling application..."
                sh 'mvn clean compile -q'
            }
            post {
                failure {
                    echo "Build stage failed!"
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Test') {
        // ─────────────────────────────────────────
            steps {
                echo "Running unit tests..."
                sh 'mvn test'
            }
            post {
                always {
                    // Publish JUnit test results
                    junit(
                        testResults: 'target/surefire-reports/*.xml',
                        allowEmptyResults: false
                    )
                    echo "Test results published."
                }
                failure {
                    echo "Tests FAILED - pipeline will not proceed to deployment."
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Code Quality - SonarCloud') {
        // ─────────────────────────────────────────
            steps {
                echo "Running SonarCloud analysis..."
                withCredentials([
                    string(credentialsId: 'sonar-token',
                           variable: 'SONAR_TOKEN')
                ]) {
                    sh """
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT} \
                          -Dsonar.organization=<your-sonarcloud-org> \
                          -Dsonar.host.url=https://sonarcloud.io \
                          -Dsonar.login=${SONAR_TOKEN} \
                          -Dsonar.qualitygate.wait=true
                    """
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Dependency Security Scan') {
        // ─────────────────────────────────────────
            steps {
                echo "Running OWASP Dependency Check..."
                sh """
                    mvn org.owasp:dependency-check-maven:check \
                      -DfailBuildOnCVSS=7 \
                      -Dformats=HTML,JSON
                """
            }
            post {
                always {
                    // Archive the report regardless of pass/fail
                    archiveArtifacts(
                        artifacts: 'target/dependency-check-report.*',
                        allowEmptyArchive: true
                    )
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Package') {
        // ─────────────────────────────────────────
            steps {
                echo "Packaging application JAR..."
                sh 'mvn package -DskipTests -q'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // ─────────────────────────────────────────
        stage('Container Build') {
        // ─────────────────────────────────────────
            steps {
                echo "Building Docker image: ${DOCKER_VERSIONED}"
                sh "docker build -t ${DOCKER_VERSIONED} -t ${DOCKER_LATEST} ."
                sh "docker images | grep finsecure-pay"
            }
        }

        // ─────────────────────────────────────────
        stage('Container Image Scan - Trivy') {
        // ─────────────────────────────────────────
            steps {
                echo "Scanning Docker image for vulnerabilities..."
                sh """
                    trivy image \
                      --exit-code 1 \
                      --severity CRITICAL \
                      --no-progress \
                      --format table \
                      ${DOCKER_VERSIONED}
                """
            }
            post {
                always {
                    sh """
                        trivy image \
                          --exit-code 0 \
                          --severity HIGH,CRITICAL \
                          --format json \
                          --output trivy-report.json \
                          ${DOCKER_VERSIONED} || true
                    """
                    archiveArtifacts(
                        artifacts: 'trivy-report.json',
                        allowEmptyArchive: true
                    )
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Push to Docker Hub') {
        // ─────────────────────────────────────────
            steps {
                echo "Pushing image to Docker Hub..."
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh """
                        echo "${DOCKER_PASS}" | \
                          docker login -u "${DOCKER_USER}" --password-stdin
                        docker push ${DOCKER_VERSIONED}
                        docker push ${DOCKER_LATEST}
                        echo "Successfully pushed ${DOCKER_VERSIONED}"
                    """
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Deploy to Kubernetes') {
        // ─────────────────────────────────────────
            steps {
                echo "Deploying to Kubernetes cluster..."
                withCredentials([
                    file(credentialsId: 'kubeconfig',
                         variable: 'KUBECONFIG')
                ]) {
                    sh """
                        # Apply latest manifests
                        kubectl apply -f k8s/configmap.yaml \
                          --kubeconfig=${KUBECONFIG}

                        # Update the deployment image
                        kubectl set image deployment/finsecure-pay \
                          finsecure-pay=${DOCKER_VERSIONED} \
                          --kubeconfig=${KUBECONFIG}

                        # Wait for rollout to complete (timeout 3 min)
                        kubectl rollout status deployment/finsecure-pay \
                          --timeout=180s \
                          --kubeconfig=${KUBECONFIG}

                        # Show running pods
                        kubectl get pods -l app=finsecure-pay \
                          --kubeconfig=${KUBECONFIG}
                    """
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Smoke Test') {
        // ─────────────────────────────────────────
            steps {
                echo "Running post-deployment smoke test..."
                withCredentials([
                    file(credentialsId: 'kubeconfig',
                         variable: 'KUBECONFIG')
                ]) {
                    sh """
                        EXTERNAL_IP=\$(kubectl get svc finsecure-pay-service \
                          --kubeconfig=${KUBECONFIG} \
                          -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

                        echo "Testing http://\${EXTERNAL_IP}/health"
                        curl -f http://\${EXTERNAL_IP}/health

                        echo "Smoke test passed!"
                    """
                }
            }
        }

    }

    // ─────────────────────────────────────────
    post {
    // ─────────────────────────────────────────
        success {
            echo "Pipeline completed successfully!"
            emailext(
                to: "${NOTIFICATION_EMAIL}",
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h2>Build Successful</h2>
                    <p><b>Job:</b> ${env.JOB_NAME}</p>
                    <p><b>Build Number:</b> ${env.BUILD_NUMBER}</p>
                    <p><b>Docker Image:</b> ${DOCKER_VERSIONED}</p>
                    <p><b>Duration:</b> ${currentBuild.durationString}</p>
                    <p><a href="${env.BUILD_URL}">View Build</a></p>
                """,
                mimeType: 'text/html'
            )
        }
        failure {
            echo "Pipeline FAILED!"
            emailext(
                to: "${NOTIFICATION_EMAIL}",
                subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h2>Build Failed</h2>
                    <p><b>Job:</b> ${env.JOB_NAME}</p>
                    <p><b>Build Number:</b> ${env.BUILD_NUMBER}</p>
                    <p><b>Failed Stage:</b> ${env.STAGE_NAME}</p>
                    <p><b>Duration:</b> ${currentBuild.durationString}</p>
                    <p><a href="${env.BUILD_URL}console">View Console Log</a></p>
                """,
                mimeType: 'text/html'
            )
        }
        always {
            // Clean up local Docker images to save disk space
            sh "docker rmi ${DOCKER_VERSIONED} || true"
            sh "docker rmi ${DOCKER_LATEST} || true"
            cleanWs()
        }
    }
}