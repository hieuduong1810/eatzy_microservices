# Jenkins CI/CD Setup Guide — Eatzy Microservices

> **Lưu ý:** Tài liệu này là bản setup Jenkins ban đầu. Với cấu hình hiện tại dùng Jenkins Multibranch Pipeline + GitHub Branch Source, nên đọc bản đầy đủ mới tại [Jenkins + GitHub Full Setup Guide](./jenkins-github-setup-full.md).

## Tổng quan

Pipeline Jenkins thay thế GitHub Actions, thực hiện 3 giai đoạn tự động khi push code:

```
Push code → GitHub → Webhook → Jenkins
                                  ↓
                               Test
                                  ↓
                          Build & Push Docker Images
                                  ↓
                      Deploy lên server (chỉ nhánh main)
```

---

## Yêu cầu

| Công cụ | Ghi chú |
|---|---|
| Jenkins 2.x | Đang chạy ở port 8080 |
| Java 17 | Để chạy Gradle build |
| Docker | Để build và push image |
| Git | Đã cài sẵn |

---

## Phần 1 — Cài đặt Jenkins

### Cài trên Ubuntu/WSL

```bash
# Thêm Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key \
  | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ \
  | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update && sudo apt install -y jenkins

# Khởi động Jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins
```

### Truy cập Jenkins

```
http://localhost:8080
```

Lấy password lần đầu:

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

---

## Phần 2 — Cài Plugins cần thiết

Vào **Manage Jenkins → Plugins → Available plugins**, tìm và cài:

| Plugin | Mục đích |
|---|---|
| `GitHub Integration Plugin` | Kết nối Jenkins với GitHub, nhận webhook |
| `Pipeline` | Hỗ trợ Jenkinsfile |
| `Docker Pipeline` | Build Docker trong pipeline |

Restart Jenkins sau khi cài xong.

---

## Phần 3 — Cấu hình Credentials

Vào **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**.

Cần thêm **6 credentials** sau:

### 3.1 Docker Hub

| Field | Giá trị |
|---|---|
| Kind | `Username with password` |
| Username | Docker Hub username |
| Password | Docker Hub access token |
| ID | `dockerhub-credentials` |

> Tạo Docker Hub token tại: Hub.docker.com → Account Settings → Personal access tokens

### 3.2 Docker Hub Username (riêng cho deploy script)

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | Docker Hub username |
| ID | `dockerhub-user` |

### 3.3 SSH Key vào server

| Field | Giá trị |
|---|---|
| Kind | `SSH Username with private key` |
| Username | Username SSH của server |
| Private Key | Nội dung file `~/.ssh/id_rsa` |
| ID | `server-ssh-key` |

### 3.4 IP Server

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | IP address của server |
| ID | `server-ip` |

### 3.5 SSH Port

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | SSH port (thường là `22`) |
| ID | `server-port` |

### 3.6 File .env

| Field | Giá trị |
|---|---|
| Kind | `Secret file` |
| File | Upload file `.env` của project |
| ID | `env-file` |

---

## Phần 4 — Kết nối Jenkins với GitHub

### 4.1 Tạo GitHub Personal Access Token

1. GitHub → click avatar → **Settings**
2. Kéo xuống **Developer settings → Personal access tokens → Tokens (classic)**
3. **Generate new token (classic)**
4. Chọn quyền: ✅ `repo`, ✅ `admin:repo_hook`
5. Copy token (chỉ hiện 1 lần)

### 4.2 Thêm token vào Jenkins

**Manage Jenkins → Credentials → Add Credentials:**

| Field | Giá trị |
|---|---|
| Kind | `Secret text` |
| Secret | GitHub token vừa tạo |
| ID | `github-token` |
| Description | `GitHub Token` |

### 4.3 Cấu hình GitHub Server

**Manage Jenkins → System → GitHub → Add GitHub Server:**

| Field | Giá trị |
|---|---|
| Name | `GitHub` |
| API URL | `https://api.github.com` |
| Credentials | chọn `github-token` |

Bấm **Test connection** → phải ra `Credentials verified for user ...`

Tick vào ✅ **Manage hooks** rồi bấm **Save**.

---

## Phần 5 — Setup Webhook

GitHub cần gọi được vào Jenkins để trigger pipeline. Có 2 trường hợp:

### Trường hợp A — Jenkins có public IP (VPS/cloud)

Vào repo GitHub → **Settings → Webhooks → Add webhook:**

| Field | Giá trị |
|---|---|
| Payload URL | `http://<IP-server>:8080/github-webhook/` |
| Content type | `application/json` |
| Which events | `Just the push event` |

### Trường hợp B — Jenkins chạy local (không có public IP)

Cần dùng **ngrok** để tạm thời expose Jenkins ra ngoài:

```bash
# Cài ngrok trên Ubuntu/WSL
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
  | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null \
  && echo "deb https://ngrok-agent.s3.amazonaws.com buster main" \
  | sudo tee /etc/apt/sources.list.d/ngrok.list \
  && sudo apt update && sudo apt install ngrok

# Đăng ký tài khoản tại ngrok.com, lấy authtoken rồi chạy:
ngrok config add-authtoken <your-authtoken>

# Expose Jenkins
ngrok http 8080
```

Ngrok sẽ cho ra URL dạng `https://abc123.ngrok-free.app`.  
Dùng URL đó làm Payload URL: `https://abc123.ngrok-free.app/github-webhook/`

> **Lưu ý:** URL ngrok thay đổi mỗi lần restart. Mỗi lần đổi phải cập nhật lại webhook trên GitHub.
> Tài khoản ngrok free bị giới hạn số lượng kết nối — chỉ nên dùng cho môi trường dev/học tập.

### Kiểm tra webhook

Sau khi tạo, GitHub tự gửi ping test:
- Vào **Settings → Webhooks** → click vào webhook → tab **Recent Deliveries**
- Dấu ✅ xanh = thành công
- Dấu ❌ đỏ = lỗi, xem chi tiết response để debug

---

## Phần 6 — Tạo Pipeline Job

1. Jenkins → **New Item**
2. Đặt tên: `eatzy-microservices`
3. Chọn **Pipeline** → OK
4. Phần **Build Triggers**: tick ✅ `GitHub hook trigger for GITScm polling`
5. Phần **Pipeline**:
   - Definition: `Pipeline script from SCM`
   - SCM: `Git`
   - Repository URL: URL repo GitHub của bạn
   - Credentials: thêm credentials GitHub nếu repo private
   - Branch: `*/main`
   - Script Path: `Jenkinsfile`
6. Bấm **Save**

---

## Phần 7 — Bảo mật Jenkins (quan trọng)

Nếu dùng ngrok hoặc Jenkins có thể truy cập từ internet, cần bật bảo mật:

**Manage Jenkins → Security:**

- ✅ **Security Realm:** Jenkins' own user database
- ✅ **Authorization:** Matrix-based security
- Thêm user của bạn với quyền **Administer**
- Anonymous users: không có quyền gì

Với cấu hình này, dù ai biết URL cũng phải đăng nhập mới vào được.

---

## Phần 8 — Cấu trúc Jenkinsfile

File `Jenkinsfile` ở root project gồm 4 stages:

```
Checkout → Test → Build & Push → Deploy (chỉ main)
```

| Stage | Mô tả |
|---|---|
| **Checkout** | Pull code từ Git, chmod +x gradlew |
| **Test** | Chạy `./gradlew test --parallel`, không block nếu fail |
| **Build & Push** | Build 12 Docker images song song, push lên Docker Hub |
| **Deploy** | SCP docker-compose + .env lên server, SSH restart containers |

> **Lưu ý:** Stage Deploy chỉ chạy trên nhánh `main`. Push lên nhánh khác chỉ chạy Test và Build.

---

## Phần 9 — Vận hành thường ngày

### Xem trạng thái build

```
http://localhost:8080/job/eatzy-microservices/
```

### Trigger thủ công

Jenkins → chọn job → **Build Now**

### Xem log build

Click vào build number → **Console Output**

### Khi ngrok URL thay đổi

```bash
# Chạy lại ngrok
ngrok http 8080
# Cập nhật Payload URL mới trên GitHub → Settings → Webhooks
```

---

## Xử lý lỗi thường gặp

| Lỗi | Nguyên nhân | Cách fix |
|---|---|---|
| `Permission denied` khi chạy gradlew | gradlew chưa có quyền execute | `chmod +x gradlew` |
| `JAVA_HOME is not set` | Chưa cài Java | `sudo apt install openjdk-17-jdk` |
| Webhook báo ❌ | Jenkins không nhận được request | Kiểm tra ngrok đang chạy, URL đúng chưa |
| Build fail ở stage Test | Test cần infrastructure (DB, Kafka) | Bình thường — stage dùng `catchError`, không block |
| `docker: command not found` | Jenkins không tìm thấy Docker | Thêm jenkins user vào docker group: `sudo usermod -aG docker jenkins` |
