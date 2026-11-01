stage('Secret Scan - Source Code') {
    steps {
        sh '''
            set -e
            mkdir -p reports

            trivy fs \
              --scanners secret \
              --severity HIGH,CRITICAL \
              --skip-dirs .git \
              --skip-files pay/pom.xml \
              --format json \
              --exit-code 0 \
              --output reports/source-secret-report.json \
              .

            trivy fs \
              --scanners secret \
              --severity HIGH,CRITICAL \
              --skip-dirs .git \
              --skip-files pay/pom.xml \
              --exit-code 1 \
              .
        '''
    }
}