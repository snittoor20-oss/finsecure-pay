pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo 'FinSecure Pay pipeline started'
            }
        }

        stage('Build') {
            steps {
                echo 'Ready to build FinSecure Pay'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully'
        }
        failure {
            echo 'Pipeline failed'
        }
    }
}
