
pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out FinSecure Pay source code'
            }
        }

        stage('Build') {
            steps {
                dir('pay') {
                    bat 'mvn clean compile'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                dir('pay') {
                    bat 'mvn test'
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
                echo 'Building Docker image'
            }
        }

        stage('Deploy to GKE') {
            steps {
                echo 'Deploying application to GKE'
            }
        }
    }
}
