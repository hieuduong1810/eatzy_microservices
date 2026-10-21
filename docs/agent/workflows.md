# Dùng agent cho backend Eatzy

Mở đúng thư mục backend trong Codex. `AGENTS.md` cung cấp cấu trúc và lệnh chung; `.agents/skills` cung cấp cách làm cho từng loại việc. Agent có thể tự chọn skill phù hợp, hoặc bạn gọi rõ bằng `$eatzy-backend-change`, `$eatzy-backend-debug`, `$eatzy-backend-review`.

## Bắt đầu một việc

Cho biết kết quả mong muốn, vai trò người dùng, cách tái hiện và giới hạn phạm vi. Không cần mô tả giải pháp kỹ thuật nếu chưa chắc.

Ví dụ triển khai:

```text
Dùng $eatzy-backend-change để thêm bộ lọc đơn theo ngày cho nhà hàng.
Đọc API hiện tại và client web trước, giữ response cũ tương thích.
Sửa backend và consumer liên quan nếu cần, kiểm tra đúng service.
Giữ thay đổi local để tôi xem diff.
```

Ví dụ sửa lỗi:

```text
Dùng $eatzy-backend-debug. Sau khi đăng nhập, customer tải lại trang thì bị logout.
Backend chạy Docker local, web chạy pnpm dev. Hãy xác định request hỏng,
kiểm tra cookie/refresh và sửa nguyên nhân. Không dùng tài khoản production.
```

Ví dụ review:

```text
Dùng $eatzy-backend-review để xem diff hiện tại với main.
Tập trung quyền thao tác đơn, giá tiền và side effects Kafka/payment.
Báo lỗi có đường dẫn/dòng và kịch bản gây lỗi; chưa sửa code.
```

Bạn có thể cho phép commit/push khi đã muốn thực hiện. Không cần xác nhận lại từng lần đọc source, chỉnh local hay chạy kiểm tra phù hợp. Push `main` là bước có tác dụng triển khai server theo workflow hiện tại, nên nêu rõ ý định khi giao việc đó.

## Kiểm tra môi trường

Từ root backend trong PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode doctor
```

Đọc kết quả trước khi cài thêm công cụ. Dự án dùng JDK 17 và Gradle wrapper; không cần cài Gradle toàn máy. Các secrets vẫn cấu hình bằng cơ chế `.env`/server hiện tại; không dán token, mật khẩu hay private key vào prompt/log.

## Chạy và kiểm tra local

```powershell
# Build image và bật backend local.
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-local.ps1

# Nếu code/images chưa đổi, dùng lại image.
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-local.ps1 -SkipBuild

# Kiểm tra Compose mà không in giá trị secrets.
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode config

# Kiểm tra health của các service.
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode health
```

Gateway local là `http://localhost:8080`; health tổng quan không thay thế đăng nhập, đặt đơn hoặc nhận socket thực tế. Lệnh `docker compose ps` cho biết container đang chạy/healthy; `docker compose logs --tail 100 <service>` hỗ trợ chẩn đoán nhưng cần lọc dữ liệu nhạy cảm trước khi chia sẻ.

Với điện thoại thật/emulator, xác nhận host gateway phù hợp và Secure cookie trước khi coi mobile đã kết nối đầy đủ. Xem [architecture.md](architecture.md) cho khác biệt auth web/mobile.

## Test theo phạm vi sửa

```powershell
# Test một service.
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode test -Service eatzy-order-service

# Test Java toàn repo khi sửa common hoặc tác động nhiều service.
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode test

# Tạo lại executable JAR mà Dockerfile cần.
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\agent-check.ps1 -Mode artifacts
```

Direct Gradle cũng dùng được: `.\gradlew.bat :eatzy-order-service:test` trên Windows, `./gradlew :eatzy-order-service:test` trên shell Unix. Reports ở `<service>/build/reports/tests/test/index.html`.

Chọn test theo behavior đã thay đổi. Với lỗi quyền, dùng trường hợp hợp lệ và user khác không có quyền; với tiền/callback, xem số dư, duplicate và failure. Không chỉ thêm test khớp tên class. Tài liệu thuần túy thường chỉ cần rà đường dẫn/lệnh; không cần rebuild toàn bộ Docker.

## Đổi hợp đồng giữa backend, web và mobile

Các thư mục sibling thường là `../eatzy_frontend` và `../eatzy_mobile`. Đọc `AGENTS.md` từng repo trước khi sửa. Hãy yêu cầu agent liệt kê consumer của route/DTO/notification, giữ bản cũ tương thích nếu đang còn client sử dụng, rồi mô tả cách kiểm chứng của từng repo. Không copy Next `/api/proxy`, browser localStorage hoặc cookie assumptions sang React Native.

Backend là nguồn hợp đồng hiện có; không tự thêm endpoint refresh body hoặc sửa payment status để khớp một mock UI. Khi thiếu API mobile, ghi rõ phần chênh lệch và triển khai adapter/API trong phạm vi được thống nhất.

## Trước khi đưa lên main

Review diff và kết quả kiểm tra; phân biệt đã thực chạy với việc chỉ đọc source. Xem [../main-ci-cd.md](../main-ci-cd.md) cho secrets/server và luồng GitHub Actions. Jenkins đang tạm ngưng. Việc bật lại Jenkins cần tránh hai pipeline cùng deploy.

Agent nên kết thúc mỗi việc bằng mô tả ngắn: sửa gì, test nào đã chạy, ảnh hưởng web/mobile, còn giới hạn gì và thay đổi đã commit/push hay chưa. Bộ hướng dẫn không tự chứng minh hệ thống đã đầy đủ security, nghiệp vụ hoặc mobile parity; cải thiện chúng theo lỗi và yêu cầu thực tế.
