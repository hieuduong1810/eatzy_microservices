# CI/CD Pipeline — Eatzy Microservices

> Tài liệu mô tả chi tiết luồng CI/CD của dự án, kèm phân tích về tính ổn định và chặt chẽ.
>
> **Công cụ:** GitHub Actions  
> **File workflow:** [`.github/workflows/cicd.yml`](../.github/workflows/cicd.yml)  
> **Cập nhật lần cuối:** 2026-05-24

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Sơ đồ luồng](#2-sơ-đồ-luồng)
3. [Chi tiết từng Stage](#3-chi-tiết-từng-stage)
4. [GitHub Secrets yêu cầu](#4-github-secrets-yêu-cầu)
5. [Docker Multi-Stage Build](#5-docker-multi-stage-build)
6. [Production Topology (docker-compose.prod.yml)](#6-production-topology)
7. [Monitoring Stack](#7-monitoring-stack)
8. [Phân tích: Điểm mạnh & Điểm yếu](#8-phân-tích-điểm-mạnh--điểm-yếu)
9. [Khuyến nghị cải thiện](#9-khuyến-nghị-cải-thiện)

---

## 1. Tổng quan

| Thuộc tính | Giá trị |
|---|---|
| CI/CD Engine | GitHub Actions |
| Trigger | `push` vào nhánh `main` |
| Số job | 1 (`build-and-deploy`) |
| Runner | `ubuntu-latest` |
| Số service build | 12 |
| Registry | Docker Hub |
| Deployment target | VPS/Server qua SSH |
| Orchestration | Docker Compose (`docker-compose.prod.yml`) |
| Timeout SSH | 30 phút |

Toàn bộ luồng CI và CD được gộp trong **một job duy nhất**, chạy tuần tự từ build đến deploy. Không có phân tách môi trường staging/production, không có bước chạy test tự động.

---

## 2. Sơ đồ luồng

```
git push → main
       │
       ▼
┌─────────────────────────────────────────────────────────────────┐
│  Job: build-and-deploy  (ubuntu-latest)                         │
│                                                                  │
│  Step 1: Checkout repository (actions/checkout@v4)              │
│       │                                                          │
│       ▼                                                          │
│  Step 2: Login to Docker Hub (docker/login-action@v3)           │
│       │                                                          │
│       ▼                                                          │
│  Step 3: Build & Push Docker images (bash loop, tuần tự)        │
│       │                                                          │
│       │  for SERVICE in [12 services]:                           │
│       │    docker build -t $USER/$SERVICE:latest ...             │
│       │    docker push $USER/$SERVICE:latest                     │
│       │                                                          │
│       ▼                                                          │
│  Step 4: SCP docker-compose.prod.yml → Server                   │
│       │  (appleboy/scp-action@v0.1.7)                           │
│       │                                                          │
│       ▼                                                          │
│  Step 5: SSH Deploy (appleboy/ssh-action@v1.0.3)                │
│       │  export DOCKERHUB_USER                                   │
│       │  docker compose -f docker-compose.prod.yml pull          │
│       │  docker compose -f docker-compose.prod.yml up -d         │
│       │  docker image prune -f                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Chi tiết từng Stage

### Stage 1 — Checkout repository

```yaml
- name: Checkout repository
  uses: actions/checkout@v4
```

Clone toàn bộ repository (bao gồm tất cả submodule nếu có) vào runner. Dùng action chính thức của GitHub, version cố định `v4`.

---

### Stage 2 — Login to Docker Hub

```yaml
- name: Login to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKERHUB_USER }}
    password: ${{ secrets.DOCKERHUB_TOKEN }}
```

Xác thực với Docker Hub dùng Personal Access Token (không dùng password). Credentials được lưu trong GitHub Secrets, không bị expose trong logs.

---

### Stage 3 — Build & Push Docker images

```yaml
- name: Build and Push Docker images
  run: |
    SERVICES=(
      "eatzy-discovery-server"
      "eatzy-config-server"
      "eatzy-api-gateway"
      "eatzy-auth-service"
      "eatzy-restaurant-service"
      "eatzy-order-service"
      "eatzy-communication-service"
      "eatzy-cart-service"
      "eatzy-payment-service"
      "eatzy-interaction-service"
      "eatzy-system-config-service"
      "eatzy-ai-service"
    )
    for SERVICE in "${SERVICES[@]}"; do
      docker build -t ${{ secrets.DOCKERHUB_USER }}/$SERVICE:latest \
        -f $SERVICE/Dockerfile .
      docker push ${{ secrets.DOCKERHUB_USER }}/$SERVICE:latest
    done
```

**12 services** được build và push **tuần tự** (không song song). Tất cả đều dùng:
- Tag: `latest` (không có versioning)
- Build context: root directory (`.`) — cho phép Gradle multi-project build hoạt động đúng
- Dockerfile: `$SERVICE/Dockerfile`

**Thứ tự build:** discovery-server → config-server → api-gateway → auth → restaurant → order → communication → cart → payment → interaction → system-config → ai-service

**Thời gian ước tính:** 30–60 phút tùy Gradle cache hit rate trên runner.

---

### Stage 4 — Copy docker-compose file lên server

```yaml
- name: Copy docker-compose files to server
  uses: appleboy/scp-action@v0.1.7
  with:
    host: ${{ secrets.SERVER_IP }}
    username: ${{ secrets.SERVER_USER }}
    key: ${{ secrets.SERVER_SSH_KEY }}
    port: ${{ secrets.SERVER_SSH_PORT }}
    source: "docker-compose.prod.yml"
    target: "/home/${{ secrets.SERVER_USER }}/projects/eatzy-microservices"
    overwrite: true
```

Chỉ copy **duy nhất file** `docker-compose.prod.yml` lên server. Các file khác (`monitoring/`, `.env`, `eatzy.jks`) phải tồn tại sẵn trên server — không được quản lý bởi pipeline này.

---

### Stage 5 — Deploy qua SSH

```yaml
- name: Deploy to server via SSH
  uses: appleboy/ssh-action@v1.0.3
  with:
    host: ${{ secrets.SERVER_IP }}
    username: ${{ secrets.SERVER_USER }}
    key: ${{ secrets.SERVER_SSH_KEY }}
    port: ${{ secrets.SERVER_SSH_PORT }}
    command_timeout: 30m
    script: |
      cd /home/${{ secrets.SERVER_USER }}/projects/eatzy-microservices
      export DOCKERHUB_USER=${{ secrets.DOCKERHUB_USER }}
      docker compose -f docker-compose.prod.yml pull
      docker compose -f docker-compose.prod.yml up -d
      docker image prune -f
```

Ba bước trong SSH session:

| Lệnh | Mục đích |
|---|---|
| `docker compose pull` | Kéo tất cả image `:latest` mới nhất từ Docker Hub |
| `docker compose up -d` | Khởi động lại các container có image mới (rolling update không thực sự, chỉ recreate) |
| `docker image prune -f` | Xóa dangling images để giải phóng disk |

**Lưu ý:** `up -d` sẽ recreate container nếu image thay đổi. Trong thời gian recreate, service **có downtime** (không có zero-downtime deployment).

---

## 4. GitHub Secrets yêu cầu

| Secret | Mục đích | Ghi chú |
|---|---|---|
| `DOCKERHUB_USER` | Username Docker Hub | Dùng làm prefix image name |
| `DOCKERHUB_TOKEN` | Personal Access Token Docker Hub | Không dùng password |
| `SERVER_IP` | IP hoặc hostname của server production | |
| `SERVER_USER` | Username SSH trên server | Dùng cả trong path `/home/$SERVER_USER/...` |
| `SERVER_SSH_KEY` | Private key SSH (PEM format) | |
| `SERVER_SSH_PORT` | Port SSH (thường 22) | |

**Bắt buộc cấu hình trên server trước khi pipeline chạy lần đầu:**
- Thư mục `/home/$SERVER_USER/projects/eatzy-microservices/` phải tồn tại
- File `.env` với các biến môi trường production phải có sẵn trong thư mục đó
- File `eatzy.jks` (JWT keystore) phải có sẵn tại thư mục gốc của project trên server

---

## 5. Docker Multi-Stage Build

### Pattern cho Java services (11 services)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :SERVICE_NAME:bootJar -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/SERVICE_NAME/build/libs/*.jar app.jar
EXPOSE PORT
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Ưu điểm của multi-stage:**
- Stage 2 chỉ chứa JRE (không có JDK, source code, Gradle cache) → image nhỏ hơn đáng kể
- `--mount=type=cache,target=/root/.gradle` tận dụng BuildKit cache → giảm thời gian build lần sau (nhưng cache bị reset mỗi lần runner khởi động mới)

### Pattern cho Python AI service

```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8089
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8089"]
```

### Mapping service → image → port

| Service | Docker Image | Port |
|---|---|---|
| eatzy-discovery-server | `$USER/eatzy-discovery-server:latest` | 8761 |
| eatzy-config-server | `$USER/eatzy-config-server:latest` | 8888 |
| eatzy-api-gateway | `$USER/eatzy-api-gateway:latest` | 8080 |
| eatzy-auth-service | `$USER/eatzy-auth-service:latest` | 8081 |
| eatzy-restaurant-service | `$USER/eatzy-restaurant-service:latest` | 8082 |
| eatzy-order-service | `$USER/eatzy-order-service:latest` | 8083 |
| eatzy-communication-service | `$USER/eatzy-communication-service:latest` | 8084 |
| eatzy-cart-service | `$USER/eatzy-cart-service:latest` | 8085 |
| eatzy-payment-service | `$USER/eatzy-payment-service:latest` | 8086 |
| eatzy-interaction-service | `$USER/eatzy-interaction-service:latest` | 8087 |
| eatzy-system-config-service | `$USER/eatzy-system-config-service:latest` | 8088 |
| eatzy-ai-service | `$USER/eatzy-ai-service:latest` | 8089 |

---

## 6. Production Topology

File: [`docker-compose.prod.yml`](../docker-compose.prod.yml)

### Dependency graph

```
zookeeper
    └── kafka
            └── eatzy-discovery-server (healthcheck)
                    ├── eatzy-config-server
                    │       ├── eatzy-api-gateway
                    │       ├── eatzy-auth-service ────── kafka
                    │       ├── eatzy-restaurant-service ─ kafka
                    │       ├── eatzy-order-service ────── kafka
                    │       ├── eatzy-communication-service kafka
                    │       ├── eatzy-cart-service
                    │       ├── eatzy-payment-service
                    │       ├── eatzy-interaction-service ─ kafka
                    │       ├── eatzy-system-config-service
                    │       └── eatzy-ai-service (condition: service_healthy)
```

### Healthcheck

Chỉ `eatzy-discovery-server` có healthcheck được cấu hình:

```yaml
healthcheck:
  test: ["CMD", "wget", "--spider", "-q", "http://localhost:8761/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 240s   # 4 phút chờ Spring Boot khởi động
```

`eatzy-ai-service` là service duy nhất dùng `condition: service_healthy` — chờ discovery-server thực sự healthy mới start.

### Environment variables injection

Tất cả service business dùng YAML anchor `x-env-mapping`:

```yaml
x-env-mapping: &env-mapping
  EUREKA_URL: http://eatzy-discovery-server:8761/eureka/
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eatzy-discovery-server:8761/eureka/
  DB_HOST: ${DB_HOST}
  DB_PORT: ${DB_PORT}
  DB_USERNAME: ${DB_USERNAME}
  DB_PASSWORD: ${DB_PASSWORD}
  REDIS_HOST: ${REDIS_HOST}
  REDIS_PORT: ${REDIS_PORT}
  REDIS_PASSWORD: ${REDIS_PASSWORD}
  KAFKA_BOOTSTRAP_SERVERS: kafka:29092
  SPRING_CONFIG_IMPORT: optional:configserver:http://eatzy-config-server:8888
  JWT_KEYSTORE_PATH: file:/app/eatzy.jks
```

Các biến `${VAR}` được đọc từ file `.env` trên server (không được quản lý bởi pipeline).

### Chú ý về volumes

`eatzy-auth-service` mount JWT keystore từ server:
```yaml
volumes:
  - ./eatzy.jks:/app/eatzy.jks:ro
```

File này **phải tồn tại sẵn** tại `/home/$SERVER_USER/projects/eatzy-microservices/eatzy.jks` trên server. Pipeline không tự động copy file này.

### Restart policy

Tất cả service đều có `restart: always` — container sẽ tự restart khi crash hoặc khi Docker daemon khởi động lại.

---

## 7. Monitoring Stack

Cấu hình monitoring đã được viết sẵn nhưng **toàn bộ bị comment out** trong `docker-compose.prod.yml`.

### Các thành phần đã cấu hình (chưa active)

| Thành phần | Image | Port | Mục đích |
|---|---|---|---|
| Prometheus | `prom/prometheus:latest` | 9090 | Thu thập metrics từ `/actuator/prometheus` mỗi 15s |
| Alertmanager | `prom/alertmanager:latest` | 9093 | Gửi alert qua email (Mailtrap SMTP) |
| Grafana | `grafana/grafana:latest` | 3000 | Dashboard visualization |
| Elasticsearch | `docker.elastic.co/elasticsearch:8.17.3` | 9200 | Log aggregation |
| Kibana | `docker.elastic.co/kibana:8.17.3` | 5601 | Log visualization |
| Logstash | `docker.elastic.co/logstash:8.17.3` | 5000 | Log ingestion từ services |
| Zipkin | `openzipkin/zipkin` | 9411 | Distributed tracing |
| Node Exporter | `prom/node-exporter:latest` | 9100 | System metrics |
| cAdvisor | `zcube/cadvisor:latest` | 8090 | Container metrics |

### Alert rules đã định nghĩa

File: [`monitoring/prometheus/alert.rules.yml`](../monitoring/prometheus/alert.rules.yml)

| Alert | Điều kiện | Severity |
|---|---|---|
| `HighErrorRate` | HTTP 5xx > 2 req/phút | warning |
| `InstanceDown` | Service offline > 2 phút | critical |
| `HighRequestRate` | Requests > 100 req/phút | warning |
| `HighCPUUsage` | CPU usage > 80% | warning |

### Alert routing

Alertmanager gửi email qua **Mailtrap sandbox** (không phải email thật) đến `hieuduong181005@gmail.com`.  
Hiện tại đây là môi trường test SMTP — không hoạt động trong production thực.

### Application-level observability (đã active)

Dù monitoring stack bị comment out, các service vẫn expose metrics thông qua:
- `spring-boot-actuator` — `/actuator/health`, `/actuator/info`
- `micrometer-registry-prometheus` — `/actuator/prometheus`
- `micrometer-tracing-bridge-brave` — distributed tracing headers
- `logstash-logback-encoder` — structured JSON logging (sẵn sàng cho ELK)

---

## 8. Phân tích: Điểm mạnh & Điểm yếu

### Điểm mạnh

| # | Điểm mạnh | Giải thích |
|---|---|---|
| 1 | **Multi-stage Docker build** | Tách biệt build và runtime, image production nhỏ gọn, không chứa source code hay JDK |
| 2 | **Secrets quản lý qua GitHub Secrets** | Credentials không bị hardcode trong code, không bị lộ trong git history |
| 3 | **`restart: always` trên toàn bộ service** | Service tự khôi phục sau crash mà không cần can thiệp thủ công |
| 4 | **Healthcheck trên Discovery Server** | Service registry có health gate — ai-service chờ healthcheck pass mới start |
| 5 | **`image prune -f` sau deploy** | Dọn dẹp dangling images tự động, tránh cạn disk theo thời gian |
| 6 | **Gradle cache mount trong Dockerfile** | `--mount=type=cache` giảm thời gian build khi cache hit |
| 7 | **YAML anchor `x-env-mapping`** | DRY — environment vars định nghĩa một lần, tái sử dụng cho tất cả service |
| 8 | **Observability được cài sẵn** | Actuator + Prometheus metrics + Brave tracing đã được wiring vào tất cả service qua root `build.gradle.kts` |
| 9 | **`command_timeout: 30m` cho SSH** | Tránh job bị treo vô thời hạn khi pull image chậm |
| 10 | **Centralized config server** | Cấu hình tập trung, service chỉ cần biết địa chỉ config server |

---

### Điểm yếu & Rủi ro

#### Rủi ro cao

**1. Không có bước chạy test**

Pipeline không có bất kỳ bước nào chạy unit test, integration test, hay lint. Build thành công không đồng nghĩa code đúng. Một commit lỗi sẽ được deploy thẳng lên production.

```
# Hiện tại:     push → build → deploy
# Nên có:       push → test → build → deploy
```

**2. Không có versioning — chỉ dùng tag `latest`**

Tất cả 12 service đều chỉ có một tag duy nhất là `latest`. Hậu quả:
- Không thể rollback về version cụ thể — `docker compose up` luôn dùng image mới nhất
- Không theo dõi được lịch sử deploy
- Nếu image mới bị lỗi, không có cơ chế tự động rollback

**3. Downtime khi deploy**

`docker compose up -d` recreate container với image mới → service có downtime trong thời gian khởi động lại. Không có zero-downtime deployment (blue/green, rolling update).

**4. Monitoring stack bị comment out**

Production đang chạy mù — không có Prometheus, không có Alertmanager, không có Grafana. Alert rules đã viết sẵn nhưng không hoạt động. Sự cố sẽ không được phát hiện tự động.

---

#### Rủi ro trung bình

**5. Build 12 service tuần tự — pipeline cực chậm**

Mỗi service Java mất 5–10 phút để build từ đầu (Gradle cache trên GitHub Actions runner không được giữ lại giữa các run). Tổng thời gian: **30–60+ phút** mỗi lần push vào main.

**6. `depends_on` không đảm bảo service sẵn sàng**

```yaml
depends_on:
  - eatzy-discovery-server
  - eatzy-config-server
```

`depends_on` chỉ đảm bảo container được *khởi động*, không đảm bảo Spring Boot bên trong đã *ready*. Các service business có thể fail khi startup vì config server hay discovery server chưa kịp sẵn sàng. Chỉ `eatzy-ai-service` dùng `condition: service_healthy`.

**7. JWT keystore phải tồn tại sẵn trên server**

```yaml
volumes:
  - ./eatzy.jks:/app/eatzy.jks:ro
```

File `eatzy.jks` không được quản lý bởi pipeline. Nếu file không có hoặc bị xóa trên server, `eatzy-auth-service` crash khi start — không có bảo vệ hay thông báo.

**8. `.env` file phải tồn tại sẵn trên server**

Pipeline không copy hay quản lý file `.env`. Nếu biến môi trường thiếu, service khởi động với config sai hoặc crash.

**9. Config server dùng file-based native profile**

Configs được bundle vào Docker image tại build time. Thay đổi config yêu cầu rebuild và redeploy toàn bộ.

---

#### Rủi ro thấp / Cải tiến kỹ thuật

**10. Kafka single broker, replication factor = 1**

```yaml
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

Single point of failure. Nếu Kafka container crash, toàn bộ event-driven flow (order, communication, auth events) ngừng hoạt động.

**11. Kafka và Zookeeper không có persistent volumes**

Không có `volumes:` cho Kafka và Zookeeper trong `docker-compose.prod.yml`. Khi container restart, toàn bộ message queue và offset data bị mất.

**12. DOCKERHUB_USER export trong SSH script**

```bash
export DOCKERHUB_USER=${{ secrets.DOCKERHUB_USER }}
```

Biến này được expand tại runtime của GitHub Actions runner trước khi gửi script qua SSH, nên giá trị thực của secret có thể xuất hiện trong SSH command logs của server.

**13. Không có staging environment**

Không có môi trường intermediate để test trước khi deploy production. Mọi thay đổi trên `main` đều đi thẳng lên production.

**14. `eatzy-config-server` bị comment out phần `depends_on: eatzy-discovery-server`**

Config server không chờ discovery server healthy → race condition khi khởi động cold start.

---

## 9. Khuyến nghị cải thiện

### Ưu tiên cao

```yaml
# 1. Tách CI và CD thành 2 jobs riêng
jobs:
  test:           # CI: chạy test trước
    steps:
      - ./gradlew test
  build-and-push: # Build sau khi test pass
    needs: test
  deploy:         # Deploy sau khi build xong
    needs: build-and-push
```

```yaml
# 2. Dùng git SHA làm image tag
IMAGE_TAG: ${{ github.sha }}
# Push cả 2 tag: latest và SHA
docker push $USER/$SERVICE:${{ github.sha }}
docker push $USER/$SERVICE:latest
```

```yaml
# 3. Build song song (matrix strategy)
strategy:
  matrix:
    service: [eatzy-discovery-server, eatzy-config-server, ...]
```

### Ưu tiên trung bình

- Bật monitoring stack (uncomment Prometheus + Alertmanager)
- Đổi SMTP Mailtrap sang email provider thực (SendGrid, SES)
- Thêm `condition: service_healthy` cho tất cả service, không chỉ ai-service
- Thêm persistent volumes cho Kafka và Zookeeper
- Thêm smoke test sau deploy (health check endpoint)

### Ưu tiên thấp

- Thêm staging environment (nhánh `develop` → staging, nhánh `main` → production)
- Chuyển config server sang Git-backed profile thay vì file-based native
- Cân nhắc Kubernetes/Helm nếu scale up

---

*Tài liệu này được tạo tự động từ phân tích codebase. Cập nhật khi có thay đổi trong `.github/workflows/cicd.yml` hoặc `docker-compose.prod.yml`.*
