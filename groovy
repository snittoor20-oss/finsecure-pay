pipeline {

    agent any

    // ── Global Environment Variables ───────────────────────────────────────
    environment {
        // Replace with your actual Docker Hub username
        DOCKER_IMAGE       = "dskn123/finsecure-pay"
        DOCKER_TAG         = "${BUILD_NUMBER}"
        SONAR_PROJECT_KEY  = "finsecure-pay"
        SONAR_HOST_URL     = "http://localhost:9000"
    }

    // ── Pipeline Options ───────────────────────────────────────────────────
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    // ══════════════════════════════════════════════════════════════════════
    //  STAGES
    // ══════════════════════════════════════════════════════════════════════
    stages {

        // ──────────────────────────────────────────────────────────────────
        // STAGE 1 — CHECKOUT
        // ──────────────────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                echo '========== STAGE 1: Checkout =========='
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/snittoor20-oss/finsecure-pay.git'
                echo "Commit: ${GIT_COMMIT}"
                echo "Branch: ${GIT_BRANCH}"
            }
        }

        // ──────────────────────────────────────────────────────────────────
        // STAGE 2 — BUILD
        // ──────────────────────────────────────────────────────────────────
        stage('Build') {
            steps {
                echo '========== STAGE 2: Build =========='
                bat 'mvn clean compile -q'
                echo 'Compilation successful'
            }
        }

        // ──────────────────────────────────────────────────────────────────
        // STAGE 3 — TEST
        // ──────────────────────────────────────────────────────────────────
        stage('Test') {
            steps {
                echo '========== STAGE 3: Unit Tests =========='
                bat 'mvn test'
            }
            post {
                always {
                    // Publish JUnit test results in Jenkins UI
                    junit 'target/surefire-reports/*.xml'
                }
                success {
                    echo 'All tests passed — continuing pipeline'
                }
                failure {
                    echo 'Tests FAILED — pipeline blocked, deployment will NOT proceed'
                    emailext(
                        subject: "FAILED: Build #${BUILD_NUMBER} — Unit Tests Failed",
                        body: """
                            <h2>Pipeline Blocked — Unit Test Failure</h2>
                            <p>Build <b>#${BUILD_NUMBER}</b> failed at the Test stage.</p>
                            <p>Deployment has been prevented.</p>
                            <br/>
                            <p><b>Branch:</b> ${GIT_BRANCH}</p>
                            <p><b>Commit:</b> ${GIT_COMMIT}</p>
                            <p><b>Console:</b> <a href="${BUILD_URL}console">${BUILD_URL}console</a></p>
                        """,
                        mimeType: 'text/html',
                        to: 'devops-team@finsecure.com'
                    )
                }
            }
        }

        // ──────────────────────────────────────────────────────────────────
        // STAGE 4 — PACKAGE
        // ──────────────────────────────────────────────────────────────────
        stage('Package') {
            steps {
                echo '========== STAGE 4: Package =========='
                bat 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar',
                                 fingerprint: true
                echo 'JAR packaged and archived'
            }
        }

        // ──────────────────────────────────────────────────────────────────
        // STAGE 5 — SECURITY CHECKS (parallel)
        // ──────────────────────────────────────────────────────────────────
        stage('Security Checks') {
            parallel {

                stage('OWASP Dependency Check') {
                    steps {
                        echo '===== OWASP Dependency Check ====='
                        dependencyCheck(
                            additionalArguments: '''
                                --scan ./
                                --format HTML
                                --format XML
                                --failOnCVSS 7
                            ''',
                            odcInstallation: 'OWASP-DC'
                        )
                        dependencyCheckPublisher(
                            pattern: 'dependency-check-report.xml',
                            failedTotalCritical: 1,
                            failedTotalHigh: 5
                        )
                    }
                }

                stage('SonarQube Analysis') {
                    steps {
                        echo '===== SonarQube Analysis ====='
                        withCredentials([string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )]) {
                            bat """
                                mvn sonar:sonar ^
                                  -Dsonar.projectKey=%SONAR_PROJECT_KEY% ^
                                  -Dsonar.projectName="FinSecure Pay" ^
                                  -Dsonar.host.url=%SONAR_HOST_URL% ^
                                  -Dsonar.token=%SONAR_TOKEN% ^
                                  -Dsonar.java.binaries=target/classes
                            """
                        }
                        echo 'SonarQube analysis complete'
                    }
                }
            }
        }

        // ──────────────────────────────────────────────────────────────────
        // STAGE 6 — CONTAINER BUILD
        // ──────────────────────────────────────────────────────────────────
        stage('Container Build') {
            steps {
                echo '========== STAGE 6: Docker Build =========='
                bat "docker build -t %DOCKER_IMAGE%:%DOCKER_TAG% ."
                bat "docker tag %DOCKER_IMAGE%:%DOCKER_TAG% %DOCKER_IMAGE%:latest"
                echo "Image built: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            }
        }

        // ──────────────────────────────────────────────────────────────────
        // STAGE 7 — TRIVY IMAGE SCAN
        // ──────────────────────────────────────────────────────────────────
        stage('Trivy Scan') {
            steps {
                echo '========== STAGE 7: Trivy Image Scan =========='
                bat """
                    trivy image ^
                      --exit-code 1 ^
                      --severity CRITICAL ^
                      --format table ^
                      --output trivy-report.txt ^
                      %DOCKER_IMAGE%:%DOCKER_TAG%
                """
                archiveArtifacts artifacts: 'trivy-report.txt',
                                 allowEmptyArchive: true
                echo 'Trivy scan passed — no CRITICAL vulnerabilities'
            }
        }

        // ──────────────────────────────────────────────────────────────────
        // STAGE 8 — PUSH TO DOCKER HUB
        // ──────────────────────────────────────────────────────────────────
        stage('Push') {
            steps {
                echo '========== STAGE 8: Push to Docker Hub =========='
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
                    bat "docker push %DOCKER_IMAGE%:%DOCKER_TAG%"
                    bat "docker push %DOCKER_IMAGE%:latest"
                    echo "Pushed ${DOCKER_IMAGE}:${DOCKER_TAG} to Docker Hub"
                }
            }
        }

        // ──────────────────────────────────────────────────────────────────
        // STAGE 9 — DEPLOY TO KUBERNETES (EKS)
        // ──────────────────────────────────────────────────────────────────
        stage('Deploy') {
            steps {
                echo '========== STAGE 9: Deploy to EKS =========='
                withCredentials([
                    file(credentialsId: 'kubeconfig',
                         variable: 'KUBECONFIG'),
                    [$class: 'AmazonWebServicesCredentialsBinding',
                         credentialsId: 'aws-credentials',
                         accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                         secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']
                ]) {
                    // Update the running deployment with the new image
                    bat """
                        kubectl set image deployment/finsecure-pay ^
                          finsecure-pay=%DOCKER_IMAGE%:%DOCKER_TAG% ^
                          --kubeconfig=%KUBECONFIG%
                    """

                    // Wait for rollout to complete
                    bat """
                        kubectl rollout status deployment/finsecure-pay ^
                          --timeout=120s ^
                          --kubeconfig=%KUBECONFIG%
                    """

                    // Print running pods as evidence
                    bat """
                        kubectl get pods -l app=finsecure-pay ^
                          --kubeconfig=%KUBECONFIG%
                    """

                    // Print the service external URL
                    bat """
                        kubectl get service finsecure-pay-service ^
                          --kubeconfig=%KUBECONFIG%
                    """
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST ACTIONS
    // ══════════════════════════════════════════════════════════════════════
    post {

        success {
            echo '========== PIPELINE SUCCEEDED =========='
            emailext(
                subject: "SUCCESS: FinSecure Pay Build #${BUILD_NUMBER} Deployed",
                body: """
                    <h2>Deployment Successful</h2>
                    <table border="1" cellpadding="5">
                      <tr><td><b>Build</b></td><td>#${BUILD_NUMBER}</td></tr>
                      <tr><td><b>Image</b></td><td>${DOCKER_IMAGE}:${DOCKER_TAG}</td></tr>
                      <tr><td><b>Branch</b></td><td>${GIT_BRANCH}</td></tr>
                      <tr><td><b>Commit</b></td><td>${GIT_COMMIT}</td></tr>
                      <tr><td><b>Duration</b></td><td>${currentBuild.durationString}</td></tr>
                    </table>
                    <br/>
                    <p><a href="${BUILD_URL}">View Build</a></p>
                """,
                mimeType: 'text/html',
                to: 'devops-team@finsecure.com'
            )
        }

        failure {
            echo '========== PIPELINE FAILED =========='
            emailext(
                subject: "FAILED: FinSecure Pay Build #${BUILD_NUMBER}",
                body: """
                    <h2>Pipeline Failed</h2>
                    <table border="1" cellpadding="5">
                      <tr><td><b>Build</b></td><td>#${BUILD_NUMBER}</td></tr>
                      <tr><td><b>Branch</b></td><td>${GIT_BRANCH}</td></tr>
                      <tr><td><b>Commit</b></td><td>${GIT_COMMIT}</td></tr>
                    </table>
                    <br/>
                    <p><a href="${BUILD_URL}console">View Console Output</a></p>
                """,
                mimeType: 'text/html',
                to: 'devops-team@finsecure.com'
            )
        }

        always {
            bat 'docker logout'
            cleanWs()
        }
    }
}