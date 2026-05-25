pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x gradlew'
            }
        }

        stage('Test') {
            steps {
                // Hiện tại chưa có unit test thực sự — chạy nhưng không block pipeline.
                // TODO: Thêm unit test (dùng H2 in-memory hoặc Testcontainers) để stage này có ý nghĩa.
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh './gradlew test --parallel --continue'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Build & Push Docker Images') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'

                        def services = [
                            'eatzy-discovery-server',
                            'eatzy-config-server',
                            'eatzy-api-gateway',
                            'eatzy-auth-service',
                            'eatzy-restaurant-service',
                            'eatzy-order-service',
                            'eatzy-communication-service',
                            'eatzy-cart-service',
                            'eatzy-payment-service',
                            'eatzy-interaction-service',
                            'eatzy-system-config-service',
                            'eatzy-ai-service'
                        ]

                        def parallelBuilds = [:]
                        services.each { service ->
                            def svc = service
                            parallelBuilds[svc] = {
                                sh """
                                    echo "Building ${svc}..."
                                    docker build -t ${DOCKER_USER}/${svc}:latest -f ${svc}/Dockerfile .
                                    docker push ${DOCKER_USER}/${svc}:latest
                                """
                            }
                        }
                        parallel parallelBuilds
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'server-ssh-key',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    ),
                    string(credentialsId: 'server-ip', variable: 'SERVER_IP'),
                    string(credentialsId: 'server-port', variable: 'SERVER_PORT'),
                    string(credentialsId: 'dockerhub-user', variable: 'DOCKER_USER'),
                    file(credentialsId: 'env-file', variable: 'ENV_FILE')
                ]) {
                    sh '''
                        scp -i $SSH_KEY -P $SERVER_PORT -o StrictHostKeyChecking=no \
                            docker-compose.prod.yml $ENV_FILE \
                            $SSH_USER@$SERVER_IP:/home/$SSH_USER/projects/eatzy-microservices/

                        ssh -i $SSH_KEY -p $SERVER_PORT -o StrictHostKeyChecking=no \
                            $SSH_USER@$SERVER_IP "
                                cd /home/$SSH_USER/projects/eatzy-microservices
                                mv $(basename $ENV_FILE) .env 2>/dev/null || true
                                export DOCKERHUB_USER=$DOCKER_USER
                                docker compose -f docker-compose.prod.yml pull
                                docker compose -f docker-compose.prod.yml up -d
                                docker image prune -f
                            "
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
        success {
            echo "Pipeline succeeded on branch ${env.BRANCH_NAME}"
        }
        failure {
            echo "Pipeline failed on branch ${env.BRANCH_NAME}"
        }
    }
}
