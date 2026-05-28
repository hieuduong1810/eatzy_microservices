# Jenkins CI Pipeline - Eatzy Microservices

Tài liệu này mô tả pipeline CI hiện tại của dự án Eatzy Microservices khi chạy bằng Jenkins Multibranch Pipeline.

Pipeline được định nghĩa trong file [`Jenkinsfile`](../Jenkinsfile).

## Mục tiêu

CI pipeline có các mục tiêu chính:

- Kiểm tra code mới trên từng branch.
- Build artifact Java bằng Gradle.
- Đóng gói Docker image cho tất cả service.
- Push image lên Docker Hub với tag theo branch và commit.
- Chỉ deploy khi build trên branch `main`.

## Tổng quan luồng CI/CD

```text
GitHub push
    |
    v
GitHub webhook
    |
    v
Jenkins Multibranch Pipeline
    |
    v
Checkout source
    |
    v
Run Gradle tests
    |
    v
Build Java bootJar artifacts
    |
    v
Build and push Docker images
    |
    +--> branch != main: dừng sau CI
    |
    +--> branch == main: deploy lên server
```

## Hành vi theo branch

| Branch | Test | Build jar | Build image | Push image | Deploy |
|---|---:|---:|---:|---:|---:|
| `main` | Có | Có | Có | Có | Có |
| `feat/*` | Có | Có | Có | Có | Không |
| Branch khác | Có | Có | Có | Có | Không |

Deploy được chặn bằng điều kiện:

```groovy
when {
    expression {
        isMainBranch()
    }
}
```

## Docker image tags

Pipeline tạo tag dựa trên branch và commit.

Với branch khác `main`:

```text
<branch-safe-name>-<short-commit>
```

Ví dụ:

```text
feat-vu-66dea5b
```

Với branch `main`, pipeline tạo 2 tag:

```text
latest
main-<short-commit>
```

Ví dụ:

```text
latest
main-66dea5b
```

Lý do:

- `latest` để production compose file có thể pull image mới nhất.
- `main-<commit>` để truy vết image được build từ commit nào.
- Branch feature không ghi đè `latest`, tránh ảnh hưởng production.

## Services được build

### Java services

Pipeline build artifact cho 11 Java services:

```text
eatzy-discovery-server
eatzy-config-server
eatzy-api-gateway
eatzy-auth-service
eatzy-restaurant-service
eatzy-order-service
eatzy-communication-service
eatzy-cart-service
eatzy-payment-service
eatzy-interaction-service
eatzy-system-config-service
```

### Python service

Service Python không có bước `bootJar`:

```text
eatzy-ai-service
```

Docker image của AI service được build trực tiếp từ `eatzy-ai-service/Dockerfile`.

## Stage 1 - Checkout

Jenkins checkout source code từ GitHub.

Trong Multibranch Pipeline, Jenkins tự biết branch nào đang build qua các biến:

- `BRANCH_NAME`
- `GIT_BRANCH`
- `GIT_COMMIT`

Sau khi checkout, pipeline xử lý khác nhau cho Windows/Linux:

```groovy
runCommand(
    'chmod +x gradlew',
    'if exist gradlew.bat echo Windows agent detected - skipping chmod'
)
```

Trên Linux, `gradlew` cần executable bit. Trên Windows, dùng `gradlew.bat`, không cần `chmod`.

## Stage 2 - Test

Lệnh test:

Linux:

```bash
./gradlew test --parallel --continue
```

Windows:

```powershell
gradlew.bat test --parallel --continue
```

Stage này đang được bọc trong:

```groovy
catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE')
```

Ý nghĩa:

- Nếu test fail, stage được đánh dấu `UNSTABLE`.
- Build tổng thể vẫn có thể tiếp tục.
- Đây là cấu hình tạm thời vì dự án chưa có đầy đủ unit/integration tests.

Pipeline publish JUnit report:

```groovy
junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
```

`allowEmptyResults: true` giúp pipeline không fail khi service chưa có test.

## Stage 3 - Build Java Artifacts

Stage này build tất cả Java service một lần trên Jenkins agent.

Lệnh tương đương:

```bash
./gradlew \
  :eatzy-discovery-server:bootJar \
  :eatzy-config-server:bootJar \
  :eatzy-api-gateway:bootJar \
  :eatzy-auth-service:bootJar \
  :eatzy-restaurant-service:bootJar \
  :eatzy-order-service:bootJar \
  :eatzy-communication-service:bootJar \
  :eatzy-cart-service:bootJar \
  :eatzy-payment-service:bootJar \
  :eatzy-interaction-service:bootJar \
  :eatzy-system-config-service:bootJar \
  --parallel -x test
```

Sau đó pipeline copy jar vào thư mục:

```text
docker-artifacts/
```

Dạng file:

```text
docker-artifacts/eatzy-discovery-server.jar
docker-artifacts/eatzy-config-server.jar
docker-artifacts/eatzy-api-gateway.jar
...
```

Thư mục `docker-artifacts/` được đưa vào `.gitignore`, chỉ dùng trong workspace Jenkins/local.

Lý do tách bước này ra khỏi Docker build:

- Không phải tải Gradle wrapper 11 lần trong container.
- Không phải compile lại Java trong từng Dockerfile.
- Giảm lỗi Docker Desktop/BuildKit cache trên Windows.
- Docker build nhanh hơn vì chỉ copy jar vào runtime image.

## Stage 4 - Build and Push Docker Images

Pipeline login Docker Hub bằng credential:

```text
dockerhub-credentials
```

Lệnh tương đương:

```bash
echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
```

Sau đó build/push image cho 12 services.

### Dockerfile Java service

Java Dockerfile hiện tại chỉ đóng gói artifact đã build:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY docker-artifacts/eatzy-auth-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Dockerfile AI service

AI service vẫn build Python dependencies trong Dockerfile:

```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY eatzy-ai-service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY eatzy-ai-service/ .
EXPOSE 8089
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8089"]
```

## Build song song hay tuần tự

Pipeline phân biệt agent:

- Linux/Unix agent: build/push Docker images song song.
- Windows agent: build/push Docker images tuần tự.

Lý do Windows build tuần tự:

- Docker Desktop trên Windows/WSL2 dễ gặp lỗi BuildKit storage khi build nhiều image cùng lúc.
- Các lỗi thường gặp:
  - `input/output error`
  - `metadata_v2.db`
  - `meta.db`
  - containerd overlayfs error

## Stage 5 - Deploy

Deploy chỉ chạy trên `main`.

Jenkins cần các credentials:

| Credential ID | Loại | Mục đích |
|---|---|---|
| `server-ssh-key` | SSH username with private key | SSH vào server |
| `server-ip` | Secret text | IP/hostname server |
| `server-port` | Secret text | SSH port |
| `dockerhub-user` | Secret text | Docker Hub username cho compose |
| `env-file` | Secret file | File `.env` production |

Pipeline copy:

- `docker-compose.prod.yml`
- `.env`

Sau đó SSH vào server và chạy:

```bash
cd /home/$SSH_USER/projects/eatzy-microservices
export DOCKERHUB_USER=$DOCKER_USER
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
docker image prune -f
```

## Post actions

Pipeline luôn logout Docker:

Linux:

```bash
docker logout || true
```

Windows:

```bat
docker logout || exit /b 0
```

Sau đó echo kết quả:

```text
Pipeline succeeded on branch <branch>
```

hoặc:

```text
Pipeline failed on branch <branch>
```

## Cách chạy CI trước khi push

Jenkins Multibranch lấy code từ GitHub, nên Jenkins không thể build commit local chưa push trong cấu hình hiện tại.

Có thể kiểm tra local bằng lệnh tương đương:

Windows:

```powershell
.\gradlew.bat test --parallel --continue
.\gradlew.bat :eatzy-discovery-server:bootJar :eatzy-config-server:bootJar :eatzy-api-gateway:bootJar :eatzy-auth-service:bootJar :eatzy-restaurant-service:bootJar :eatzy-order-service:bootJar :eatzy-communication-service:bootJar :eatzy-cart-service:bootJar :eatzy-payment-service:bootJar :eatzy-interaction-service:bootJar :eatzy-system-config-service:bootJar --parallel -x test
```

Linux/WSL:

```bash
./gradlew test --parallel --continue
./gradlew :eatzy-discovery-server:bootJar :eatzy-config-server:bootJar :eatzy-api-gateway:bootJar :eatzy-auth-service:bootJar :eatzy-restaurant-service:bootJar :eatzy-order-service:bootJar :eatzy-communication-service:bootJar :eatzy-cart-service:bootJar :eatzy-payment-service:bootJar :eatzy-interaction-service:bootJar :eatzy-system-config-service:bootJar --parallel -x test
```

Muốn Jenkins chạy thật trước khi merge `main`, push lên branch riêng:

```bash
git checkout -b ci/test-local
git push origin ci/test-local
```

Sau khi test xong:

```bash
git push origin --delete ci/test-local
git checkout feat/vu
git branch -D ci/test-local
```
