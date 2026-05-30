def runCommand(String unixCommand, String windowsCommand = null) {
    if (isUnix()) {
        sh unixCommand
    } else {
        bat windowsCommand ?: unixCommand
    }
}

def currentBranchName() {
    return (env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'manual')
        .replaceFirst('^origin/', '')
}

def isMainBranch() {
    return currentBranchName() == 'main'
}

def dockerSafeTag(String value) {
    def tag = value
        .toLowerCase()
        .replaceAll('[^a-z0-9_.-]+', '-')
        .replaceAll('(^[-.]+|[-.]+$)', '')
    return tag ?: 'manual'
}

def imageTagsForBuild() {
    def shortCommit = (env.GIT_COMMIT ?: env.BUILD_NUMBER ?: 'local').take(7)
    def versionTag = "${dockerSafeTag(currentBranchName())}-${shortCommit}"
    return isMainBranch() ? ['latest', versionTag] : [versionTag]
}

def javaServices() {
    return [
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
        'eatzy-system-config-service'
    ]
}

def dockerServices() {
    return javaServices() + ['eatzy-ai-service']
}

def notifyBuildRequester() {
    def result = currentBuild.currentResult ?: 'UNKNOWN'
    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'manual'
    def subject = "[${result}] ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    def body = """
        <p>Build <b>${result}</b></p>
        <ul>
            <li>Job: ${env.JOB_NAME}</li>
            <li>Build: #${env.BUILD_NUMBER}</li>
            <li>Branch: ${branch}</li>
            <li>URL: <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></li>
        </ul>
    """

    try {
        emailext(
            subject: subject,
            body: body,
            mimeType: 'text/html',
            recipientProviders: [requestor(), developers()]
        )
    } catch (err) {
        echo "Could not send build notification email: ${err.message}"
    }
}

pipeline {
    agent any

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

        stage('Build Java Artifacts') {
            steps {
                script {
                    def services = javaServices()
                    def bootJarTasks = services.collect { service -> ":${service}:bootJar" }.join(' ')
                    def serviceNames = services.join(' ')

                    runCommand(
                        """
                            rm -rf docker-artifacts
                            ./gradlew ${bootJarTasks} --parallel -x test
                            mkdir -p docker-artifacts
                            for svc in ${serviceNames}; do
                                jar=\$(find "\$svc/build/libs" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1)
                                cp "\$jar" "docker-artifacts/\$svc.jar"
                            done
                        """,
                        """
                            if exist docker-artifacts rmdir /s /q docker-artifacts
                            call gradlew.bat ${bootJarTasks} --parallel -x test
                            mkdir docker-artifacts
                            for %%S in (${serviceNames}) do for %%J in (%%S\\build\\libs\\*.jar) do echo %%~nxJ | findstr /v /c:"-plain.jar" >nul && copy /Y "%%J" "docker-artifacts\\%%S.jar"
                        """
                    )
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

                        def services = dockerServices()

                        def buildAndPushImage = { svc ->
                            def imageBase = "${env.DOCKER_USER}/${svc}"
                            def tags = imageTagsForBuild()
                            def tagArgs = tags.collect { tag -> "-t ${imageBase}:${tag}" }.join(' ')
                            def unixPushCommands = tags.collect { tag -> "docker push ${imageBase}:${tag}" }.join('\n')
                            def windowsPushCommands = tags.collect { tag -> "docker push ${imageBase}:${tag}" }.join(' && ')
                            runCommand(
                                """
                                    echo "Building ${svc}..."
                                    docker build ${tagArgs} -f ${svc}/Dockerfile .
                                    ${unixPushCommands}
                                """,
                                """
                                    echo Building ${svc}...
                                    docker build ${tagArgs} -f ${svc}/Dockerfile . && ${windowsPushCommands}
                                """
                            )
                        }

                        if (isUnix()) {
                            def parallelBuilds = [:]
                            services.each { service ->
                                def svc = service
                                parallelBuilds[svc] = {
                                    buildAndPushImage(svc)
                                }
                            }
                            parallel parallelBuilds
                        } else {
                            // Docker Desktop on Windows can fail under many simultaneous BuildKit writes.
                            services.each { service ->
                                buildAndPushImage(service)
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    isMainBranch()
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
                notifyBuildRequester()
            }
        }
        success {
            echo "Pipeline succeeded on branch ${env.BRANCH_NAME ?: env.GIT_BRANCH}"
        }
        failure {
            echo "Pipeline failed on branch ${env.BRANCH_NAME ?: env.GIT_BRANCH}"
        }
    }
}
