# Jenkins + GitHub Full Setup Guide

Tài liệu này hướng dẫn setup Jenkins Multibranch Pipeline với GitHub cho dự án Eatzy Microservices từ đầu đến cuối.

Mục tiêu:

- GitHub push trigger Jenkins.
- Jenkins scan branch/PR.
- Jenkins checkout code bằng credential GitHub.
- Jenkins chạy CI cho mọi branch: checkout, test, build Java artifacts.
- Jenkins chỉ build/push Docker images và deploy khi branch là `main`.

## 1. Kiến trúc tổng quan

```text
Developer
    |
    | git push
    v
GitHub repository
    |
    | webhook: /github-webhook/
    v
Jenkins Multibranch Pipeline
    |
    | checkout branch
    v
Gradle test/build
    |
    +--> feature branch: dừng
    |
    +--> main branch:
            Docker build/push
            |
            v
            deploy qua SSH
```

## 2. Yêu cầu trên máy Jenkins

### Bắt buộc

| Thành phần | Yêu cầu |
|---|---|
| Jenkins | 2.x |
| Java | JDK 17 |
| Git | Có `git.exe` trong PATH |
| Docker Desktop | Đang chạy và CLI dùng được |
| OpenSSH client | Có `ssh` và `scp` trong PATH |
| Gradle wrapper | Dùng file `gradlew`/`gradlew.bat` trong repo |
| Internet | Truy cập GitHub, Docker Hub, Maven Central |

### Kiểm tra nhanh trên Windows

Mở PowerShell trên máy Jenkins:

```powershell
java -version
git --version
docker info
docker compose version
ssh -V
```

Kỳ vọng:

- `java -version` là Java 17.
- `git --version` trả về Git version.
- `docker info` không lỗi.
- Docker context thường là `desktop-linux`.
- `ssh -V` trả về OpenSSH version.

## 3. Cài Jenkins trên Windows

1. Tải Jenkins Windows installer từ trang Jenkins.
2. Cài Jenkins như service.
3. Chọn port, thường là `8080`.
4. Mở Jenkins:

```text
http://localhost:8080
```

5. Lấy initial admin password:

```text
C:\ProgramData\Jenkins\.jenkins\secrets\initialAdminPassword
```

6. Tạo admin user.

## 4. Cài plugins Jenkins

Vào:

```text
Manage Jenkins -> Plugins -> Available plugins
```

Cài các plugin sau:

| Plugin | Mục đích |
|---|---|
| Pipeline | Chạy Jenkinsfile |
| Git | Checkout source |
| GitHub | Tích hợp GitHub webhook/API |
| GitHub Branch Source | Multibranch Pipeline với GitHub |
| Credentials Binding | Đưa credentials vào pipeline |
| SSH Credentials | SSH private key credential |
| Docker Pipeline | Hỗ trợ Docker trong pipeline |
| JUnit | Publish test result |

Restart Jenkins sau khi cài plugin nếu Jenkins yêu cầu.

## 5. Tạo GitHub Personal Access Token

Vào GitHub:

```text
GitHub -> Settings -> Developer settings -> Personal access tokens
```

Nên tạo Fine-grained token nếu có thể.

Quyền tối thiểu cho public repo:

- Read access repository metadata.
- Read access contents.
- Webhook management nếu Jenkins quản lý hook tự động.

Nếu dùng classic token:

- Repo public: `public_repo`
- Repo private: `repo`
- Nếu Jenkins quản lý webhook: `admin:repo_hook`

Copy token ngay lúc tạo, vì GitHub chỉ hiện một lần.

## 6. Tạo GitHub credential trong Jenkins

Vào:

```text
Manage Jenkins -> Credentials -> System -> Global credentials -> Add Credentials
```

Tạo credential:

| Field | Giá trị |
|---|---|
| Kind | `Username with password` |
| Username | GitHub username |
| Password | GitHub Personal Access Token |
| ID | ví dụ `github` hoặc `githubtokens` |
| Description | `GitHub token for Eatzy Jenkins` |

Lý do dùng `Username with password`:

- Branch Source GitHub UI thường hiện credential loại này.
- `Secret text` có thể không hiện trong dropdown Branch Sources tùy plugin/version.

Sau khi dùng thành công, log checkout sẽ có:

```text
using credential github
using GIT_ASKPASS to set credentials ...
```

## 7. Tạo Docker Hub credential

Tạo Docker Hub access token tại Docker Hub:

```text
Docker Hub -> Account Settings -> Personal access tokens
```

Trong Jenkins, tạo credential:

| Field | Giá trị |
|---|---|
| Kind | `Username with password` |
| Username | Docker Hub username |
| Password | Docker Hub access token |
| ID | `dockerhub-credentials` |
| Description | `Docker Hub credentials` |

Pipeline dùng credential này để:

```text
docker login
docker push
```

Với production hiện tại, Docker Hub username phải là namespace mà server pull image từ đó. Ví dụ nếu server đang chạy image dạng:

```text
duonghieu1810/eatzy-api-gateway:latest
```

thì `dockerhub-credentials.Username` và credential `dockerhub-user` bên dưới cũng phải là `duonghieu1810`.

## 8. Tạo deploy credentials

Chỉ cần nếu muốn deploy branch `main`.

### 8.1 SSH private key

| Field | Giá trị |
|---|---|
| Kind | `SSH Username with private key` |
| Username | SSH user trên server |
| Private Key | Private key SSH |
| ID | `server-ssh-key` |

Test trước trên máy Jenkins:

```powershell
ssh -i C:\Users\<windows-user>\.ssh\jenkins-deploy-key -p <port> <ssh-user>@<server-host>
```

Ví dụ production hiện tại:

```powershell
ssh -i C:\Users\ADMIN\.ssh\jenkins-deploy-key -p 38283 hieu@hieussh.hoanduong.net
```

### 8.2 Server IP

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | IP/hostname server, ví dụ `hieussh.hoanduong.net` |
| ID | `server-ip` |

### 8.3 Server SSH port

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | SSH port, ví dụ `38283` |
| ID | `server-port` |

### 8.4 Docker Hub username cho deploy

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | Docker Hub username/namespace dùng trong `docker-compose.prod.yml` |
| ID | `dockerhub-user` |

### 8.5 File `.env`

| Field | Giá trị |
|---|---|
| Kind | `Secret file` |
| File | File `.env` production |
| ID | `env-file` |

File này sẽ được Jenkins copy lên server thành:

```text
/home/<ssh-user>/projects/eatzy-microservices/.env
```

Không commit `.env` production lên GitHub.

## 9. Expose Jenkins ra internet

GitHub webhook cần gọi được Jenkins.

Nếu Jenkins chạy local, dùng ngrok.

### 9.1 Ngrok reserved domain

Ví dụ domain:

```text
https://pavilion-legume-stunt.ngrok-free.dev
```

Chạy:

```powershell
ngrok http --url=pavilion-legume-stunt.ngrok-free.dev 8080
```

Hoặc tùy cấu hình ngrok của bạn.

Webhook URL sẽ là:

```text
https://pavilion-legume-stunt.ngrok-free.dev/github-webhook/
```

Bắt buộc có dấu `/` cuối.

## 10. Tạo GitHub webhook

Vào repository GitHub:

```text
Settings -> Webhooks -> Add webhook
```

Cấu hình:

| Field | Giá trị |
|---|---|
| Payload URL | `https://<jenkins-public-url>/github-webhook/` |
| Content type | `application/json` |
| Secret | Có thể để trống, hoặc cấu hình secret nếu Jenkins dùng |
| SSL verification | Enable |
| Which events | Just the push event |
| Active | Checked |

Sau khi tạo, vào tab `Recent Deliveries`.

Thành công khi GitHub ping trả:

```text
200 OK
```

## 11. Tạo Multibranch Pipeline job

Trong Jenkins:

```text
New Item -> Multibranch Pipeline
```

Đặt tên:

```text
eatzy-microservices-multibranch
```

### Branch Sources

Thêm source `GitHub`.

Cấu hình:

| Field | Giá trị |
|---|---|
| Credentials | GitHub credential đã tạo, ví dụ `github` |
| Repository HTTPS URL | `https://github.com/hieuduong1810/eatzy_microservices` |

Bấm `Validate`.

Nếu validate fail, kiểm tra:

- Token có đúng không.
- Username có đúng không.
- Repo URL có đúng không.
- Token có quyền đọc repo không.

### Behaviors

Nên bật:

| Behavior | Strategy |
|---|---|
| Discover branches | Exclude branches that are also filed as PRs |
| Discover pull requests from origin | The current pull request revision |
| Discover pull requests from forks | Tùy nhu cầu, cẩn thận với repo public |

### Build Configuration

| Field | Giá trị |
|---|---|
| Mode | by Jenkinsfile |
| Script Path | `Jenkinsfile` |

### Scan Repository Triggers

Bật nếu muốn Jenkins scan định kỳ:

```text
Periodically if not otherwise run
```

Vì webhook đã có, có thể để interval vừa phải, ví dụ 1 day, để backup.

Không nên scan quá dày nếu GitHub API quota thấp.

## 12. Lưu và scan

Bấm:

```text
Save
```

Sau đó:

```text
Scan Multibranch Pipeline Now
```

Log đúng:

```text
Connecting to https://api.github.com using ...
```

Nếu thấy:

```text
Connecting to https://api.github.com with no credentials, anonymous access
```

thì Jenkins chưa dùng GitHub credential cho Branch Source.

Cần:

1. Abort scan cũ nếu đang sleep.
2. Vào Configure.
3. Chọn credential GitHub.
4. Validate.
5. Save.
6. Scan lại.

## 13. Cấu hình GitHub API rate limit trong Jenkins

Nếu log có:

```text
Jenkins-Imposed API Limiter
Sleeping for ...
```

Vào:

```text
Manage Jenkins -> System -> GitHub API usage
```

Chọn strategy ít bảo thủ hơn, ví dụ chỉ throttle khi gần chạm GitHub API rate limit.

Nhưng việc quan trọng nhất vẫn là gắn GitHub credential cho Branch Source.

Anonymous GitHub API có quota thấp hơn nhiều.

## 14. Bỏ trigger kép

Với Multibranch Pipeline, không cần block này trong `Jenkinsfile`:

```groovy
triggers {
    githubPush()
}
```

Nếu có block đó, có thể thấy:

```text
Started by GitHub push
Started by GitHub push
```

Multibranch đã được trigger qua webhook/branch indexing, nên nên bỏ block trigger trong Jenkinsfile.

## 15. Kiểm tra build branch

Push lên branch:

```bash
git push origin feat/vu
```

Jenkins log đúng:

```text
Push event to branch feat/vu
Checking out Revision <commit> (feat/vu)
```

Docker build/push và deploy phải bị skip:

```text
Stage "Build & Push Docker Images" skipped due to when conditional
Stage "Deploy" skipped
```

Với pipeline đã tối ưu build artifact trước Docker, log đúng còn có:

```text
Stage "Build Java Artifacts"
BUILD SUCCESSFUL
1 file(s) copied.
Pipeline succeeded on branch feat/vu
Finished: SUCCESS
```

Nếu thấy `1 file(s) copied.` lặp lại 11 lần, nghĩa là Jenkins đã copy đủ jar cho 11 Java services.

## 16. Kiểm tra build main

Push lên `main`:

```bash
git push origin main
```

Image tag đúng:

```text
<dockerhub-user>/eatzy-auth-service:latest
<dockerhub-user>/eatzy-auth-service:main-<commit>
```

Deploy sẽ chạy nếu đã có đầy đủ credentials:

```text
dockerhub-credentials
server-ssh-key
server-ip
server-port
dockerhub-user
env-file
```

## 17. Chuẩn bị server deploy

Trên server production, tạo thư mục:

```bash
mkdir -p /home/<user>/projects/eatzy-microservices
```

Server cần có:

- Docker Engine
- Docker Compose plugin
- Quyền user được chạy Docker
- Network/firewall mở các port cần thiết
- File `eatzy.jks` nếu service auth đang mount `./eatzy.jks:/app/eatzy.jks:ro`

Pipeline sẽ copy:

- `docker-compose.prod.yml`
- `.env`

Sau đó chạy:

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Với server hiện tại, đường dẫn deploy là:

```text
/home/hieu/projects/eatzy-microservices
```

Kiểm tra nhanh:

```bash
cd /home/hieu/projects/eatzy-microservices
docker ps
docker compose version
ls -la eatzy.jks
```

## 18. Troubleshooting

### Jenkins vẫn anonymous access

Triệu chứng:

```text
Connecting to https://api.github.com with no credentials, anonymous access
```

Cách xử lý:

1. Abort scan đang chạy.
2. Chọn credential trong Branch Sources.
3. Bấm Validate.
4. Bấm Save.
5. Scan lại.

### Credential GitHub không hiện trong dropdown

Tạo lại credential với:

```text
Kind: Username with password
Username: GitHub username
Password: GitHub PAT
```

Không dùng `Secret text` nếu plugin không hiện credential đó.

### Docker Desktop lỗi `input/output error`

Chạy trên PowerShell:

```powershell
wsl --shutdown
```

Restart Docker Desktop.

Kiểm tra:

```powershell
docker info
docker system df
```

Dọn cache:

```powershell
docker builder prune -af
```

Nếu cần dọn mạnh hơn:

```powershell
docker system prune -af
```

Lưu ý: prune sẽ xóa cache/images/containers không dùng.

### Docker daemon không chạy trên Jenkins Windows

Triệu chứng:

```text
error during connect: this error may indicate that the docker daemon is not running
open //./pipe/docker_engine
```

Cách xử lý:

1. Mở Docker Desktop.
2. Chờ Docker Engine running.
3. Test trong PowerShell trên máy Jenkins:

```powershell
docker ps
```

Nếu Jenkins chạy dạng Windows service, restart service sau khi Docker Desktop đã chạy:

```powershell
Restart-Service Jenkins
```

### Windows OpenSSH báo private key permission

Triệu chứng:

```text
WARNING: UNPROTECTED PRIVATE KEY FILE
Load key "...": bad permissions
Load key "...": Permission denied
```

Nguyên nhân là file private key tạm do Jenkins tạo có ACL không phù hợp với Windows OpenSSH.

`Jenkinsfile` hiện tại xử lý bằng cách copy key sang workspace, tắt inheritance và cấp quyền đọc cho user đang chạy Jenkins:

```bat
set "SSH_KEY_SAFE=%WORKSPACE%\.jenkins-server-ssh-key-%BUILD_NUMBER%"
copy /Y "%SSH_KEY%" "%SSH_KEY_SAFE%" >nul
for /f "delims=" %%U in ('whoami') do set "CURRENT_USER=%%U"
icacls "%SSH_KEY_SAFE%" /inheritance:r
icacls "%SSH_KEY_SAFE%" /grant:r "%CURRENT_USER%:R" "*S-1-5-18:R"
```

### Gradle wrapper zip corrupt

Triệu chứng:

```text
zip END header not found
```

Nguyên nhân thường là Docker BuildKit cache bị corrupt.

Pipeline mới tránh lỗi này bằng cách build jar trên Jenkins agent, không tải Gradle wrapper trong từng Dockerfile Java nữa.

### Docker không tìm thấy `docker-artifacts/*.jar`

Triệu chứng:

```text
COPY docker-artifacts/eatzy-discovery-server.jar app.jar
not found
```

Nguyên nhân thường gặp trên Windows:

- Stage `Build Java Artifacts` gọi `gradlew.bat` mà không dùng `call`.
- Batch script dừng sau khi Gradle chạy xong.
- Lệnh `mkdir docker-artifacts` và `copy /Y ...` không được thực thi.

Cách đúng trong Jenkinsfile:

```bat
call gradlew.bat :eatzy-discovery-server:bootJar ... --parallel -x test
mkdir docker-artifacts
```

Sau khi sửa đúng, log sẽ có:

```text
1 file(s) copied.
```

và Docker build sẽ đi qua bước:

```text
COPY docker-artifacts/eatzy-discovery-server.jar app.jar
DONE
```

### Deploy fail `server-ssh-key` not found

Tạo credential ID đúng:

```text
server-ssh-key
```

Loại:

```text
SSH Username with private key
```

### Jenkins không trigger khi push

Kiểm tra:

1. Ngrok/Jenkins public URL còn sống không.
2. GitHub webhook Payload URL đúng `/github-webhook/` không.
3. GitHub Recent Deliveries có `200 OK` không.
4. Jenkins Multibranch job có Branch Source đúng repo không.
5. Jenkins log có nhận `Push event to branch ...` không.

## 19. Checklist hoàn chỉnh

Trước khi coi setup đã xong, kiểm tra tất cả:

- [ ] Jenkins mở được qua browser.
- [ ] Java 17 đúng trong Jenkins.
- [ ] Git dùng được trong Jenkins.
- [ ] Docker CLI dùng được trong Jenkins.
- [ ] GitHub credential cho Branch Source tồn tại.
- [ ] Docker Hub credential `dockerhub-credentials` tồn tại.
- [ ] Docker Hub username secret `dockerhub-user` tồn tại và trùng namespace image production.
- [ ] Deploy credentials `server-ssh-key`, `server-ip`, `server-port`, `env-file` tồn tại.
- [ ] Branch Source chọn GitHub credential, không để `none`.
- [ ] GitHub webhook trỏ về `/github-webhook/`.
- [ ] Webhook recent delivery trả `200`.
- [ ] Multibranch scan không còn anonymous access.
- [ ] Push branch feature trigger Jenkins.
- [ ] Branch feature chỉ chạy test và build Java artifacts.
- [ ] Branch feature không deploy.
- [ ] Push hoặc merge vào `main` tạo image `latest` và `main-<commit>`.
- [ ] Push hoặc merge vào `main` deploy thành công lên server.
