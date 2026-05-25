# Testing Guide — Eatzy Microservices

## Yêu cầu

| Công cụ | Version | Ghi chú |
|---|---|---|
| Java | 17 | `sudo apt install openjdk-17-jdk` |
| Gradle Wrapper | — | Dùng `./gradlew`, không cần cài Gradle riêng |

Kiểm tra môi trường:

```bash
java -version        # phải ra openjdk 17
./gradlew --version  # phải ra Gradle 8.x
```

Nếu lỗi `Permission denied` với gradlew:

```bash
chmod +x gradlew
```

Nếu lỗi `JAVA_HOME is not set`:

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

---

## Chạy test

### Một service cụ thể

```bash
./gradlew :eatzy-auth-service:test
```

### Tất cả services cùng lúc (song song)

```bash
./gradlew test --parallel
```

### Buộc chạy lại (bỏ qua Gradle cache)

```bash
./gradlew :eatzy-auth-service:test --rerun-tasks
```

### Xem output chi tiết trong terminal

```bash
./gradlew :eatzy-auth-service:test --info
```

### Dừng ngay khi có test fail

```bash
./gradlew :eatzy-auth-service:test --fail-fast
```

---

## Xem kết quả

### HTML report (khuyến nghị)

```bash
# Sau khi chạy test, mở file này trong browser
eatzy-auth-service/build/reports/tests/test/index.html
```

### XML report (dùng cho Jenkins / CI)

```
eatzy-auth-service/build/test-results/test/*.xml
```

### Xem nhanh trong terminal

```bash
./gradlew :eatzy-auth-service:test --info 2>&1 | grep -E "PASSED|FAILED|SKIPPED"
```

---

## Các lệnh Gradle liên quan

### Build (bỏ qua test)

```bash
./gradlew :eatzy-auth-service:build -x test
```

### Chỉ compile test (không chạy)

```bash
./gradlew :eatzy-auth-service:testClasses
```

### Xóa toàn bộ build artifacts

```bash
./gradlew clean
```

### Clean rồi test lại từ đầu

```bash
./gradlew clean :eatzy-auth-service:test
```

---

## Tình trạng test hiện tại

| Service | Test | Loại |
|---|---|---|
| `eatzy-auth-service` | `UserServiceTest` (15 tests) | Unit test — Mockito |
| `eatzy-api-gateway` | Boilerplate (commented out) | — |
| `eatzy-config-server` | Boilerplate `contextLoads()` | Sẽ fail nếu không có infrastructure |
| `eatzy-discovery-server` | Boilerplate (commented out) | — |
| Các service còn lại | Chưa có | — |

> **Lưu ý:** Boilerplate `@SpringBootTest contextLoads()` yêu cầu DB, Kafka, Redis đang chạy mới pass được.
> Unit test trong `eatzy-auth-service` dùng Mockito nên không cần infrastructure — chạy được ngay.

---

## Thêm test cho service mới

Tạo file theo đúng đường dẫn package:

```
<service-name>/src/test/java/com/eatzy/<module>/service/<ClassName>Test.java
```

Ví dụ cho `eatzy-restaurant-service`:

```
eatzy-restaurant-service/src/test/java/com/eatzy/restaurant/service/RestaurantServiceTest.java
```

Template cơ bản:

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @Mock
    private MyRepository myRepository;

    @InjectMocks
    private MyService myService;

    @Test
    void someMethod_doesExpectedThing() {
        // given
        when(myRepository.findById(1L)).thenReturn(Optional.of(new MyEntity()));

        // when
        MyEntity result = myService.someMethod(1L);

        // then
        assertThat(result).isNotNull();
    }
}
```

---

## Chạy test trong Jenkins

Jenkins tự động chạy stage **Test** khi push lên bất kỳ branch nào.
Nếu test fail, build sẽ bị đánh dấu **UNSTABLE** (không block deploy).

Xem kết quả trên Jenkins UI:
1. Vào job build tương ứng
2. Click **Test Result** để xem báo cáo JUnit
