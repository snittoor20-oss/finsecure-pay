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