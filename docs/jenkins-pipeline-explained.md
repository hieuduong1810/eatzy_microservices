# Jenkinsfile Explained - Eatzy Microservices

Tài liệu này giải thích cách `Jenkinsfile` của dự án hoạt động, gồm helper functions, branch logic, tagging, build artifact, Docker build và deploy.

File liên quan: [`Jenkinsfile`](../Jenkinsfile)

Pipeline hiện tại chạy CI cho mọi branch đến hết stage `Build Java Artifacts`. Hai stage `Build & Push Docker Images` và `Deploy` chỉ chạy trên branch `main`.

## 1. Kiểu pipeline

Dự án dùng Declarative Pipeline:

```groovy
pipeline {
    agent any
    options { ... }
    stages { ... }
    post { ... }
}
```

`agent any` nghĩa là Jenkins có thể chạy pipeline trên bất kỳ node/agent nào phù hợp.

Trong setup hiện tại, Jenkins agent đang chạy trên Windows:

```text
C:\ProgramData\Jenkins\.jenkins\workspace\...
```

Vì vậy `Jenkinsfile` phải hỗ trợ cả Windows và Linux.

## 2. Helper `runCommand`

```groovy
def runCommand(String unixCommand, String windowsCommand = null) {
    if (isUnix()) {
        sh unixCommand
    } else {
        bat windowsCommand ?: unixCommand
    }
}
```

Mục đích:

- Nếu agent là Linux/macOS: chạy `sh`.
- Nếu agent là Windows: chạy `bat`.
- Giảm lặp code khi pipeline cần chạy được trên cả hai môi trường.

Ví dụ:

```groovy
runCommand(
    './gradlew test --parallel --continue',
    'gradlew.bat test --parallel --continue'
)
```

Nếu không có helper này, Jenkins trên Windows sẽ lỗi:

```text
Cannot run program "sh"
CreateProcess error=2
```

## 3. Branch detection

```groovy
def currentBranchName() {
    return (env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'manual')
        .replaceFirst('^origin/', '')
}
```

Jenkins Multibranch thường có `env.BRANCH_NAME`.

Pipeline fallback sang `env.GIT_BRANCH` nếu không có `BRANCH_NAME`.

Nếu chạy manual trong trường hợp không có branch, dùng `manual`.

Ví dụ:

| Input | Output |
|---|---|
| `feat/vu` | `feat/vu` |
| `origin/main` | `main` |
| empty | `manual` |

## 4. Main branch detection

```groovy
def isMainBranch() {
    return currentBranchName() == 'main'
}
```

Hàm này được dùng để:

- Tạo tag `latest` chỉ trên `main`.
- Cho phép build/push Docker image chỉ trên `main`.
- Cho phép deploy chỉ trên `main`.

## 5. Docker-safe tag

```groovy
def dockerSafeTag(String value) {
    def tag = value
        .toLowerCase()
        .replaceAll('[^a-z0-9_.-]+', '-')
        .replaceAll('(^[-.]+|[-.]+$)', '')
    return tag ?: 'manual'
}
```

Docker tag không nên chứa dấu `/`, khoảng trắng, ký tự đặc biệt.

Hàm này biến branch name thành tag hợp lệ.

Ví dụ:

| Branch | Docker-safe tag |
|---|---|
| `feat/vu` | `feat-vu` |
| `Feature/Login` | `feature-login` |
| `bugfix/payment#12` | `bugfix-payment-12` |

## 6. Image tags

```groovy
def imageTagsForBuild() {
    def shortCommit = (env.GIT_COMMIT ?: env.BUILD_NUMBER ?: 'local').take(7)
    def versionTag = "${dockerSafeTag(currentBranchName())}-${shortCommit}"
    return isMainBranch() ? ['latest', versionTag] : [versionTag]
}
```

Nếu branch là `main`:

```text
latest
main-<commit>
```

Nếu branch khác:

```text
<branch>-<commit>
```

Lưu ý: `Jenkinsfile` hiện tại vẫn định nghĩa cách tạo tag cho branch khác, nhưng stage Docker chỉ chạy trên `main`. Vì vậy tag branch khác chưa được push trong flow hiện tại.

Mục đích:

- Production vẫn dùng `latest`.
- Vẫn có tag theo commit để debug/rollback.
- Feature branch không ghi đè image production.

## 7. Service lists

```groovy
def javaServices() {
    return [
        'eatzy-discovery-server',
        ...
        'eatzy-system-config-service'
    ]
}
```

`javaServices()` gồm 11 service Spring Boot.

```groovy
def dockerServices() {
    return javaServices() + ['eatzy-ai-service']
}
```

`dockerServices()` gồm 11 Java services và 1 Python AI service.

## 8. Pipeline options

```groovy
options {
    timeout(time: 60, unit: 'MINUTES')
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '10'))
}
```

| Option | Ý nghĩa |
|---|---|
| `timeout(60 min)` | Chặn build treo quá lâu |
| `disableConcurrentBuilds()` | Không cho cùng một job chạy song song |
| `buildDiscarder(...)` | Chỉ giữ 10 build gần nhất |

`disableConcurrentBuilds()` quan trọng khi Docker Desktop trên Windows đang build image, vì nhiều build chồng lên nhau dễ gây lỗi storage/cache.

## 9. Stage `Checkout`

```groovy
checkout scm
```

Trong Multibranch Pipeline, `scm` được Jenkins tạo sẵn dựa trên branch source.

Sau checkout:

```groovy
runCommand(
    'chmod +x gradlew',
    'if exist gradlew.bat echo Windows agent detected - skipping chmod'
)
```

Linux cần `chmod +x gradlew`.

Windows dùng `gradlew.bat`, không cần chmod.

## 10. Stage `Test`

```groovy
script {
    runCommand(
        './gradlew test --parallel --continue',
        'gradlew.bat test --parallel --continue'
    )
}
```

Nếu test fail, pipeline fail. Stage này không dùng `catchError`, vì code lỗi test không nên đi tiếp sang Docker build hoặc deploy.

## 11. Stage `Build Java Artifacts`

Stage này gồm 2 việc:

1. Chạy Gradle `bootJar` cho tất cả Java services.
2. Copy jar sang `docker-artifacts/`.

Lý do cần stage này:

- Trước đây mỗi Dockerfile tự build lại service bằng Gradle.
- Trên Windows Docker Desktop, việc này rất chậm và dễ lỗi BuildKit/containerd.
- Build jar một lần trên Jenkins agent nhanh và ổn định hơn.

Output:

```text
docker-artifacts/eatzy-auth-service.jar
docker-artifacts/eatzy-cart-service.jar
...
```

Dockerfile Java sau đó chỉ copy file jar này.

### Vì sao Windows dùng `call gradlew.bat`

Trong Jenkins `bat`, khi gọi một file `.bat` khác, phải dùng `call`:

```bat
call gradlew.bat :eatzy-discovery-server:bootJar ... --parallel -x test
```

Nếu chỉ viết:

```bat
gradlew.bat :eatzy-discovery-server:bootJar ... --parallel -x test
```

batch script cha sẽ kết thúc ngay sau Gradle. Các lệnh sau đó sẽ không chạy:

```bat
mkdir docker-artifacts
copy /Y ...
```

Kết quả là Docker stage fail vì không tìm thấy jar:

```text
COPY docker-artifacts/eatzy-discovery-server.jar app.jar
not found
```

Log đúng sau khi copy artifact trên Windows sẽ có 11 dòng:

```text
1 file(s) copied.
```

## 12. Stage `Build & Push Docker Images`

Stage này có điều kiện:

```groovy
when {
    expression {
        isMainBranch()
    }
}
```

Vì vậy chỉ branch `main` mới build/push Docker images. Feature branch dừng sau `Build Java Artifacts`.

Pipeline login Docker Hub bằng:

```groovy
withCredentials([usernamePassword(
    credentialsId: 'dockerhub-credentials',
    usernameVariable: 'DOCKER_USER',
    passwordVariable: 'DOCKER_PASS'
)]) {
    ...
}
```

Không in password ra log. Jenkins mask giá trị `%DOCKER_PASS%`/`$DOCKER_PASS`.

Lệnh build/push tương đương:

```bash
docker build -t <dockerhub-user>/eatzy-auth-service:latest -t <dockerhub-user>/eatzy-auth-service:main-66dea5b -f eatzy-auth-service/Dockerfile .
docker push <dockerhub-user>/eatzy-auth-service:latest
docker push <dockerhub-user>/eatzy-auth-service:main-66dea5b
```

Trên `main`, lệnh build có nhiều tag:

```bash
docker build \
  -t <dockerhub-user>/eatzy-auth-service:latest \
  -t <dockerhub-user>/eatzy-auth-service:main-66dea5b \
  -f eatzy-auth-service/Dockerfile .
```

Sau đó push từng tag.

## 13. Windows vs Linux Docker build

```groovy
if (isUnix()) {
    parallel parallelBuilds
} else {
    services.each { service ->
        buildAndPushImage(service)
    }
}
```

Linux agent build song song để nhanh hơn.

Windows agent build tuần tự để tránh các lỗi:

```text
write /var/lib/docker/buildkit/containerd-overlayfs/metadata_v2.db: input/output error
write /var/lib/desktop-containerd/.../meta.db: input/output error
```

## 14. Stage `Deploy`

Deploy chỉ chạy khi:

```groovy
isMainBranch()
```

Nếu build branch `feat/vu`, stage `Deploy` sẽ bị skip.

Deploy dùng các Jenkins credentials:

```groovy
sshUserPrivateKey(credentialsId: 'server-ssh-key', ...)
string(credentialsId: 'server-ip', ...)
string(credentialsId: 'server-port', ...)
string(credentialsId: 'dockerhub-user', ...)
file(credentialsId: 'env-file', ...)
```

Pipeline copy file cần thiết lên server bằng `scp`, sau đó chạy `docker compose` qua `ssh`.

Trên Windows agent, đoạn deploy dùng file key tạm riêng:

```bat
set "SSH_KEY_SAFE=%WORKSPACE%\.jenkins-server-ssh-key-%BUILD_NUMBER%"
copy /Y "%SSH_KEY%" "%SSH_KEY_SAFE%" >nul
for /f "delims=" %%U in ('whoami') do set "CURRENT_USER=%%U"
icacls "%SSH_KEY_SAFE%" /inheritance:r
icacls "%SSH_KEY_SAFE%" /grant:r "%CURRENT_USER%:R" "*S-1-5-18:R"
```

Mục đích là tránh lỗi Windows OpenSSH từ chối private key:

```text
WARNING: UNPROTECTED PRIVATE KEY FILE
Load key "...": bad permissions
Load key "...": Permission denied
```

## 15. Post actions

`post { always { ... } }` luôn chạy, kể cả pipeline fail.

Đang dùng để:

```groovy
docker logout
```

Mục đích:

- Xóa Docker Hub login session khỏi agent.
- Giảm rủi ro credentials lưu lại trên máy Jenkins.

## 16. Những điểm cần chú ý

### Không cần `triggers { githubPush() }` trong Multibranch

Multibranch Pipeline đã nhận webhook và scan branch riêng.

Nếu thêm `triggers { githubPush() }`, có thể thấy log:

```text
Started by GitHub push
Started by GitHub push
```

Do đó pipeline đã bỏ block trigger này.

### Jenkins API credential khác Git checkout credential

Trong scan log, cần thấy:

```text
Connecting to https://api.github.com using ...
```

Nếu vẫn thấy:

```text
with no credentials, anonymous access
```

thì Branch Source chưa dùng GitHub token cho API.

### Docker artifacts không được commit

`docker-artifacts/` chỉ là output tạm thời của pipeline/local build.

Thư mục này đã được thêm vào `.gitignore`.

## 17. Khi nào pipeline fail?

Một số lỗi thường gặp:

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Cannot run program "sh"` | Jenkins agent là Windows nhưng pipeline dùng `sh` | Dùng `runCommand` với `bat` |
| `server-ssh-key not found` | Thiếu deploy credential | Tạo credential đúng ID |
| `Docker login failed` | Sai Docker Hub token | Tạo lại Docker Hub access token |
| `COPY docker-artifacts/...jar: not found` | Windows batch script dừng sau `gradlew.bat`, chưa copy jar | Dùng `call gradlew.bat ...` trong stage `Build Java Artifacts` |
| `open //./pipe/docker_engine` | Docker Desktop chưa chạy trên máy Jenkins Windows | Mở Docker Desktop, test `docker ps`, restart Jenkins service nếu cần |
| `UNPROTECTED PRIVATE KEY FILE` | ACL file SSH key quá rộng trên Windows | Dùng đoạn `SSH_KEY_SAFE` và `icacls` trong Jenkinsfile |
| `meta.db input/output error` | Docker Desktop storage lỗi | Restart Docker Desktop, `wsl --shutdown`, prune cache |
| `zip END header not found` | Gradle wrapper zip trong Docker cache corrupt | Không build Gradle trong Docker nữa, build artifact trước |
| Deploy skipped | Build branch khác `main` | Đây là hành vi đúng |

## 18. Log thành công hiện tại

Một build branch feature được xem là đúng khi có chuỗi kết quả:

```text
BUILD SUCCESSFUL in ...s
1 file(s) copied.
Stage "Build & Push Docker Images" skipped due to when conditional
Stage "Deploy" skipped due to when conditional
Pipeline succeeded on branch feat/vu
Finished: SUCCESS
```

Nếu branch là `feat/vu`, Docker build/push và deploy bị skip là đúng thiết kế. Chỉ `main` mới build/push image và deploy.

Một build `main` deploy thành công sẽ có các log kiểu:

```text
docker push <dockerhub-user>/eatzy-api-gateway:latest
docker push <dockerhub-user>/eatzy-api-gateway:main-<commit>
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
Container eatzy-api-gateway Started
Pipeline succeeded on branch main
Finished: SUCCESS
```
