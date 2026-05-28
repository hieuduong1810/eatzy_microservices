def runCommand(String unixCommand, String windowsCommand = null) {
    if (isUnix()) {
        sh unixCommand
    } else {
        bat windowsCommand ?: unixCommand
    }
}

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
                script {
                    runCommand(
                        'chmod +x gradlew',
                        'if exist gradlew.bat echo Windows agent detected - skipping chmod'
                    )
                }
            }
        }

        stage('Test') {
            steps {
                // Hiện tại chưa có unit test thực sự — chạy nhưng không block pipeline.
                // TODO: Thêm unit test (dùng H2 in-memory hoặc Testcontainers) để stage này có ý nghĩa.
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    script {
                        runCommand(
                            './gradlew test --parallel --continue',
                            'gradlew.bat test --parallel --continue'
                        )
                    }
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
                        runCommand(
                            'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin',
                            'echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin'
                        )

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
                                def imageName = "${env.DOCKER_USER}/${svc}:latest"
                                runCommand(
                                    """
                                        echo "Building ${svc}..."
                                        docker build -t ${imageName} -f ${svc}/Dockerfile .
                                        docker push ${imageName}
                                    """,
                                    """
                                        echo Building ${svc}...
                                        docker build -t ${imageName} -f ${svc}/Dockerfile .
                                        docker push ${imageName}
                                    """
                                )
                            }
                        }
                        parallel parallelBuilds
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    env.BRANCH_NAME == 'main' || env.GIT_BRANCH == 'main' || env.GIT_BRANCH == 'origin/main'
                }
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
                    script {
                        runCommand(
                            '''
                                scp -i "$SSH_KEY" -P "$SERVER_PORT" -o StrictHostKeyChecking=no \
                                    docker-compose.prod.yml \
                                    "$SSH_USER@$SERVER_IP:/home/$SSH_USER/projects/eatzy-microservices/docker-compose.prod.yml"

                                scp -i "$SSH_KEY" -P "$SERVER_PORT" -o StrictHostKeyChecking=no \
                                    "$ENV_FILE" \
                                    "$SSH_USER@$SERVER_IP:/home/$SSH_USER/projects/eatzy-microservices/.env"

                                ssh -i "$SSH_KEY" -p "$SERVER_PORT" -o StrictHostKeyChecking=no \
                                    "$SSH_USER@$SERVER_IP" "
                                        cd /home/$SSH_USER/projects/eatzy-microservices
                                        export DOCKERHUB_USER=$DOCKER_USER
                                        docker compose -f docker-compose.prod.yml pull
                                        docker compose -f docker-compose.prod.yml up -d
                                        docker image prune -f
                                    "
                            ''',
                            '''
                                scp -i "%SSH_KEY%" -P %SERVER_PORT% -o StrictHostKeyChecking=no docker-compose.prod.yml "%SSH_USER%@%SERVER_IP%:/home/%SSH_USER%/projects/eatzy-microservices/docker-compose.prod.yml"
                                scp -i "%SSH_KEY%" -P %SERVER_PORT% -o StrictHostKeyChecking=no "%ENV_FILE%" "%SSH_USER%@%SERVER_IP%:/home/%SSH_USER%/projects/eatzy-microservices/.env"
                                ssh -i "%SSH_KEY%" -p %SERVER_PORT% -o StrictHostKeyChecking=no "%SSH_USER%@%SERVER_IP%" "cd /home/%SSH_USER%/projects/eatzy-microservices && export DOCKERHUB_USER=%DOCKER_USER% && docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d && docker image prune -f"
                            '''
                        )
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                runCommand('docker logout || true', 'docker logout || exit /b 0')
            }
        }
        success {
            echo "Pipeline succeeded on branch ${env.BRANCH_NAME}"
        }
        failure {
            echo "Pipeline failed on branch ${env.BRANCH_NAME}"
        }
    }
}
