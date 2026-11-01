
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

        
stage('Trivy Secret Security Gate') {
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
