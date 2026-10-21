# Kiến trúc backend và hợp đồng với client

Tài liệu này ghi lại source hiện có, giúp agent chọn đúng nơi sửa. Khi source thay đổi, đối chiếu lại những đường dẫn liên quan; các giới hạn bên dưới không tự biến thành bảo đảm chỉ vì có tài liệu.

## Ranh giới module

Backend dùng Java 17, Spring Boot 3.4.3, Spring Cloud 2024.0.0 và Gradle wrapper 8.7. `settings.gradle.kts` khai báo 12 module Java: 11 ứng dụng executable và `eatzy-common`. `eatzy-ai-service` là Python FastAPI riêng, không nằm trong test/build Java.

| Module | Trách nhiệm và nơi bắt đầu đọc |
|---|---|
| `eatzy-api-gateway` | `src/main/resources/application.yml`: route public và Swagger; security dùng WebFlux |
| `eatzy-auth-service` | `controller/AuthController.java`, `service/UserService.java`, `domain`, `mapper`, `config`: tài khoản, JWT, roles, hồ sơ và địa chỉ |
| `eatzy-restaurant-service` | `controller`, `service`, `dto`, `mapper`, `client`: nhà hàng, menu và báo cáo |
| `eatzy-order-service` | `service/OrderService.java`, `designpattern/facade/OrderCreationFacade.java`, `designpattern/cor`, `designpattern/state`: đặt đơn, chuyển trạng thái, giao hàng |
| `eatzy-cart-service` | `service/CartService.java`: giỏ hàng theo người dùng/nhà hàng |
| `eatzy-payment-service` | `service`, `designpattern/strategy`: COD/VNPAY/WALLET, ví, vouchers |
| `eatzy-communication-service` | `config`, `controller`, `kafka`, `designpattern/factory`: STOMP, chat, vị trí và notifications |
| `eatzy-interaction-service` | `controller`, `designpattern/event`: reviews, favorites và scoring |
| `eatzy-system-config-service` | `controller/SystemConfigurationController.java`: cấu hình nghiệp vụ |
| `eatzy-common` | `config`, `dto`, `event`, `exception`, `service`, `util`: contracts/hạ tầng dùng chung |
| Config / Discovery | `eatzy-config-server/src/main/resources/configurations`, `eatzy-discovery-server`: cấu hình tập trung và Eureka |
| `eatzy-ai-service` | `main.py`: lấy dữ liệu nhà hàng/reviews qua gateway, gọi Gemini tạo insights |

Đường dẫn Java trong bảng tính từ `src/main/java/com/eatzy/<domain>/`, trừ package gateway/discovery/config-server có tên riêng. Đọc class entry point để xác nhận package và component scanning.

## Gateway và response

Client gọi gateway cổng `8080`, endpoint nghiệp vụ dạng `/api/v1/...`. Route của auth bao gồm users/roles/profiles/addresses; restaurant bao gồm dishes/options/categories; payment bao gồm vouchers/wallets/transactions. Gateway cũng chuyển `/ws/**` sang communication và `/api/v1/ai/**` sang AI. Source route là `eatzy-api-gateway/src/main/resources/application.yml`.

`eatzy-common/.../config/FormatRestResponse.java` bọc JSON thành công của các Spring MVC service:

```json
{"statusCode":200,"error":null,"message":"CALL API SUCCESS","data":{}}
```

Pagination dùng `data.meta` chứa `page`, `pageSize`, `total`, `pages`, và `data.result` chứa danh sách; page phản hồi được tính từ 1 trong các service hiện tại. Không nhầm `result` là root response. OpenAPI/JWKS, body String/Resource, media type khác JSON và lỗi có nhánh xử lý khác; xem advice/exception handler khi thay error contract. Python AI trả JSON trực tiếp và không đi qua Java response advice.

Feign adapters nằm ở `designpattern/adapter` của nhiều service; một số service dùng `client`. `eatzy-common/.../config/FeignConfig.java` đăng ký `RestResponseDecoder` để unwrap `data`, được cấp qua `@EnableFeignClients(defaultConfiguration = FeignConfig.class)`. Hai class này cố ý không component-scanned. `FeignAutoConfiguration` đăng ký `FeignClientInterceptor`, forward Authorization từ HTTP request hiện tại; không giả định background Kafka thread có sẵn HTTP request/token.

## Auth: hợp đồng web và mobile

Nguồn: `eatzy-auth-service/.../controller/AuthController.java`, `domain/res/ResLoginDTO.java`, `util/SecurityUtil.java`, `config/SecurityConfiguration.java`; shared lookup ở `eatzy-common/.../util/SecurityUtils.java`.

| Request | Hành vi hiện có |
|---|---|
| `POST /api/v1/auth/login` với `{username,password}` | Trả `data.access_token` và `data.user`; đặt cookie `refresh_token` |
| `GET /api/v1/auth/account` với bearer token | Trả user đang đăng nhập |
| `GET /api/v1/auth/refresh` với cookie | Kiểm tra JWT và token đang lưu trong User, trả access token mới, xoay refresh cookie |
| `POST /api/v1/auth/logout` với bearer token | Xóa refresh token trong User và cookie |
| `POST /api/v1/auth/register` | Tạo tài khoản, có luồng xác minh email liên quan |

Cookie refresh dùng `HttpOnly`, `Secure`, `Path=/`; refresh token không được trả trong JSON và endpoint refresh không đọc JSON body. User hiện lưu một refresh token, nên đăng nhập mới có thể làm phiên cũ không refresh được. Không thiết kế client dựa trên giả định nhiều refresh session độc lập.

Access JWT ký RS256, `sub=email`, `user={id,email,name}`, `permission` là mảng authorities. Hiện `SecurityUtil` gán cứng hai authorities `ROLE_USER_CREATE`, `ROLE_USER_UPDATE`; không được diễn giải thành hệ RBAC hoàn chỉnh. Role object nằm trong login/account `user.role`, không phải JWT role claim. `SecurityUtils.getCurrentUserId()` lấy `user.id` từ JWT.

Gateway hiện `permitAll`; kiểm tra xác thực/ủy quyền nằm từng service. UI role guard không thay thế backend ownership checks. Khi sửa quyền, kiểm tra controller, service và từng `SecurityConfiguration`, không chỉ gateway.

Web dùng proxy Next `/api/proxy`; React Native cần URL gateway tuyệt đối. `localhost` trên điện thoại/emulator không luôn là máy chạy Docker. HTTP local trên điện thoại không bảo đảm gửi được Secure cookie. Cần kiểm chứng cookie storage/rotation với runtime mobile và HTTPS dev phù hợp; không tự bỏ Secure hay thêm token vào response để làm demo chạy.

## Đơn hàng, giỏ hàng, thanh toán

`OrderCreationFacade` gọi chain customer → restaurant → delivery fee → order items, tạo đơn PENDING, tính tổng, publish events và gọi payment adapter. Giá món/options lấy từ restaurant, quantity phải lớn hơn 0; money dùng `BigDecimal`. `DeliveryFeeValidationHandler` so giá giao hàng do server tính với client sau khi làm tròn; khi khác giá client cần tải lại báo giá.

`designpattern/state/OrderStatus.java` là bảng chuyển trạng thái; `OrderStateMachine` kiểm tra transition/cancel. DELIVERED và REJECTED là trạng thái cuối; customer cancel cho PENDING/PREPARING. Bảng có cả PREPARING→READY và DRIVER_ASSIGNED→PICKED_UP; đừng suy luận chỉ từ tuyến thẳng trong comment. Thao tác chuyển trạng thái thực tế nằm `OrderService` và có side effects về ví/events/driver.

`CartService.saveOrUpdateCart` lấy customer từ JWT, tìm giỏ theo customer+restaurant, thay toàn bộ items/options. Giỏ rỗng được xóa; không đổi sang append nếu chưa đánh giá semantics của client.

`PaymentStrategyFactory` dùng map Spring beans cho COD/VNPAY/WALLET. Khi sửa callback, refund hay wallet, kiểm tra signature, số dư, duplicate/retry và transaction hiện có; chưa có test coverage đủ để khẳng định các luồng này an toàn idempotent.

Các giới hạn cần tính đến khi mở rộng:

- Order creation đang nhận customer ID và paymentStatus trong request; customer validation xác nhận tồn tại. Đừng coi đó là bằng chứng đã kiểm tra quyền sở hữu ở mọi thao tác.
- Kafka OrderCreated được publish trước initiate payment trong DB transaction. Rollback DB không thu hồi Kafka event hoặc side effect payment từ service khác; chưa có bằng chứng outbox/exactly-once.
- Voucher discount lookup có nhánh fallback khi service lỗi; đọc implementation trước khi thay exception behavior.

## Kafka và STOMP

Common event DTO ở `eatzy-common/.../event`. Order publisher là `eatzy-order-service/.../kafka/OrderEventProducer.java`. Các topic `order-events` và `order_events_topic` phục vụ mục đích khác nhau: nghiệp vụ notifications/payment và scoring; không tự đồng nhất tên. Còn có `user-events`, `restaurant-events`, `driver-events`, `cart_events_topic`; trace producer và consumer trước khi đổi schema/topic.

`WebSocketConfig` mở STOMP endpoint SockJS `/ws`; `WebSocketAuthChannelInterceptor` xác thực CONNECT bằng `Authorization: Bearer <token>` hoặc native header `token`. Principal là email. Client gửi tới prefix `/app`; client nhận user destinations prefix `/user`.

| Hành động | Destination |
|---|---|
| Gửi chat / typing | `/app/chat/{orderId}`, `/app/typing/{orderId}` |
| Gửi vị trí driver | `/app/driver/location` |
| Nhận order / chat | `/user/queue/orders`, `/user/queue/chat/order/{orderId}` |
| Nhận typing / location / system | `/user/queue/chat/order/{orderId}/typing`, `/user/queue/driver-location`, `/user/queue/system` |

ChatController kiểm tra principal là customer hoặc driver của order và lấy senderId/type từ phía server. DriverLocationController lấy user từ principal email, cập nhật Redis Geo và chuyển vị trí cho customer của active order. Không bỏ các bước này khi port client.

`NotificationFactory` inject `List<INotificationCreator>` thành registry. Thêm loại notification qua creator component và Notification subclass; giữ các type `ORDER_STATUS`, `CHAT_MESSAGE`, `DRIVER_LOCATION`, `SYSTEM` và payload/destination tương thích client.

## Kiểm chứng và vận hành

Các test hiện hữu: auth 16, cart 2, order 2, restaurant 2 unit tests Mockito; config-server 1 context test. Gateway/discovery có boilerplate commented. Đây là coverage hẹp: order tests chủ yếu fee và Mapbox fallback; payment, realtime và AI chưa được bao phủ tương đương. Test context config-server có thể log cảnh báo Eureka/Logstash khi hạ tầng không bật.

`prepareDockerArtifacts` tạo 11 executable JAR đúng tên Dockerfile cần. `scripts/start-local.ps1` dùng Java 17, build image tuần tự và đợi health; các tham số `-SkipBuild`, `-BuildOnly` có sẵn. Local Compose mặc định DB trên host:3307, Redis trong Docker; secrets nằm `.env` riêng. Không ghi đè `.env` production bằng cấu hình local.

`.github/workflows/cicd.yml` đang hoạt động: PR main test/tạo JAR; push main test → tạo JAR → build/push 12 images → SCP compose → SSH server vật lý. Production dùng tag latest, healthcheck chưa chứng minh toàn bộ nghiệp vụ và chưa có rollback tự động. Jenkins tạm ngưng, pipeline cũ lưu `ci/Jenkinsfile.jenkins`.

## Phối hợp ba repository

Thông thường checkout đặt cạnh nhau: backend này, `../eatzy_frontend` (Next.js monorepo) và `../eatzy_mobile` (React Native). Nếu không có đúng sibling, tìm trong workspace hoặc báo rõ repo chưa có; không tạo giả client implementation.

Khi đổi API/auth/event: xác định controller + DTO + gateway + web API/types/socket consumer + mobile adapter/types bị ảnh hưởng. Tìm route/field/type cũ ở các checkout liên quan, cập nhật đồng bộ trong scope được giao hoặc ghi follow-up có chi tiết. Ghi rõ đâu là behavior hiện có, đâu là migration đề xuất. Các thay đổi hướng dẫn agent không tự cho phép push main hay triển khai server.
