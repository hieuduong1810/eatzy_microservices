# Jenkins CI/CD Setup Guide - Eatzy Microservices

Tài liệu này hướng dẫn cấu hình Jenkins local để chạy đúng [`Jenkinsfile`](../Jenkinsfile) hiện tại của dự án Eatzy Microservices.

Pipeline hiện tại:

```text
Push/merge code
  -> GitHub webhook
  -> Jenkins Multibranch Pipeline
  -> Checkout
  -> Test
  -> Build Java Artifacts
  -> nếu branch != main: dừng tại đây
  -> nếu branch == main: Build & Push Docker Images
  -> Deploy qua SSH lên server
```

## 1. Yêu cầu trên máy Jenkins

Máy chạy Jenkins cần có:

| Thành phần | Ghi chú |
|---|---|
| Jenkins 2.x | Local hoặc server riêng |
| Java 17 | Chạy Gradle build |
| Git | Checkout source từ GitHub |
| Docker Desktop/Docker Engine | Build và push image |
| Docker Compose plugin | Kiểm tra tương thích với server |
| OpenSSH client | Chạy `ssh` và `scp` |
| Ngrok | Nếu Jenkins local cần nhận webhook từ GitHub |

Kiểm tra nhanh trên Windows PowerShell:

```powershell
java -version
git --version
docker ps
docker compose version
ssh -V
```

Nếu `docker ps` báo lỗi pipe hoặc daemon, mở Docker Desktop trước, chờ Docker Engine running rồi thử lại.

## 2. Jenkins plugins cần có

Vào:

```text
Manage Jenkins -> Plugins -> Available plugins
```

Cài các plugin:

| Plugin | Mục đích |
|---|---|
| Pipeline | Chạy Jenkinsfile |
| Git | Checkout source |
| GitHub | Nhận webhook/tích hợp GitHub |
| GitHub Branch Source | Multibranch Pipeline |
| Credentials Binding | Inject credentials vào pipeline |
| SSH Credentials | Lưu SSH private key |
| Docker Pipeline | Hỗ trợ môi trường Docker |
| JUnit | Publish test result |
| Email Extension | Gửi email build result nếu cấu hình SMTP |

Restart Jenkins nếu được yêu cầu.

## 3. Jenkins credentials bắt buộc

Vào:

```text
Manage Jenkins -> Credentials -> System -> Global credentials -> Add Credentials
```

Tạo đúng các credential ID dưới đây. ID phải khớp chính xác với `Jenkinsfile`.

### 3.1 GitHub credential

Dùng cho Multibranch Pipeline scan repo và checkout code.

| Field | Giá trị |
|---|---|
| Kind | `Username with password` |
| Username | GitHub username |
| Password | GitHub Personal Access Token |
| ID | ví dụ `github` hoặc `githubtokens` |

Token cần quyền đọc repo. Nếu repo private, token cần quyền `repo`. Nếu Jenkins tự quản lý webhook, cần thêm quyền quản lý webhook.

### 3.2 Docker Hub login

`Jenkinsfile` dùng credential này để `docker login` và `docker push`.

| Field | Giá trị |
|---|---|
| Kind | `Username with password` |
| Username | Docker Hub username/namespace production |
| Password | Docker Hub access token |
| ID | `dockerhub-credentials` |

Ví dụ nếu server đang chạy image:

```text
duonghieu1810/eatzy-api-gateway:latest
```

thì username nên là:

```text
duonghieu1810
```

Không paste Docker Hub token vào chat hoặc commit vào repo. Nếu token bị lộ, revoke và tạo token mới.

### 3.3 Docker Hub username cho deploy

`docker-compose.prod.yml` dùng biến `DOCKERHUB_USER`.

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | Docker Hub username/namespace, ví dụ `duonghieu1810` |
| ID | `dockerhub-user` |

Giá trị này phải trùng namespace image mà server sẽ pull.

### 3.4 SSH key deploy server

`Jenkinsfile` dùng key này để `scp` và `ssh` vào server.

| Field | Giá trị |
|---|---|
| Kind | `SSH Username with private key` |
| Username | SSH user trên server, ví dụ `hieu` |
| Private Key | Private key tương ứng public key đã add trên server |
| ID | `server-ssh-key` |

Test trên máy Jenkins trước:

```powershell
ssh -i C:\Users\ADMIN\.ssh\jenkins-deploy-key -p 38283 hieu@hieussh.hoanduong.net
```

Nếu vào được server không cần password, Jenkins có thể dùng key đó.

### 3.5 Server host

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | Host/IP server, ví dụ `hieussh.hoanduong.net` |
| ID | `server-ip` |

### 3.6 Server SSH port

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | SSH port, ví dụ `38283` |
| ID | `server-port` |

### 3.7 Production `.env`

| Field | Giá trị |
|---|---|
| Kind | `Secret file` |
| File | File `.env` production |
| ID | `env-file` |

Jenkins sẽ copy file này lên server thành:

```text
/home/<ssh-user>/projects/eatzy-microservices/.env
```

Không commit `.env` production lên GitHub.

## 4. Chuẩn bị server deploy

Trên server production:

```bash
mkdir -p /home/hieu/projects/eatzy-microservices
cd /home/hieu/projects/eatzy-microservices
docker ps
docker compose version
ls -la eatzy.jks
```

Server cần:

- Docker Engine.
- Docker Compose plugin.
- User SSH chạy được `docker ps`.
- Thư mục `/home/<ssh-user>/projects/eatzy-microservices`.
- File `eatzy.jks` nếu compose mount `./eatzy.jks:/app/eatzy.jks:ro`.

Nếu `docker ps` báo permission denied:

```bash
sudo usermod -aG docker hieu
```

Sau đó logout/login SSH lại.

## 5. Cấu hình Jenkins Multibranch Pipeline

Tạo job:

```text
New Item -> Multibranch Pipeline
```

Tên gợi ý:

```text
eatzy-microservices-multibranch
```

Vào `Configure`.

### Branch Sources

Chọn:

```text
Add source -> GitHub
```

Cấu hình:

| Field | Giá trị |
|---|---|
| Credentials | GitHub credential đã tạo |
| Repository HTTPS URL | `https://github.com/hieuduong1810/eatzy_microservices` |

Bấm `Validate`. Nếu validate fail, kiểm tra token, username và repo URL.

### Behaviors

Khuyến nghị:

| Behavior | Giá trị |
|---|---|
| Discover branches | Exclude branches that are also filed as PRs |
| Discover pull requests from origin | The current pull request revision |

PR nên dùng để chạy CI/test. Production deploy chỉ chạy sau khi merge vào `main`.

### Build Configuration

| Field | Giá trị |
|---|---|
| Mode | by Jenkinsfile |
| Script Path | `Jenkinsfile` |

Lưu và bấm:

```text
Scan Repository Now
```

Log scan đúng phải có dạng:

```text
Connecting to https://api.github.com using <credential>
```

Nếu thấy anonymous access, Branch Source chưa chọn đúng GitHub credential.

## 6. Cấu hình ngrok và GitHub webhook

Nếu Jenkins local chạy ở:

```text
http://localhost:8080
```

chạy:

```powershell
ngrok http 8080
```

Ngrok sẽ cấp URL dạng:

```text
https://abc.ngrok-free.app
```

Trong Jenkins:

```text
Manage Jenkins -> System -> Jenkins URL
```

đặt:

```text
https://abc.ngrok-free.app/
```

Trong GitHub repo:

```text
Settings -> Webhooks -> Add webhook
```

Cấu hình:

| Field | Giá trị |
|---|---|
| Payload URL | `https://abc.ngrok-free.app/github-webhook/` |
| Content type | `application/json` |
| Which events | `Just the push event` |
| Active | Checked |

Vào `Recent Deliveries`, ping hoặc push event phải trả `200 OK`.

Nếu dùng ngrok free và URL đổi, phải cập nhật lại webhook.

## 7. Cách Jenkinsfile chạy CI/CD

### Mọi branch

Các branch như `feat/vu` chạy:

```text
Checkout
Test
Build Java Artifacts
```

Sau đó dừng. Đây là CI.

### Branch `main`

Khi push hoặc merge vào `main`, Jenkins chạy thêm:

```text
Build & Push Docker Images
Deploy
```

Docker image được push với 2 tag:

```text
latest
main-<short-commit>
```

Deploy stage copy:

```text
docker-compose.prod.yml
.env
```

rồi SSH vào server chạy:

```bash
cd /home/$SSH_USER/projects/eatzy-microservices
export DOCKERHUB_USER=$DOCKER_USER
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
docker image prune -f
```

## 8. Chạy thử

### Test CI branch feature

Push branch feature:

```bash
git push origin feat/vu
```

Log đúng:

```text
Stage "Build & Push Docker Images" skipped due to when conditional
Stage "Deploy" skipped due to when conditional
Finished: SUCCESS
```

### Test CD trên `main`

Merge PR vào `main` hoặc build branch `main` thủ công trong Jenkins.

Log deploy đúng có dạng:

```text
docker push <dockerhub-user>/eatzy-api-gateway:latest
scp ...
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
Container eatzy-api-gateway Started
Finished: SUCCESS
```

Kiểm tra trên server:

```bash
cd /home/hieu/projects/eatzy-microservices
docker compose -f docker-compose.prod.yml ps
docker logs eatzy-api-gateway --tail=100
```

## 9. Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| `open //./pipe/docker_engine` | Docker Desktop chưa chạy | Mở Docker Desktop, test `docker ps`, restart Jenkins nếu cần |
| `Docker login failed` | Sai Docker Hub token | Revoke token cũ, tạo token mới, cập nhật `dockerhub-credentials` |
| `COPY docker-artifacts/...jar: not found` | Windows batch không chạy tiếp sau `gradlew.bat` | Đảm bảo dùng `call gradlew.bat` trong Jenkinsfile |
| `WARNING: UNPROTECTED PRIVATE KEY FILE` | ACL SSH key tạm quá rộng trên Windows | Jenkinsfile đã copy key sang file tạm và chỉnh ACL bằng `icacls` |
| `Load key "...": Permission denied` | ACL key tạm bị khóa quá chặt | Cấp quyền read cho user chạy Jenkins và `SYSTEM` |
| `server-ssh-key not found` | Thiếu credential hoặc sai ID | Tạo credential đúng ID `server-ssh-key` |
| Webhook không trigger | Ngrok URL sai/hết hạn hoặc GitHub webhook sai | Cập nhật Payload URL và kiểm tra Recent Deliveries |
| Branch `main` không deploy | Build không chạy trên branch `main` hoặc stage trước fail | Kiểm tra `BRANCH_NAME`, Console Output và stage fail |

## 10. Checklist cuối

- [ ] Jenkins mở được tại local.
- [ ] Docker Desktop đang chạy và `docker ps` OK trên máy Jenkins.
- [ ] GitHub credential được chọn trong Branch Source.
- [ ] Credentials tồn tại: `dockerhub-credentials`, `dockerhub-user`, `server-ssh-key`, `server-ip`, `server-port`, `env-file`.
- [ ] SSH từ máy Jenkins vào server không hỏi password.
- [ ] Server có `/home/hieu/projects/eatzy-microservices`.
- [ ] Server có `eatzy.jks`.
- [ ] Ngrok đang chạy nếu Jenkins local.
- [ ] GitHub webhook trỏ tới `/github-webhook/` và trả `200`.
- [ ] Feature branch chỉ chạy CI.
- [ ] Merge vào `main` build/push image và deploy thành công.
