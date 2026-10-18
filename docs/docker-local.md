# Chạy Eatzy bằng Docker trên máy phát triển

Chạy từ thư mục root `eatzy-microservices`.

## Một lệnh trên Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

Script dùng Java 17 cho phiên chạy hiện tại, build 11 executable JAR, build image tuần tự trên Windows,
sau đó khởi động Compose local và chờ các healthcheck. Nó không push image hay truy cập server triển khai.

Nếu JDK 17 được cài ở vị trí khác:

```powershell
.\scripts\start-local.ps1 -JavaHome "C:\path\to\jdk-17"
```

Nếu image đã build và chưa đổi source:

```powershell
.\scripts\start-local.ps1 -SkipBuild
```

## Chạy từng bước

Cần `JAVA_HOME` trỏ tới Java 17 trước khi gọi Gradle 8.7:

```powershell
.\gradlew.bat prepareDockerArtifacts --parallel
docker compose build
docker compose up -d --no-build --wait --wait-timeout 300
docker compose ps
```

`docker compose up --build` chỉ build image theo Dockerfile, không tự chạy Gradle.
Phải tạo `docker-artifacts/*.jar` trước. Task `prepareDockerArtifacts` dùng output bootJar,
đổi tên thành `<service>.jar` và không lấy các file `*-plain.jar`.
Sau khi sửa source Java, chạy lại task này và build image.

## Dịch vụ và dữ liệu local

- `docker-compose.yml` là cấu hình dùng trên máy phát triển.
- MariaDB dùng bản đang có trên máy host, mặc định cổng 3307; không tạo DB container mới.
- Có thể đổi host/cổng local bằng `LOCAL_DB_HOST` và `LOCAL_DB_PORT` trong `.env`.
- Username/password database lấy từ `.env`.
- Redis local chạy trong Compose, dùng volume `eatzy-redis-data` và mật khẩu `REDIS_PASSWORD`.
- Các container dùng `redis:6379`, không dùng địa chỉ Redis remote trong `.env`.
- Redis không mở cổng ra máy host; các ứng dụng trong mạng Compose truy cập trực tiếp.
- Kafka/Zookeeper nằm trong Compose.
- Auth mount file `eatzy-auth-service/src/main/resources/eatzy.jks`.
  Có thể đổi bằng `JWT_KEYSTORE_FILE`; file phải tồn tại thật.
- Cấu hình JPA hiện tại có thể tạo/cập nhật schema trong database local khi service khởi động.

Các JVM local mặc định dùng heap tối đa 384 MB; có thể đổi qua `LOCAL_JAVA_TOOL_OPTIONS`.
Đây là cấu hình cho Docker local, không đổi JVM production.

## Production

`docker-compose.prod.yml` dùng địa chỉ DB/Redis trong `.env` của server.
Keystore mặc định là `./eatzy.jks` trên server hoặc đường dẫn `JWT_KEYSTORE_FILE` được cấu hình.

Jenkins đã chuẩn bị JAR/image trước khi deploy. Server thường chạy:

```sh
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --no-build
```

Nếu build source bằng file prod trên máy phát triển, vẫn phải chạy task tạo JAR,
cấu hình DB/Redis phù hợp và cung cấp đúng keystore trước.
`localhost` bên trong container không phải máy Windows host.

## Kiểm tra lỗi

```powershell
docker compose ps -a
docker compose logs --tail 100 <service>
docker compose logs --tail 100 eatzy-config-server
docker compose logs --tail 100 eatzy-auth-service
```

- Thiếu `docker-artifacts/...jar`: chạy `prepareDockerArtifacts`.
- Không có Java 17: cài JDK 17 hoặc dùng tham số `-JavaHome`.
- MariaDB connection refused: kiểm tra dịch vụ MariaDB host và cổng 3307.
- JWT keystore missing: kiểm tra file mount; Compose không tự tạo thư mục thay cho file bị thiếu.
- Redis unhealthy: kiểm tra container Redis và mật khẩu trong `.env`.
- Eureka/config server chưa sẵn sàng: các service phụ thuộc sẽ chờ healthcheck.
- Logstash không chạy có thể tạo cảnh báo; các service logging/monitoring hiện chưa được bật trong Compose.

`docker compose stop` dừng stack. Không dùng `down -v` nếu muốn giữ dữ liệu Redis local.
