pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out FinSecure Pay'
            }
        }

        stage('Build') {
            steps {
                dir('pay') {
                    sh 'mvn clean compile'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                dir('pay') {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit 'pay/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build and Test') {
    steps {
        sh '''
            docker run --rm \
              -v "$WORKSPACE:/workspace" \
              -w /workspace/pay \
              maven:3.9.9-eclipse-temurin-17 \
              mvn clean verify
        '''
    }
    post {
        always {
            junit 'pay/target/surefire-reports/*.xml'
        }
    }
}

        stage('Container Image Scan - Trivy') {
            steps {
                sh 'trivy image --severity HIGH,CRITICAL --exit-code 1 dskn123/finsecure-pay:trivy-demo'
            }
        }

        stage('Deploy to GKE') {
            steps {
                echo 'Deploying application to GKE'
            }
        }
    }
}
