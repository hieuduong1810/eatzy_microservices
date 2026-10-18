// GitHub Actions is the active CI/CD provider. Keep Jenkins paused.
// The previous implementation is preserved in ci/Jenkinsfile.jenkins.
pipeline {
    agent none
    stages {
        stage('Jenkins paused') {
            steps {
                echo 'CI/CD runs in GitHub Actions. Jenkins will not build, publish or deploy.'
            }
        }
    }
}
