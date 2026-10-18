# CI/CD của main — GitHub Actions

## Công cụ đang được cấu hình

Backend dùng `.github/workflows/cicd.yml`. Frontend có workflow riêng trong repository frontend.
`main` vẫn là nhánh phát hành chính. Server vật lý vẫn là đích triển khai.
Các thay đổi local chỉ có hiệu lực trên GitHub/Jenkins sau khi được commit và push.

`Jenkinsfile` hiện chỉ in thông báo tạm ngưng, không test/build/push/deploy hay gửi email.
Pipeline Jenkins trước đó được lưu đầy đủ tại `ci/Jenkinsfile.jenkins`.
Nên Disable job/multibranch pipeline Eatzy trong Jenkins để ngừng cả webhook và quét nhánh.
Source không thể dừng build Jenkins đã chạy hoặc job còn đang dùng commit cũ.

## Luồng chạy

- Pull request vào main: Java 17 → Gradle test → lưu báo cáo → prepareDockerArtifacts.
- Push main: các bước trên → build đủ 12 image → push latest và main-<commit> → SCP compose → SSH deploy.
- Run workflow thủ công: test và tạo JAR; chỉ nhánh main mới được publish/deploy.
- Test hoặc build lỗi sẽ dừng các bước publish/deploy tiếp theo.
- Mỗi nhánh chỉ chạy một workflow cùng lúc; không hủy bản main đang triển khai.

Task `prepareDockerArtifacts` tạo đúng 11 executable JAR trong `docker-artifacts/` mà Dockerfile Java cần.
Service AI dùng Dockerfile Python riêng. Gradle tests không kiểm thử hành vi AI.
Sau triển khai, Compose chờ các healthcheck đã khai báo và workflow gọi health của gateway cổng 8080.
Đây chưa phải kiểm thử toàn bộ nghiệp vụ hoặc rollback tự động. Production đang dùng tag latest.

## Chuẩn bị GitHub và server

Vào Settings → Secrets and variables → Actions của repository backend, kiểm tra các repository secrets:

| Secret | Nội dung |
|---|---|
| DOCKERHUB_USER | Tài khoản Docker Hub sở hữu image |
| DOCKERHUB_TOKEN | Token được phép push image |
| SERVER_IP | Host/IP mà GitHub runner kết nối được |
| SERVER_USER | User SSH trên server |
| SERVER_SSH_KEY | SSH private key tương ứng |
| SERVER_SSH_PORT | Cổng SSH |

Credentials trong Jenkins không tự đồng bộ sang GitHub Secrets.
Server cần Docker Engine, Compose hỗ trợ --wait, curl và quyền chạy Docker cho user SSH.
Giữ sẵn `.env` production và keystore tại `/home/<SERVER_USER>/projects/eatzy-microservices`.
Mặc định keystore là `eatzy.jks`; có thể đặt JWT_KEYSTORE_FILE trong .env để chỉ định đường dẫn khác.
Không dùng .env local trên máy Windows để ghi đè .env production: DB/Redis có địa chỉ khác.
Workflow chỉ gửi compose, không gửi .env hay keystore. Nếu image private, server cần docker login từ trước.
Máy GitHub runner phải truy cập được cổng SSH server; cấu hình firewall/NAT nằm ngoài source.

## Chuyển lại Jenkins sau này

1. Tắt workflow GitHub Actions trong giao diện Actions trước khi bật Jenkins để tránh hai đường deploy.
2. Khôi phục nội dung `ci/Jenkinsfile.jenkins` vào `Jenkinsfile`, commit/push.
3. Bật lại Jenkins job và kiểm tra credentials/webhook.

Các tài liệu `jenkins-ci-pipeline.md` và `testing.md` có phần mô tả pipeline Jenkins lưu trữ.
Với cấu hình hiện tại, dùng tài liệu này làm mô tả CI/CD chính.
Hướng dẫn Docker local: [docker-local.md](docker-local.md).
