# Jenkins CI/CD Pipeline - Eatzy Microservices

Tài liệu này mô tả pipeline CI/CD hiện tại của dự án Eatzy Microservices khi chạy bằng Jenkins Multibranch Pipeline.

Pipeline được định nghĩa trong file [`Jenkinsfile`](../Jenkinsfile).

## Mục tiêu

Pipeline có các mục tiêu chính:

- Kiểm tra code mới trên từng branch.
- Build artifact Java bằng Gradle.
- Chỉ trên branch `main`: đóng gói Docker image cho tất cả service.
- Chỉ trên branch `main`: push image lên Docker Hub với tag `latest` và tag theo commit.
- Chỉ trên branch `main`: deploy lên server qua SSH.

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
    +--> branch != main: dừng sau CI
    |
    +--> branch == main:
            Build and push Docker images
            |
            v
            Deploy lên server qua SSH
```

## Hành vi theo branch

| Branch | Test | Build jar | Build image | Push image | Deploy |
|---|---:|---:|---:|---:|---:|
| `main` | Có | Có | Có | Có | Có |
| `feat/*` | Có | Có | Không | Không | Không |
| Branch khác | Có | Có | Không | Không | Không |

Các stage Docker build/push và Deploy đều được chặn bằng điều kiện:

```groovy
when {
    expression {
        isMainBranch()
    }
}
```

## Docker image tags

Pipeline có helper tạo tag dựa trên branch và commit, nhưng stage Docker hiện chỉ chạy trên `main`. Vì vậy trong thực tế mỗi service trên `main` được build với 2 tag:

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
- Branch feature không build/push Docker, tránh ảnh hưởng production và giảm thời gian CI.

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

Nếu test fail, pipeline fail. Đây là hành vi mong muốn cho CI hiện tại: branch lỗi test không được đi tiếp tới build artifact, Docker image hoặc deploy.

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

### Lưu ý riêng cho Windows agent

Khi chạy lệnh Gradle từ Jenkins `bat`, phải gọi `gradlew.bat` bằng `call`:

```bat
call gradlew.bat :eatzy-discovery-server:bootJar ... --parallel -x test
```

Nếu không có `call`, batch script sẽ dừng ngay sau khi `gradlew.bat` kết thúc. Khi đó các lệnh tạo thư mục và copy jar sẽ không chạy, dẫn đến lỗi Docker:

```text
COPY docker-artifacts/eatzy-discovery-server.jar app.jar
not found
```

Log đúng sau stage này phải có đủ 11 dòng dạng:

```text
1 file(s) copied.
```

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

Stage này chỉ chạy trên `main`. Feature branch dừng sau stage `Build Java Artifacts`.

### Dockerfile Java service

Java Dockerfile hiện tại chỉ đóng gói artifact đã build:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY docker-artifacts/eatzy-auth-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Khi build thành công, Docker log sẽ có dạng:

```text
COPY docker-artifacts/eatzy-discovery-server.jar app.jar
DONE
```

Nếu thấy Docker build context khoảng vài chục đến hơn 100 MB, đó là do Docker vẫn gửi workspace hiện tại làm build context. Pipeline đã nhanh hơn vì không compile trong Docker nữa, nhưng vẫn có thể tối ưu tiếp bằng cách giảm build context nếu cần.

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

Trên Windows agent, Jenkins tạo SSH key credential thành file tạm có quyền quá rộng. `Jenkinsfile` copy key sang file riêng trong workspace và dùng `icacls` để chỉ cấp quyền đọc cho user đang chạy Jenkins và `SYSTEM`, tránh lỗi:

```text
WARNING: UNPROTECTED PRIVATE KEY FILE
Load key "...": bad permissions
Load key "...": Permission denied
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

## Dấu hiệu CI branch feature chạy đúng

Với branch `feat/vu`, log đúng sẽ có các điểm sau:

```text
Push event to branch feat/vu
Connecting to https://api.github.com using ...
BUILD SUCCESSFUL
1 file(s) copied.
Stage "Build & Push Docker Images" skipped due to when conditional
Stage "Deploy" skipped due to when conditional
Pipeline succeeded on branch feat/vu
Finished: SUCCESS
```

Điều này xác nhận:

- Jenkins đã dùng GitHub credential, không còn anonymous API.
- Test và `bootJar` đã thành công.
- `docker-artifacts/` đã được tạo đúng.
- Docker image không được build/push trên branch feature.
- Deploy không chạy trên branch feature.

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
