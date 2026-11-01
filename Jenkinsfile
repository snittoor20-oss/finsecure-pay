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

        stage('Docker Build') {
            steps {
                sh 'docker build -t dskn123/finsecure-pay:trivy-demo ./pay'
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
